#!/usr/bin/env python3
"""
Local MOSIP WebSub hub mock for id-repository docker-compose.

Supports register / subscribe / unsubscribe / publish against /hub/
and delivers published payloads to subscribers with MOSIP-compatible
x-hub-signature (SHA256=<lowercase hex HMAC>).

Local partner ack:
  When topic ends with /CREDENTIAL_ISSUED, auto-publishes CREDENTIAL_STATUS_UPDATE
  with status=STORED (signed) so credential_transaction moves past ISSUED.

Also exposes:
  POST /deliver/{topic}  — sign + push body to subscribers (for tests/JMeter)
  GET  /health
  GET  /subscriptions
"""

from __future__ import annotations

import hashlib
import hmac
import json
import os
import secrets
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Dict, List, Tuple

HOST = "0.0.0.0"
PORT = 8085
SUBS_FILE = os.environ.get("WEBSUB_SUBS_FILE", "/tmp/websub-subs.json")
# Delay so credential_transaction ISSUED row is committed before STORED update.
AUTO_STORED_DELAY_SECS = float(os.environ.get("WEBSUB_AUTO_STORED_DELAY_SECS", "2.5"))
DEFAULT_STATUS_CALLBACK = os.environ.get(
    "WEBSUB_STATUS_CALLBACK",
    "http://id-repository-service:8090/v1/credentialrequest/callback/notifyStatus",
)
DEFAULT_STATUS_SECRET = os.environ.get("WEBSUB_STATUS_SECRET", "test")

# topic -> list of (callback_url, secret)
_subs: Dict[str, List[Tuple[str, str]]] = {}
_lock = threading.Lock()


def _hub_accepted() -> bytes:
    return b"hub.mode=accepted"


def _sign(secret: str, body: bytes) -> str:
    digest = hmac.new(secret.encode("utf-8"), body, hashlib.sha256).hexdigest()
    return f"SHA256={digest}"


def _load_subs() -> None:
    try:
        with open(SUBS_FILE, "r", encoding="utf-8") as f:
            raw = json.load(f)
        with _lock:
            _subs.clear()
            for topic, items in raw.items():
                _subs[topic] = [(i["callback"], i["secret"]) for i in items]
        print(f"websub-mock: loaded {sum(len(v) for v in _subs.values())} subscriptions", flush=True)
    except FileNotFoundError:
        pass
    except Exception as e:  # noqa: BLE001
        print(f"websub-mock: load subs failed: {e}", flush=True)


def _save_subs_unlocked() -> None:
    data = {
        topic: [{"callback": c, "secret": s} for c, s in items]
        for topic, items in _subs.items()
    }
    try:
        with open(SUBS_FILE, "w", encoding="utf-8") as f:
            json.dump(data, f)
    except Exception as e:  # noqa: BLE001
        print(f"websub-mock: save subs failed: {e}", flush=True)


def _save_subs() -> None:
    with _lock:
        _save_subs_unlocked()


def _ensure_status_subscription() -> None:
    """Seed CREDENTIAL_STATUS_UPDATE if empty (survives mock restart before id-repo re-subscribes)."""
    with _lock:
        bucket = _subs.setdefault("CREDENTIAL_STATUS_UPDATE", [])
        if not any(c == DEFAULT_STATUS_CALLBACK for c, _ in bucket):
            bucket.append((DEFAULT_STATUS_CALLBACK, DEFAULT_STATUS_SECRET))
            print(
                f"websub-mock: seeded CREDENTIAL_STATUS_UPDATE -> {DEFAULT_STATUS_CALLBACK}",
                flush=True,
            )
            _save_subs_unlocked()


def _deliver(topic: str, body: bytes, content_type: str = "application/json") -> List[str]:
    with _lock:
        targets = list(_subs.get(topic, []))
    results: List[str] = []
    for callback, secret in targets:
        headers = {
            "Content-Type": content_type,
            "x-hub-signature": _sign(secret, body),
        }
        req = urllib.request.Request(callback, data=body, headers=headers, method="POST")
        try:
            with urllib.request.urlopen(req, timeout=15) as resp:
                results.append(f"{callback} -> {resp.status}")
        except urllib.error.HTTPError as e:
            results.append(f"{callback} -> HTTP {e.code}")
        except Exception as e:  # noqa: BLE001
            results.append(f"{callback} -> ERR {e}")
    return results


