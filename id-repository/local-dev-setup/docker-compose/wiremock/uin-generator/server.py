#!/usr/bin/env python3
"""
Local MOSIP UIN/VID generator for docker-compose WireMock proxy.

Filters mirror kernel VidFilterUtils / UIN filter rules used by id-repo validators
(KER-IDV-002): sequence, repeating digit, repeating block, not-start-with, restricted.

Endpoints:
  GET /v1/idgenerator/uin  -> 10-digit UIN
  GET /v1/idgenerator/vid  -> 16-digit VID
  GET /health
"""

from __future__ import annotations

import json
import random
import re
import secrets
import threading
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse

_rng = random.SystemRandom()

# Verhoeff tables (Apache Commons / MOSIP-compatible)
_D = [
    [0, 1, 2, 3, 4, 5, 6, 7, 8, 9],
    [1, 2, 3, 4, 0, 6, 7, 8, 9, 5],
    [2, 3, 4, 0, 1, 7, 8, 9, 5, 6],
    [3, 4, 0, 1, 2, 8, 9, 5, 6, 7],
    [4, 0, 1, 2, 3, 9, 5, 6, 7, 8],
    [5, 9, 8, 7, 6, 0, 4, 3, 2, 1],
    [6, 5, 9, 8, 7, 1, 0, 4, 3, 2],
    [7, 6, 5, 9, 8, 2, 1, 0, 4, 3],
    [8, 7, 6, 5, 9, 3, 2, 1, 0, 4],
    [9, 8, 7, 6, 5, 4, 3, 2, 1, 0],
]
_P = [
    [0, 1, 2, 3, 4, 5, 6, 7, 8, 9],
    [1, 5, 7, 6, 2, 8, 3, 0, 9, 4],
    [5, 8, 0, 3, 7, 9, 6, 1, 4, 2],
    [8, 9, 1, 6, 0, 4, 3, 5, 2, 7],
    [9, 4, 5, 3, 1, 2, 6, 8, 7, 0],
    [4, 2, 8, 6, 5, 7, 3, 9, 0, 1],
    [2, 7, 9, 3, 8, 0, 6, 4, 1, 5],
    [7, 0, 4, 6, 9, 1, 3, 2, 5, 8],
]
_INV = [0, 4, 3, 2, 1, 5, 6, 7, 8, 9]

_SEQ_ASC = "0123456789"
_SEQ_DEC = "9876543210"

# From application-default.properties
_UIN_LEN = 10
_VID_LEN = 16
_SEQUENCE_LIMIT = 3
_REPEATING_LIMIT = 2  # (\d)\d{0,1}\1  → 11, 1x1
_REPEATING_BLOCK_LIMIT = 2  # (\d{2,}).*?\1
_RESTRICTED = ("786", "666")
_NOT_START = ("0", "1")

# Compiled like VidFilterUtils.initializeRegEx()
_REPEATING_RE = re.compile(rf"(\d)\d{{0,{_REPEATING_LIMIT - 1}}}\1")
_REPEATING_BLOCK_RE = re.compile(rf"(\d{{{_REPEATING_BLOCK_LIMIT},}}).*?\1")

_lock = threading.Lock()
_counter = 0
_recent_uin: set[str] = set()
_recent_vid: set[str] = set()
_RECENT_MAX = 5000


def verhoeff_checksum(digits: str, includes_check: bool) -> int:
    checksum = 0
    for i in range(len(digits)):
        idx = len(digits) - (i + 1)
        num = ord(digits[idx]) - 48
        pos = i if includes_check else i + 1
        checksum = _D[checksum][_P[pos % 8][num]]
    return checksum


def verhoeff_check_digit(body: str) -> str:
    return str(_INV[verhoeff_checksum(body, False)])


def verhoeff_valid(code: str) -> bool:
    return code.isdigit() and verhoeff_checksum(code, True) == 0


def sequence_filter(value: str, limit: int = _SEQUENCE_LIMIT) -> bool:
    """True if INVALID (contains ascending/descending run of `limit` digits)."""
    if limit <= 0:
        return False
    for i in range(0, len(value) - limit + 1):
        sub = value[i : i + limit]
        if sub in _SEQ_ASC or sub in _SEQ_DEC:
            return True
    return False


def conjugative_even_filter(value: str, limit: int = 3) -> bool:
    """UIN-only: three or more adjacent even digits."""
    run = 0
    for ch in value:
        if int(ch) % 2 == 0:
            run += 1
            if run >= limit:
                return True
        else:
            run = 0
    return False


def reverse_digits_filter(value: str, half: int = 5) -> bool:
    """UIN: first half must not equal last half or reverse(last half)."""
    if len(value) < 2 * half:
        return False
    first = value[:half]
    last = value[-half:]
    return first == last or first == last[::-1]


