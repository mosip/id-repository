#!/usr/bin/env python3
"""Smoke: HMAC notifyStatus + hub publish for one ISSUED request id."""
from __future__ import annotations

import hashlib
import hmac
import json
import pathlib
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone

RID = sys.argv[1] if len(sys.argv) > 1 else "5a491297-f929-4f5f-8bac-3eb0748fb5d6"
NOTIFY = "http://localhost:8090/v1/credentialrequest/callback/notifyStatus"
HUB = "http://localhost:8082/hub/?hub.mode=publish&hub.topic=mpartner-default-auth/CREDENTIAL_ISSUED"


def now() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + "Z"


def notify_stored(request_id: str) -> None:
    body = json.dumps(
        {
            "publisher": "LOCAL",
            "topic": "CREDENTIAL_STATUS_UPDATE",
            "publishedOn": now(),
            "event": {
                "id": "e1",
                "requestId": request_id,
                "timestamp": now(),
                "status": "STORED",
                "url": "http://localhost:8082/v1/datashare/get/x",
            },
        },
        separators=(",", ":"),
    )
    sig = hmac.new(b"test", body.encode(), hashlib.sha256).hexdigest()
    req = urllib.request.Request(
        NOTIFY,
        data=body.encode(),
        headers={"Content-Type": "application/json", "x-hub-signature": f"SHA256={sig}"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            print("direct_notify", resp.status, resp.read()[:200])
    except urllib.error.HTTPError as e:
        print("direct_notify_http", e.code, e.read()[:400])
    except Exception as e:  # noqa: BLE001
        print("direct_notify_err", e)


def hub_publish(request_id: str) -> None:
    payload = {
        "publisher": "CREDENTIAL_SERVICE",
        "topic": "mpartner-default-auth/CREDENTIAL_ISSUED",
        "publishedOn": now(),
        "event": {
            "id": "evt-1",
            "transactionId": request_id,
            "timestamp": now(),
            "dataShareUri": "http://localhost:8082/v1/datashare/get/x",
        },
    }
    raw = json.dumps(payload).encode()
    req = urllib.request.Request(
        HUB, data=raw, headers={"Content-Type": "application/json"}, method="POST"
    )
    with urllib.request.urlopen(req, timeout=15) as resp:
        print("hub_publish", resp.status, resp.read()[:80])


if __name__ == "__main__":
    mode = sys.argv[2] if len(sys.argv) > 2 else "both"
    if mode in ("direct", "both"):
        notify_stored(RID)
    if mode in ("hub", "both"):
        hub_publish(RID)
        time.sleep(2)
