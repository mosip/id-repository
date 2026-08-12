#!/usr/bin/env python3
"""
Host-facing gateway on :8082.

WireMock/Jetty rewrites known headers to canonical form (set-cookie → Set-Cookie).
Auth mint paths bypass WireMock and go straight to auth-token-bridge so casing is preserved.
All other paths proxy to WireMock.
"""

from __future__ import annotations

import os
from http.client import HTTPConnection
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Iterable, Tuple
from urllib.parse import urlsplit

PORT = int(os.environ.get("PORT", "8082"))
WIREMOCK = os.environ.get("WIREMOCK_URL", "http://mock-service:8082").rstrip("/")
AUTH_BRIDGE = os.environ.get("AUTH_BRIDGE_URL", "http://auth-token-bridge:8086").rstrip("/")

# Paths that must keep lowercase set-cookie from the Python bridge.
AUTH_MINT_PREFIXES = (
    "/v1/authmanager/authenticate/clientidsecretkey",
    "/v1/authmanager/authenticate/useridPwd",
    "/v1/authmanager/authenticate/internal/useridPwd",
)

HOP_BY_HOP = {
    "connection",
    "keep-alive",
    "proxy-authenticate",
    "proxy-authorization",
    "te",
    "trailers",
    "transfer-encoding",
    "upgrade",
    "server",  # avoid duplicate Server (gateway + upstream)
}


def _split_url(base: str) -> Tuple[str, int]:
    u = urlsplit(base)
    host = u.hostname or "127.0.0.1"
    port = u.port or (443 if u.scheme == "https" else 80)
    return host, port


def _upstream_for(path: str) -> str:
    bare = path.split("?", 1)[0]
    for prefix in AUTH_MINT_PREFIXES:
        if bare == prefix or bare.startswith(prefix + "/"):
            return AUTH_BRIDGE
    return WIREMOCK


class GatewayHandler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, fmt: str, *args) -> None:  # noqa: A003
        print(f"gateway {self.address_string()} {fmt % args}")

    def do_GET(self) -> None:  # noqa: N802
        self._proxy()

    def do_POST(self) -> None:  # noqa: N802
        self._proxy()

    def do_PUT(self) -> None:  # noqa: N802
        self._proxy()

    def do_PATCH(self) -> None:  # noqa: N802
        self._proxy()

    def do_DELETE(self) -> None:  # noqa: N802
        self._proxy()

    def do_HEAD(self) -> None:  # noqa: N802
        self._proxy()

    def do_OPTIONS(self) -> None:  # noqa: N802
        self._proxy()

    def _proxy(self) -> None:
        if self.path.split("?", 1)[0] == "/health":
            body = b'{"status":"UP"}'
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(body)))
            self.send_header("Connection", "close")
            self.end_headers()
            self.wfile.write(body)
            return

        length = int(self.headers.get("Content-Length") or "0")
        body = self.rfile.read(length) if length else b""
        upstream = _upstream_for(self.path)
        host, port = _split_url(upstream)

        headers = {
            k: v
            for k, v in self.headers.items()
            if k.lower() not in HOP_BY_HOP and k.lower() != "host"
        }
        headers["Connection"] = "close"
        headers["Host"] = f"{host}:{port}"

        conn = HTTPConnection(host, port, timeout=60)
        try:
            conn.request(self.command, self.path, body=body or None, headers=headers)
            resp = conn.getresponse()
            resp_body = resp.read()
            self.send_response(resp.status, resp.reason)
            # Preserve upstream header name casing (critical for set-cookie).
            for name, value in resp.getheaders():
                if name.lower() in HOP_BY_HOP:
                    continue
                self.send_header(name, value)
            if not any(n.lower() == "content-length" for n, _ in resp.getheaders()):
                self.send_header("Content-Length", str(len(resp_body)))
            self.send_header("Connection", "close")
            self.end_headers()
            if self.command != "HEAD":
                self.wfile.write(resp_body)
                self.wfile.flush()
        finally:
            conn.close()


def main() -> None:
    server = ThreadingHTTPServer(("0.0.0.0", PORT), GatewayHandler)
    print(f"mock-gateway listening on :{PORT} wiremock={WIREMOCK} auth={AUTH_BRIDGE}")
    server.serve_forever()


if __name__ == "__main__":
    main()