def mosip_vid_ok(value: str) -> bool:
    """Match VidFilterUtils.isValidId (inverted: True = acceptable)."""
    if len(value) != _VID_LEN or not value.isdigit():
        return False
    if any(value.startswith(s) for s in _NOT_START):
        return False
    if any(r in value for r in _RESTRICTED):
        return False
    if sequence_filter(value):
        return False
    if _REPEATING_RE.search(value):
        return False
    if _REPEATING_BLOCK_RE.search(value):
        return False
    return True


def mosip_uin_ok(value: str) -> bool:
    """VID filters + common UIN extras from kernel docs."""
    if len(value) != _UIN_LEN or not value.isdigit():
        return False
    if any(value.startswith(s) for s in _NOT_START):
        return False
    if any(r in value for r in _RESTRICTED):
        return False
    if sequence_filter(value):
        return False
    if _REPEATING_RE.search(value):
        return False
    if _REPEATING_BLOCK_RE.search(value):
        return False
    if conjugative_even_filter(value):
        return False
    if reverse_digits_filter(value, 5):
        return False
    return True


def _next_body(body_len: int) -> str:
    """SecureRandom-style body (see VidGeneratorImpl), first digit never 0/1."""
    global _counter
    _counter += 1
    first = 2 + secrets.randbelow(8)
    rest = "".join(str(secrets.randbelow(10)) for _ in range(body_len - 1))
    return f"{first}{rest}"


def _build_filtered_body(body_len: int) -> str | None:
    """
    Construct digits left-to-right avoiding VID filters as far as possible.
    Check digit is appended later and may still fail — caller retries.
    """
    digits: list[str] = []
    for i in range(body_len):
        choices = [str(d) for d in range(2, 10)] if i == 0 else [str(d) for d in range(10)]
        _rng.shuffle(choices)
        placed = False
        for ch in choices:
            trial = "".join(digits) + ch
            if any(r in trial for r in _RESTRICTED):
                continue
            if sequence_filter(trial):
                continue
            if _REPEATING_RE.search(trial):
                continue
            if _REPEATING_BLOCK_RE.search(trial):
                continue
            digits.append(ch)
            placed = True
            break
        if not placed:
            return None
    return "".join(digits)


def _generate(length: int, recent: set[str], validator) -> str:
    body_len = length - 1
    # Prefer constructive build (much higher hit rate for 16-digit VID filters)
    for _ in range(20_000):
        body = _build_filtered_body(body_len) or _next_body(body_len)
        value = body + verhoeff_check_digit(body)
        if not verhoeff_valid(value) or not validator(value):
            continue
        if value in recent:
            continue
        recent.add(value)
        if len(recent) > _RECENT_MAX:
            recent.pop()
        return value
    raise RuntimeError(f"unable to generate a valid id of length {length}")


def generate_uin() -> str:
    with _lock:
        return _generate(_UIN_LEN, _recent_uin, mosip_uin_ok)


def generate_vid() -> str:
    with _lock:
        return _generate(_VID_LEN, _recent_vid, mosip_vid_ok)


def _now() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + "Z"


class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt: str, *args) -> None:
        pass

    def _json(self, code: int, payload: dict) -> None:
        raw = json.dumps(payload).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(raw)))
        self.end_headers()
        self.wfile.write(raw)

    def do_GET(self) -> None:  # noqa: N802
        path = urlparse(self.path).path.rstrip("/") or "/"
        if path in ("/health", "/__admin/health"):
            self._json(200, {"status": "UP"})
            return

        now = _now()
        if path.startswith("/v1/idgenerator/uin"):
            try:
                uin = generate_uin()
            except Exception as exc:  # noqa: BLE001
                self._json(
                    500,
                    {
                        "id": "mosip.uin.status.update",
                        "version": "v1",
                        "responsetime": now,
                        "metadata": None,
                        "response": None,
                        "errors": [{"errorCode": "UIN-GEN-001", "message": str(exc)}],
                    },
                )
                return
            self._json(
                200,
                {
                    "id": "mosip.uin.status.update",
                    "version": "v1",
                    "responsetime": now,
                    "metadata": None,
                    "response": {"uin": uin, "status": "UNUSED"},
                    "errors": None,
                },
            )
            return

        if path.startswith("/v1/idgenerator/vid"):
            try:
                vid = generate_vid()
            except Exception as exc:  # noqa: BLE001
                self._json(
                    500,
                    {
                        "id": "mosip.vid.create",
                        "version": "v1",
                        "responsetime": now,
                        "metadata": None,
                        "response": None,
                        "errors": [{"errorCode": "VID-GEN-001", "message": str(exc)}],
                    },
                )
                return
            self._json(
                200,
                {
                    "id": "mosip.vid.create",
                    "version": "v1",
                    "responsetime": now,
                    "metadata": None,
                    "response": {"vid": vid},
                    "errors": None,
                },
            )
            return

        self._json(404, {"errors": [{"errorCode": "NOT_FOUND", "message": path}]})


def main() -> None:
    host, port = "0.0.0.0", 8084
    httpd = ThreadingHTTPServer((host, port), Handler)
    print(f"id-generator (uin+vid) listening on {host}:{port}", flush=True)
    httpd.serve_forever()


if __name__ == "__main__":
    main()