def _iso_now() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + "Z"


def _build_status_event(request_id: str, status: str, url: str = "") -> bytes:
    status_body = {
        "publisher": "PRINT_SERVICE",
        "topic": "CREDENTIAL_STATUS_UPDATE",
        "publishedOn": _iso_now(),
        "event": {
            "id": str(uuid.uuid4()),
            "requestId": request_id,
            "timestamp": _iso_now(),
            "status": status,
            "url": url or "",
        },
    }
    return json.dumps(status_body, separators=(",", ":")).encode("utf-8")


def _publish_status_to_subscribers(raw: bytes) -> List[str]:
    """Hub delivery for CREDENTIAL_STATUS_UPDATE → /callback/notifyStatus (signed)."""
    _ensure_status_subscription()
    return _deliver("CREDENTIAL_STATUS_UPDATE", raw)


def _auto_stored_ack(issued_body: bytes) -> None:
    """
    Local mock partner: after CREDENTIAL_ISSUED, publish CREDENTIAL_STATUS_UPDATE (STORED)
    to the hub so the subscribed notifyStatus callback updates credential_transaction.
    """
    try:
        time.sleep(AUTO_STORED_DELAY_SECS)
        payload = json.loads(issued_body.decode("utf-8"))
        event = payload.get("event") or {}
        request_id = event.get("transactionId") or event.get("requestId")
        if not request_id:
            print("websub-mock: CREDENTIAL_ISSUED missing transactionId — skip STORED", flush=True)
            return
        url = event.get("dataShareUri") or ""
        raw = _build_status_event(request_id, "STORED", url)
        results = _publish_status_to_subscribers(raw)
        print(
            f"websub-mock: partner→hub CREDENTIAL_STATUS_UPDATE status=STORED requestId={request_id} deliveries={results}",
            flush=True,
        )
    except Exception as e:  # noqa: BLE001
        print(f"websub-mock: auto STORED failed: {e}", flush=True)


def _intent_verify(callback: str, topic: str, mode: str = "subscribe") -> None:
    challenge = secrets.token_hex(8)
    qs = urllib.parse.urlencode(
        {
            "hub.mode": mode,
            "hub.topic": topic,
            "hub.challenge": challenge,
            "hub.lease_seconds": "9999999",
        }
    )
    url = f"{callback}{'&' if '?' in callback else '?'}{qs}"
    try:
        with urllib.request.urlopen(url, timeout=10) as resp:
            body = resp.read().decode("utf-8", errors="replace").strip()
            if challenge not in body:
                print(f"websub-mock: intent challenge mismatch for {callback}", flush=True)
            else:
                print(f"websub-mock: intent OK topic={topic} callback={callback}", flush=True)
    except Exception as e:  # noqa: BLE001
        print(f"websub-mock: intent verify skipped/failed ({e})", flush=True)


def _parse_form(raw: bytes, content_type: str) -> Dict[str, str]:
    if "application/x-www-form-urlencoded" in (content_type or ""):
        return {k: v[0] for k, v in urllib.parse.parse_qs(raw.decode("utf-8")).items()}
    return {}


