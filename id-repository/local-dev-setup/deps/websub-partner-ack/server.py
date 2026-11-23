#!/usr/bin/env python3
"""Local WebSub hub that auto-acks credential STORED after CREDENTIAL_ISSUED.

MOSIP flow locally:
  credential issue → publish {partner}/CREDENTIAL_ISSUED → this hub
  → wait until credential_transaction is ISSUED (fields persisted)
  → POST /v1/credentialrequest/callback/notifyStatus with status=STORED
  → credential_transaction.status_code becomes STORED (keeps credential_id etc.)

Why wait: publish runs *before* the ISSUED row is saved. An immediate STORED
callback can load a stale NEW entity and overwrite the later ISSUED save
(null credential_id / issuanceDate / signature).

HMAC matches kernel AuthenticatedContentVerifier: header ``SHA256=<hex>``,
secret from annotation (credential notifyStatus uses ``test``).
"""
from __future__ import annotations

import hashlib
import hmac
import json
import os
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from datetime import datetime, timezone

PORT = int(os.environ.get("WEBSUB_PORT", "8085"))
NOTIFY_URL = os.environ.get(
    "CREDENTIAL_STATUS_CALLBACK",
    "http://host.docker.internal:8090/v1/credentialrequest/callback/notifyStatus",
)
STATUS_URL_BASE = os.environ.get(
    "CREDENTIAL_STATUS_GET_URL",
    "http://host.docker.internal:8090/v1/credentialrequest/get",
).rstrip("/")
AUTH_TOKEN_URL = os.environ.get(
    "AUTH_TOKEN_URL",
    "http://mock-service:8082/v1/authmanager/authenticate/clientidsecretkey",
)
AUTH_CLIENT_ID = os.environ.get("AUTH_CLIENT_ID", "mosip-idrepo-client")
AUTH_SECRET_KEY = os.environ.get("AUTH_SECRET_KEY", "local-secret")
AUTH_APP_ID = os.environ.get("AUTH_APP_ID", "idrepo")
NOTIFY_SECRET = os.environ.get("CREDENTIAL_STATUS_SECRET", "test")
# Delay + poll until ISSUED is committed before STORED (avoids stale overwrite).
ACK_INITIAL_DELAY_SEC = float(os.environ.get("CREDENTIAL_ACK_INITIAL_DELAY_SEC", "1.5"))
ACK_POLL_INTERVAL_SEC = float(os.environ.get("CREDENTIAL_ACK_POLL_INTERVAL_SEC", "0.5"))
ACK_POLL_TIMEOUT_SEC = float(os.environ.get("CREDENTIAL_ACK_POLL_TIMEOUT_SEC", "30"))
ISSUED_SUFFIX = "/CREDENTIAL_ISSUED"

_token_cache: str | None = None
_token_lock = threading.Lock()


def utc_now() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + "Z"


def hmac_sha256_header(secret: str, body: str) -> str:
    digest = hmac.new(secret.encode("utf-8"), body.encode("utf-8"), hashlib.sha256).hexdigest()
    return f"SHA256={digest}"


def parse_form(raw: bytes) -> dict[str, str]:
    text = raw.decode("utf-8", errors="replace")
    return {k: v[0] for k, v in urllib.parse.parse_qs(text, keep_blank_values=True).items()}


