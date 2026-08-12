#!/usr/bin/env python3
"""
Live authmanager → Keycloak bridge for local docker-compose.

WireMock proxies POST /v1/authmanager/authenticate/clientidsecretkey here.
Each call mints a fresh client_credentials JWT from local Keycloak (no hardcoded token).

  POST /v1/authmanager/authenticate/clientidsecretkey
  GET  /health
"""

from __future__ import annotations

import json
import os
import time
import urllib.error
import urllib.parse
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Any, Dict, Optional, Tuple

KEYCLOAK_URL = os.environ.get("KEYCLOAK_URL", "http://keycloak:8080").rstrip("/")
REALM = os.environ.get("KEYCLOAK_REALM", "mosip")
DEFAULT_CLIENT_ID = os.environ.get("DEFAULT_CLIENT_ID", "mosip-idrepo-client")
DEFAULT_CLIENT_SECRET = os.environ.get("DEFAULT_CLIENT_SECRET", "QTGizTYN4US0XHOU")
PORT = int(os.environ.get("PORT", "8086"))

# Must match keycloak/bootstrap_and_mint_token.py LOCAL_CLIENTS + mosip-config secrets.
CLIENT_SECRETS = {
    "mosip-idrepo-client": os.environ.get("IDREPO_CLIENT_SECRET", "QTGizTYN4US0XHOU"),
    "mosip-datsha-client": os.environ.get("DATSHA_CLIENT_SECRET", "nA8DF7lnUSw8zWS6"),
    "mosip-crereq-client": os.environ.get("CREREQ_CLIENT_SECRET", "Cd63IMonG2G9RHjS"),
    "mosip-creser-client": os.environ.get("CRESER_CLIENT_SECRET", "H5aL9iWnju1fxhwv"),
    "mosip-admin-client": os.environ.get("ADMIN_CLIENT_SECRET", "local-dev-secret"),
    "mosip-testrig-client": os.environ.get("TESTRIG_CLIENT_SECRET", "local-dev-testrig-secret"),
    "mosip-pms-client": os.environ.get("PMS_CLIENT_SECRET", "local-dev-secret"),
    "mosip-partner-client": os.environ.get("PARTNER_CLIENT_SECRET", "local-dev-secret"),
}