class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt: str, *args) -> None:
        try:
            line = args[0] if args else str(fmt)
        except Exception:  # noqa: BLE001
            line = str(fmt)
        if "GET /health" in line or "GET / HTTP" in line:
            return
        print(f"websub-mock: {line}", flush=True)

    def _send(self, code: int, body: bytes, content_type: str = "application/x-www-form-urlencoded") -> None:
        self.send_response(code)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self) -> None:  # noqa: N802
        parsed = urllib.parse.urlparse(self.path)
        if parsed.path in ("/health", "/"):
            self._send(200, b'{"status":"UP"}', "application/json")
            return
        if parsed.path == "/subscriptions":
            with _lock:
                data = {t: [{"callback": c, "secret": "***"} for c, _s in items] for t, items in _subs.items()}
            self._send(200, json.dumps(data).encode(), "application/json")
            return
        self._send(404, b"not found", "text/plain")

    def do_POST(self) -> None:  # noqa: N802
        parsed = urllib.parse.urlparse(self.path)
        length = int(self.headers.get("Content-Length", "0"))
        raw = self.rfile.read(length) if length else b""
        ctype = self.headers.get("Content-Type", "")
        qs = urllib.parse.parse_qs(parsed.query)

        if parsed.path.startswith("/deliver/"):
            topic = urllib.parse.unquote(parsed.path[len("/deliver/") :])
            results = _deliver(topic, raw, ctype or "application/json")
            self._send(200, json.dumps({"topic": topic, "delivered": results}).encode(), "application/json")
            return

        # Partner (or JMeter) publishes status to the hub — do NOT POST notifyStatus unsigned.
        # Body: CredentialStatusEvent JSON (same as callback payload).
        if parsed.path in ("/partner/credential-status", "/hub/partner/credential-status"):
            try:
                payload = json.loads(raw.decode("utf-8") or "{}")
                event = payload.get("event") or {}
                if not event.get("requestId"):
                    self._send(400, b'{"error":"event.requestId required"}', "application/json")
                    return
                if not event.get("status"):
                    event["status"] = "STORED"
                    payload["event"] = event
                payload.setdefault("publisher", "PRINT_SERVICE")
                payload.setdefault("topic", "CREDENTIAL_STATUS_UPDATE")
                payload.setdefault("publishedOn", _iso_now())
                body = json.dumps(payload, separators=(",", ":")).encode("utf-8")
                results = _publish_status_to_subscribers(body)
                print(
                    f"websub-mock: partner publish CREDENTIAL_STATUS_UPDATE requestId={event.get('requestId')} "
                    f"status={event.get('status')} deliveries={results}",
                    flush=True,
                )
                self._send(
                    200,
                    json.dumps({"topic": "CREDENTIAL_STATUS_UPDATE", "delivered": results}).encode(),
                    "application/json",
                )
            except Exception as e:  # noqa: BLE001
                self._send(500, json.dumps({"error": str(e)}).encode(), "application/json")
            return

        if not parsed.path.startswith("/hub"):
            self._send(404, b"not found", "text/plain")
            return

        form = _parse_form(raw, ctype)
        mode = (form.get("hub.mode") or (qs.get("hub.mode") or [""])[0] or "").lower()
        topic = form.get("hub.topic") or (qs.get("hub.topic") or [""])[0]

        if mode in ("register", "unregister"):
            print(f"websub-mock: {mode} topic={topic}", flush=True)
            self._send(200, _hub_accepted())
            return

        if mode == "subscribe":
            callback = form.get("hub.callback", "")
            secret = form.get("hub.secret", "test")
            if topic and callback:
                with _lock:
                    bucket = _subs.setdefault(topic, [])
                    bucket[:] = [(c, s) for c, s in bucket if c != callback]
                    bucket.append((callback, secret))
                _save_subs()
                threading.Thread(target=_intent_verify, args=(callback, topic, "subscribe"), daemon=True).start()
                print(f"websub-mock: subscribe topic={topic} callback={callback}", flush=True)
            self._send(200, _hub_accepted())
            return

        if mode == "unsubscribe":
            callback = form.get("hub.callback", "")
            with _lock:
                if topic in _subs:
                    _subs[topic] = [(c, s) for c, s in _subs[topic] if c != callback]
            _save_subs()
            self._send(200, _hub_accepted())
            return

        if mode == "publish":
            # Ensure status subscribers exist before identity/partner publish.
            if topic == "CREDENTIAL_STATUS_UPDATE":
                _ensure_status_subscription()
            results = _deliver(topic, raw, ctype or "application/json")
            print(f"websub-mock: publish topic={topic} deliveries={results}", flush=True)
            # Local stand-in for print partner: publish STORED back on CREDENTIAL_STATUS_UPDATE
            # so /v1/credentialrequest/callback/notifyStatus updates the row.
            if topic.endswith("/CREDENTIAL_ISSUED") or topic == "CREDENTIAL_ISSUED":
                threading.Thread(target=_auto_stored_ack, args=(raw,), daemon=True).start()
            self._send(200, _hub_accepted())
            return

        self._send(200, _hub_accepted())


def main() -> None:
    _load_subs()
    _ensure_status_subscription()
    httpd = ThreadingHTTPServer((HOST, PORT), Handler)
    print(f"websub-mock listening on {HOST}:{PORT}", flush=True)
    httpd.serve_forever()


if __name__ == "__main__":
    main()