def fetch_auth_token(force: bool = False) -> str | None:
    """WireMock local IAM JWT for AuthFilter on GET /credentialrequest/get/{id}."""
    global _token_cache
    with _token_lock:
        if _token_cache and not force:
            return _token_cache
        body = json.dumps(
            {
                "id": "string",
                "metadata": {},
                "request": {
                    "clientId": AUTH_CLIENT_ID,
                    "secretKey": AUTH_SECRET_KEY,
                    "appId": AUTH_APP_ID,
                },
                "requesttime": utc_now(),
                "version": "string",
            }
        )
        req = urllib.request.Request(
            AUTH_TOKEN_URL,
            data=body.encode("utf-8"),
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        try:
            with urllib.request.urlopen(req, timeout=10) as resp:
                token = resp.headers.get("Authorization") or resp.headers.get("authorization")
                raw = resp.read().decode("utf-8", errors="replace")
                if not token:
                    try:
                        payload = json.loads(raw)
                        token = (payload.get("response") or {}).get("token")
                    except Exception:  # noqa: BLE001
                        token = None
        except Exception as e:  # noqa: BLE001
            print(f"[auto-stored] auth token fetch failed: {e}", flush=True)
            return None
        if isinstance(token, str) and token.strip():
            _token_cache = token.strip()
            return _token_cache
        print("[auto-stored] auth token response empty", flush=True)
        return None


def fetch_status(request_id: str) -> str | None:
    url = f"{STATUS_URL_BASE}/{urllib.parse.quote(request_id, safe='')}"
    token = fetch_auth_token()
    if not token:
        return None
    headers = {
        "Authorization": token,
        "Cookie": f"Authorization={token}",
    }
    req = urllib.request.Request(url, headers=headers, method="GET")
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            payload = json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        # Token rejected / expired — refresh once.
        if e.code in (401, 403):
            token = fetch_auth_token(force=True)
            if token:
                headers = {"Authorization": token, "Cookie": f"Authorization={token}"}
                try:
                    req2 = urllib.request.Request(url, headers=headers, method="GET")
                    with urllib.request.urlopen(req2, timeout=10) as resp:
                        payload = json.loads(resp.read().decode("utf-8"))
                except Exception as e2:  # noqa: BLE001
                    print(
                        f"[auto-stored] status poll requestId={request_id} failed after reauth: {e2}",
                        flush=True,
                    )
                    return None
            else:
                return None
        else:
            err = e.read().decode("utf-8", errors="replace")[:200]
            print(
                f"[auto-stored] status poll requestId={request_id} HTTP {e.code}: {err}",
                flush=True,
            )
            return None
    except Exception as e:  # noqa: BLE001
        print(f"[auto-stored] status poll requestId={request_id} failed: {e}", flush=True)
        return None
    response = payload.get("response") if isinstance(payload, dict) else None
    if not isinstance(response, dict):
        return None
    status = response.get("statusCode")
    return status.strip() if isinstance(status, str) else None


def wait_until_issued(request_id: str) -> bool:
    if ACK_INITIAL_DELAY_SEC > 0:
        time.sleep(ACK_INITIAL_DELAY_SEC)
    deadline = time.monotonic() + ACK_POLL_TIMEOUT_SEC
    while time.monotonic() < deadline:
        status = fetch_status(request_id)
        if status == "ISSUED":
            print(f"[auto-stored] requestId={request_id} status=ISSUED — sending STORED", flush=True)
            return True
        if status in ("FAILED", "CANCELLED"):
            print(f"[auto-stored] requestId={request_id} status={status} — skip STORED", flush=True)
            return False
        if status == "STORED":
            print(f"[auto-stored] requestId={request_id} already STORED — skip", flush=True)
            return False
        time.sleep(ACK_POLL_INTERVAL_SEC)
    print(
        f"[auto-stored] requestId={request_id} timed out waiting for ISSUED "
        f"({ACK_POLL_TIMEOUT_SEC}s) — skip STORED",
        flush=True,
    )
    return False


def post_stored(request_id: str, data_share_url: str | None) -> None:
    if not wait_until_issued(request_id):
        return
    body_obj = {
        "publisher": "LOCAL_WEBSUB_PARTNER_ACK",
        "topic": "CREDENTIAL_STATUS_UPDATE",
        "publishedOn": utc_now(),
        "event": {
            "id": f"local-stored-{request_id}",
            "requestId": request_id,
            "timestamp": utc_now(),
            "status": "STORED",
            "url": data_share_url or "",
        },
    }
    body = json.dumps(body_obj, separators=(",", ":"))
    headers = {
        "Content-Type": "application/json",
        "x-hub-signature": hmac_sha256_header(NOTIFY_SECRET, body),
    }
    req = urllib.request.Request(NOTIFY_URL, data=body.encode("utf-8"), headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            print(f"[auto-stored] requestId={request_id} notifyStatus HTTP {resp.status}", flush=True)
    except urllib.error.HTTPError as e:
        err = e.read().decode("utf-8", errors="replace")[:300]
        print(f"[auto-stored] requestId={request_id} notifyStatus HTTP {e.code}: {err}", flush=True)
    except Exception as e:  # noqa: BLE001
        print(f"[auto-stored] requestId={request_id} notifyStatus failed: {e}", flush=True)


def maybe_auto_stored(topic: str, body_bytes: bytes) -> None:
    if not topic or not topic.endswith(ISSUED_SUFFIX):
        return
    try:
        payload = json.loads(body_bytes.decode("utf-8"))
    except Exception:  # noqa: BLE001
        print(f"[auto-stored] non-JSON publish body for topic={topic}", flush=True)
        return
    event = payload.get("event") if isinstance(payload, dict) else None
    if not isinstance(event, dict):
        print(f"[auto-stored] no event in publish topic={topic}", flush=True)
        return
    request_id = None
    tid = event.get("transactionId")
    if isinstance(tid, str) and tid.strip():
        request_id = tid.strip()
    elif isinstance(event.get("requestId"), str) and event["requestId"].strip():
        request_id = event["requestId"].strip()
    if not request_id:
        print(f"[auto-stored] no transactionId in publish topic={topic}", flush=True)
        return
    data_share_url = event.get("dataShareUri") or event.get("url")
    if not (isinstance(data_share_url, str) and data_share_url.strip()):
        # Successful issue always includes dataShareUri; skip incomplete / error publishes.
        print(
            f"[auto-stored] requestId={request_id} missing dataShareUri — skip STORED",
            flush=True,
        )
        return
    threading.Thread(
        target=post_stored, args=(request_id, data_share_url.strip()), daemon=True
    ).start()


def intent_verify(callback: str, topic: str) -> bool:
    if not callback:
        return True
    challenge = f"local-challenge-{int(time.time())}"
    qs = urllib.parse.urlencode(
        {"hub.mode": "subscribe", "hub.topic": topic, "hub.challenge": challenge}
    )
    sep = "&" if ("?" in callback) else "?"
    url = f"{callback}{sep}{qs}"
    try:
        with urllib.request.urlopen(url, timeout=10) as resp:
            body = resp.read().decode("utf-8", errors="replace")
            ok = challenge in body
            print(f"[hub] intent-verify callback={callback} ok={ok}", flush=True)
            return ok
    except Exception as e:  # noqa: BLE001
        print(f"[hub] intent-verify failed callback={callback}: {e}", flush=True)
        return True


class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt: str, *args) -> None:
        print(f"[hub] {self.address_string()} {fmt % args}", flush=True)

    def _read(self) -> bytes:
        length = int(self.headers.get("Content-Length") or "0")
        return self.rfile.read(length) if length > 0 else b""

    def _accept(self) -> None:
        body = b"hub.mode=accepted"
        self.send_response(200)
        self.send_header("Content-Type", "application/x-www-form-urlencoded")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self) -> None:
        if self.path.startswith("/health"):
            body = b'{"status":"UP"}'
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return
        self._accept()

    def do_POST(self) -> None:
        raw = self._read()
        parsed = urllib.parse.urlparse(self.path)
        q = {k: v[0] for k, v in urllib.parse.parse_qs(parsed.query, keep_blank_values=True).items()}
        form = parse_form(raw) if raw and not q.get("hub.mode") else {}
        params = {**form, **q}
        mode = (params.get("hub.mode") or "").lower()
        topic = params.get("hub.topic") or ""

        if mode == "subscribe":
            intent_verify(params.get("hub.callback") or "", topic)
            self._accept()
            return

        if mode == "publish":
            maybe_auto_stored(topic, raw)
            self._accept()
            return

        self._accept()


def main() -> None:
    print(
        f"[hub] listening :{PORT} notifyStatus={NOTIFY_URL} "
        f"statusGet={STATUS_URL_BASE} auth={AUTH_TOKEN_URL} "
        f"delay={ACK_INITIAL_DELAY_SEC}s pollTimeout={ACK_POLL_TIMEOUT_SEC}s "
        f"secret_len={len(NOTIFY_SECRET)}",
        flush=True,
    )
    httpd = ThreadingHTTPServer(("0.0.0.0", PORT), Handler)
    httpd.serve_forever()


if __name__ == "__main__":
    main()