def _http_form(url: str, form: Dict[str, str]) -> Tuple[int, Any]:
    data = urllib.parse.urlencode(form).encode()
    req = urllib.request.Request(
        url,
        data=data,
        headers={"Content-Type": "application/x-www-form-urlencoded"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        raw = e.read().decode(errors="replace")
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"error": raw}


def mint_token(client_id: str, client_secret: str) -> Dict[str, Any]:
    code, body = _http_form(
        f"{KEYCLOAK_URL}/auth/realms/{REALM}/protocol/openid-connect/token",
        {
            "grant_type": "client_credentials",
            "client_id": client_id,
            "client_secret": client_secret,
            # Required for Keycloak /userinfo (online token validation)
            "scope": "openid profile email",
        },
    )
    if code != 200 or not isinstance(body, dict) or "access_token" not in body:
        raise RuntimeError(f"Keycloak token failed for client_id={client_id}: {code} {body}")
    return body


def extract_credentials(payload: Dict[str, Any]) -> Tuple[str, Optional[str]]:
    """Return (client_id, requested_secret_or_none)."""
    req = payload.get("request") if isinstance(payload.get("request"), dict) else payload
    client_id = (
        req.get("clientId")
        or req.get("client_id")
        or payload.get("clientId")
        or DEFAULT_CLIENT_ID
    )
    client_id = str(client_id)
    requested = (
        req.get("secretKey")
        or req.get("clientSecret")
        or req.get("client_secret")
        or payload.get("secretKey")
    )
    return client_id, (str(requested) if requested else None)


def secret_candidates(client_id: str, requested: Optional[str]) -> list[str]:
    """Try request secret first, then known local map (handles config drift)."""
    out: list[str] = []
    for s in (requested, CLIENT_SECRETS.get(client_id), DEFAULT_CLIENT_SECRET):
        if s and s not in out:
            out.append(s)
    return out


def mint_with_fallback(client_id: str, requested: Optional[str]) -> Tuple[Dict[str, Any], str]:
    last_err: Optional[Exception] = None
    for secret in secret_candidates(client_id, requested):
        try:
            return mint_token(client_id, secret), secret
        except RuntimeError as e:
            last_err = e
    raise RuntimeError(str(last_err) if last_err else f"no secret candidates for {client_id}")


def extract_bearer_token(handler: BaseHTTPRequestHandler) -> Optional[str]:
    """Accept Authorization: Bearer, Cookie Authorization=, or Cookie Bearer-style MOSIP headers."""
    # Collect candidate header values (MOSIP uses Cookie: Authorization=<jwt>)
    candidates: list[str] = []
    for name in ("Authorization", "authorization", "Cookie", "cookie"):
        val = handler.headers.get(name)
        if val:
            candidates.append(val.strip())

    for raw in candidates:
        # Full header may be "Authorization=<jwt>" (sent as Cookie value) or "Bearer <jwt>"
        if raw.lower().startswith("bearer "):
            return raw[7:].strip()
        if raw.lower().startswith("authorization="):
            val = raw.split("=", 1)[1].strip().strip(";")
            if val.lower().startswith("bearer "):
                return val[7:].strip()
            return val
        # Cookie list: Authorization=<jwt>; Path=/ ...
        for part in raw.split(";"):
            part = part.strip()
            lower = part.lower()
            if lower.startswith("authorization="):
                val = part.split("=", 1)[1].strip()
                if val.lower().startswith("bearer "):
                    return val[7:].strip()
                return val
            if lower.startswith("bearer "):
                return part[7:].strip()
        # Bare JWT (three base64 segments)
        if raw.count(".") == 2 and " " not in raw and "=" not in raw[:20]:
            return raw
    return None


def decode_jwt_payload(access_token: str) -> Optional[Dict[str, Any]]:
    try:
        import base64

        payload_b64 = access_token.split(".")[1]
        pad = "=" * (-len(payload_b64) % 4)
        return json.loads(base64.urlsafe_b64decode(payload_b64 + pad))
    except Exception:  # noqa: BLE001
        return None


def keycloak_userinfo(access_token: str) -> Tuple[int, bytes]:
    url = f"{KEYCLOAK_URL}/auth/realms/{REALM}/protocol/openid-connect/userinfo"
    req = urllib.request.Request(
        url,
        headers={"Authorization": f"Bearer {access_token}"},
        method="GET",
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return resp.status, resp.read()
    except urllib.error.HTTPError as e:
        return e.code, e.read()


def userinfo_from_jwt(access_token: str) -> Optional[bytes]:
    """
    Local fallback when Keycloak userinfo returns 401/403 (common for
    client_credentials tokens minted without openid scope).
    Builds a userinfo-shaped body from JWT claims — no ValidateTokenHelper changes.
    """
    payload = decode_jwt_payload(access_token)
    if not payload:
        return None
    now = int(time.time())
    exp = payload.get("exp")
    if isinstance(exp, int) and exp < now:
        return None
    body = {
        "sub": payload.get("sub"),
        "preferred_username": payload.get("preferred_username") or payload.get("clientId"),
        "email": payload.get("email"),
        "email_verified": payload.get("email_verified", False),
        "azp": payload.get("azp"),
        "clientId": payload.get("client_id") or payload.get("azp"),
        "realm_access": payload.get("realm_access") or {},
        "resource_access": payload.get("resource_access") or {},
        "scope": payload.get("scope"),
    }
    return json.dumps(body).encode()


def resolve_userinfo(access_token: str) -> Tuple[int, bytes]:
    code, body = keycloak_userinfo(access_token)
    if code == 200 and body:
        return code, body
    # Keycloak often denies userinfo for client_credentials without openid → 403 {}
    fallback = userinfo_from_jwt(access_token)
    if fallback is not None:
        print(f"userinfo keycloak={code} → jwt-fallback 200")
        return 200, fallback
    print(f"userinfo keycloak={code} no fallback")
    return code, body if body else b"{}"


class Handler(BaseHTTPRequestHandler):
    # WireMock proxy often resets keep-alive; avoid ConnectionResetError spam.
    protocol_version = "HTTP/1.0"
    close_connection = True

    def log_message(self, fmt: str, *args: Any) -> None:
        if args and str(args[0]).startswith("GET /health"):
            return
        super().log_message(fmt, *args)

    def handle(self) -> None:
        try:
            super().handle()
        except (ConnectionResetError, BrokenPipeError, ConnectionAbortedError):
            pass

    def finish(self) -> None:
        try:
            super().finish()
        except (ConnectionResetError, BrokenPipeError, ConnectionAbortedError):
            pass

    def _send(self, status: int, body: bytes, headers: Optional[Dict[str, str]] = None) -> None:
        self.send_response(status)
        hdrs = {
            "Content-Type": "application/json",
            "Content-Length": str(len(body)),
            "Connection": "close",
        }
        if headers:
            hdrs.update(headers)
        for k, v in hdrs.items():
            self.send_header(k, v)
        self.end_headers()
        try:
            self.wfile.write(body)
            self.wfile.flush()
        except (ConnectionResetError, BrokenPipeError, ConnectionAbortedError):
            pass

    def do_GET(self) -> None:  # noqa: N802
        path = self.path.split("?", 1)[0]
        if path == "/health":
            self._send(200, b'{"status":"UP"}')
            return
        # MOSIP ValidateTokenHelper online path (Cookie Authorization=...) → Keycloak Bearer
        if path in (
            "/v1/oidc/userinfo",
            f"/auth/realms/{REALM}/protocol/openid-connect/userinfo",
        ) or path.endswith("/protocol/openid-connect/userinfo"):
            token = extract_bearer_token(self)
            if not token:
                self._send(401, b'{"error":"invalid_request","error_description":"missing token"}')
                return
            code, body = resolve_userinfo(token)
            self._send(code, body if body else b"{}")
            return
        self._send(404, b'{"error":"not found"}')

    def do_POST(self) -> None:  # noqa: N802
        path = self.path.split("?", 1)[0]
        if path == "/v1/authmanager/authenticate/clientidsecretkey":
            self._handle_clientidsecretkey()
            return
        if path in (
            "/v1/authmanager/authenticate/useridPwd",
            "/v1/authmanager/authenticate/internal/useridPwd",
        ):
            self._handle_userid_pwd()
            return
        self._send(404, b'{"error":"not found"}')

    def _handle_clientidsecretkey(self) -> None:
        length = int(self.headers.get("Content-Length") or "0")
        raw = self.rfile.read(length) if length else b"{}"
        try:
            payload = json.loads(raw.decode() or "{}")
        except json.JSONDecodeError:
            payload = {}

        try:
            client_id, requested = extract_credentials(
                payload if isinstance(payload, dict) else {}
            )
            minted, used_secret = mint_with_fallback(client_id, requested)
            access = minted["access_token"]
            body = {
                "id": payload.get("id") if isinstance(payload, dict) else "string",
                "version": payload.get("version") if isinstance(payload, dict) else "v1",
                "responsetime": time.strftime("%Y-%m-%dT%H:%M:%S.000Z", time.gmtime()),
                "metadata": None,
                "response": {
                    "status": "Success",
                    "message": "Clientid and Token combination had been validated successfully",
                },
                "errors": None,
            }
            data = json.dumps(body).encode()
            self._send(
                200,
                data,
                {
                    "authorization": access,
                    "access-control-expose-headers": "set-cookie,authorization",
                    "set-cookie": f"Authorization={access}; Path=/; HttpOnly",
                },
            )
            src = (
                "request"
                if requested and used_secret == requested
                else ("known" if CLIENT_SECRETS.get(client_id) == used_secret else "default")
            )
            print(
                f"minted token client_id={client_id} expires_in={minted.get('expires_in')} "
                f"secret_source={src}"
            )
        except Exception as e:  # noqa: BLE001
            err = {
                "id": None,
                "version": "v1",
                "responsetime": time.strftime("%Y-%m-%dT%H:%M:%S.000Z", time.gmtime()),
                "response": None,
                "errors": [{"errorCode": "KER-ATH-401", "message": str(e)}],
            }
            self._send(401, json.dumps(err).encode())
            print(f"token mint error: {e}")

    def _handle_userid_pwd(self) -> None:
        """apitest KernelAuthentication useridPwd — body must include response.token."""
        length = int(self.headers.get("Content-Length") or "0")
        raw = self.rfile.read(length) if length else b"{}"
        try:
            payload = json.loads(raw.decode() or "{}")
        except json.JSONDecodeError:
            payload = {}

        req = payload.get("request") if isinstance(payload, dict) else {}
        if not isinstance(req, dict):
            req = {}
        client_id = req.get("clientId") or req.get("client_id") or DEFAULT_CLIENT_ID
        requested = req.get("clientSecret") or req.get("secretKey") or req.get("client_secret")
        try:
            minted, _used = mint_with_fallback(str(client_id), requested)
            access = minted["access_token"]
            body = {
                "id": payload.get("id") if isinstance(payload, dict) else "string",
                "version": payload.get("version") if isinstance(payload, dict) else "v1",
                "responsetime": time.strftime("%Y-%m-%dT%H:%M:%S.000Z", time.gmtime()),
                "metadata": None,
                "response": {
                    "status": "Success",
                    "message": "Username and password combination had been validated successfully",
                    "token": access,
                    "refreshToken": access,
                    "expiryTime": minted.get("expires_in"),
                },
                "errors": None,
            }
            data = json.dumps(body).encode()
            self._send(
                200,
                data,
                {
                    "authorization": access,
                    "access-control-expose-headers": "set-cookie,authorization",
                    "set-cookie": f"Authorization={access}; Path=/; HttpOnly",
                },
            )
            print(
                f"useridPwd minted client_id={client_id} user={req.get('userName')} "
                f"expires_in={minted.get('expires_in')}"
            )
        except Exception as e:  # noqa: BLE001
            err = {
                "id": None,
                "version": "v1",
                "responsetime": time.strftime("%Y-%m-%dT%H:%M:%S.000Z", time.gmtime()),
                "response": None,
                "errors": [{"errorCode": "KER-ATH-401", "message": str(e)}],
            }
            self._send(401, json.dumps(err).encode())
            print(f"useridPwd token mint error: {e}")


def main() -> None:
    print(f"auth-token-bridge listening on :{PORT} → {KEYCLOAK_URL}/auth/realms/{REALM}")
    print(f"known clients: {', '.join(sorted(CLIENT_SECRETS))}")
    ThreadingHTTPServer(("0.0.0.0", PORT), Handler).serve_forever()


if __name__ == "__main__":
    main()
