#!/usr/bin/env python3
"""
Bootstrap local Keycloak mosip realm (roles + service-account, ~10y token lifespan).

By default does NOT write WireMock stubs — live tokens come from auth-token-bridge
(proxied via wiremock/mappings/auth-client-token.json → Keycloak).

Set WRITE_WIREMOCK_STUBS=true only if you want a static snapshot (legacy).

Usage (from docker-compose/):
  python keycloak/bootstrap_and_mint_token.py
"""

from __future__ import annotations

import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Any, Dict, List, Optional

KEYCLOAK_URL = os.environ.get("KEYCLOAK_URL", "http://localhost:8081").rstrip("/")
ADMIN_USER = os.environ.get("KEYCLOAK_ADMIN", "admin")
ADMIN_PASSWORD = os.environ.get("KEYCLOAK_ADMIN_PASSWORD", "admin")
REALM = os.environ.get("KEYCLOAK_REALM", "mosip")
CLIENT_ID = os.environ.get("IDREPO_CLIENT_ID", "mosip-idrepo-client")
CLIENT_SECRET = os.environ.get("IDREPO_CLIENT_SECRET", "QTGizTYN4US0XHOU")
# All local MOSIP clients that call clientidsecretkey / OIDC client_credentials
LOCAL_CLIENTS = [
    ("mosip-idrepo-client", os.environ.get("IDREPO_CLIENT_SECRET", "QTGizTYN4US0XHOU")),
    ("mosip-datsha-client", os.environ.get("DATSHA_CLIENT_SECRET", "nA8DF7lnUSw8zWS6")),
    ("mosip-crereq-client", os.environ.get("CREREQ_CLIENT_SECRET", "Cd63IMonG2G9RHjS")),
    ("mosip-creser-client", os.environ.get("CRESER_CLIENT_SECRET", "H5aL9iWnju1fxhwv")),
    ("mosip-admin-client", os.environ.get("ADMIN_CLIENT_SECRET", "local-dev-secret")),
    # api-test KeycloakUserManager uses client_credentials as mosip-testrig-client
    ("mosip-testrig-client", os.environ.get("TESTRIG_CLIENT_SECRET", "local-dev-testrig-secret")),
    # api-test PartnerRegistration.deviceGeneration() authenticates as mosip-pms-client
    ("mosip-pms-client", os.environ.get("PMS_CLIENT_SECRET", "local-dev-secret")),
    ("mosip-partner-client", os.environ.get("PARTNER_CLIENT_SECRET", "local-dev-secret")),
]
# ~10 years
TOKEN_LIFESPAN_SECS = int(os.environ.get("TOKEN_LIFESPAN_SECS", "315360000"))
WRITE_WIREMOCK_STUBS = os.environ.get("WRITE_WIREMOCK_STUBS", "false").lower() in (
    "1",
    "true",
    "yes",
)

ROOT = Path(os.environ.get("COMPOSE_DIR", Path(__file__).resolve().parent.parent))
AUTH_CLIENT_TOKEN = ROOT / "wiremock" / "mappings" / "auth-client-token.json"
OIDC_TOKEN = ROOT / "wiremock" / "mappings" / "keycloak-oidc-token.json"

REALM_ROLES = [
    "ID_REPOSITORY",
    "CREATE_SHARE",
    "CREDENTIAL_REQUEST",
    "CREDENTIAL_ISSUANCE",
    "REGISTRATION_PROCESSOR",
    "POLICYMANAGER",
    "PUBLISH_IDENTITY_CREATED_GENERAL",
    "PUBLISH_IDENTITY_UPDATED_GENERAL",
    "PUBLISH_ACTIVATE_ID_ALL_INDIVIDUAL",
    "PUBLISH_DEACTIVATE_ID_ALL_INDIVIDUAL",
    "PUBLISH_REMOVE_ID_ALL_INDIVIDUAL",
    "PUBLISH_AUTH_TYPE_STATUS_UPDATE_ALL_INDIVIDUAL",
    "PUBLISH_CREDENTIAL_ISSUED_ALL_INDIVIDUAL",
    "PUBLISH_VID_CRED_STATUS_UPDATE_GENERAL",
    "PUBLISH_AUTHENTICATION_TRANSACTION_STATUS_GENERAL",
    "SUBSCRIBE_CREDENTIAL_STATUS_UPDATE_GENERAL",
    "SUBSCRIBE_VID_CRED_STATUS_UPDATE_GENERAL",
    "SUBSCRIBE_REMOVE_ID_STATUS_GENERAL",
    "offline_access",
    "uma_authorization",
    "default-roles-mosip",
]


def _http(
    method: str,
    url: str,
    data: Optional[bytes] = None,
    headers: Optional[Dict[str, str]] = None,
    form: Optional[Dict[str, str]] = None,
) -> tuple[int, Any]:
    hdrs = dict(headers or {})
    body = data
    if form is not None:
        body = urllib.parse.urlencode(form).encode()
        hdrs.setdefault("Content-Type", "application/x-www-form-urlencoded")
    req = urllib.request.Request(url, data=body, headers=hdrs, method=method)
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            raw = resp.read()
            if not raw:
                return resp.status, None
            ctype = resp.headers.get("Content-Type", "")
            if "application/json" in ctype:
                return resp.status, json.loads(raw.decode())
            return resp.status, raw.decode()
    except urllib.error.HTTPError as e:
        err = e.read().decode(errors="replace")
        try:
            return e.code, json.loads(err)
        except Exception:
            return e.code, err


def wait_ready(timeout: int = 180) -> None:
    deadline = time.time() + timeout
    url = f"{KEYCLOAK_URL}/auth/realms/master"
    while time.time() < deadline:
        try:
            code, _ = _http("GET", url)
            if code == 200:
                print(f"keycloak ready: {url}")
                return
        except Exception as e:  # noqa: BLE001
            pass
        time.sleep(3)
    raise SystemExit(f"Keycloak not ready at {KEYCLOAK_URL} within {timeout}s")


def admin_token() -> str:
    code, body = _http(
        "POST",
        f"{KEYCLOAK_URL}/auth/realms/master/protocol/openid-connect/token",
        form={
            "grant_type": "password",
            "client_id": "admin-cli",
            "username": ADMIN_USER,
            "password": ADMIN_PASSWORD,
        },
    )
    if code != 200 or not isinstance(body, dict) or "access_token" not in body:
        raise SystemExit(f"admin token failed: {code} {body}")
    return body["access_token"]


def auth_headers(token: str) -> Dict[str, str]:
    return {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}


def ensure_realm(token: str) -> None:
    code, realms = _http("GET", f"{KEYCLOAK_URL}/auth/admin/realms", headers=auth_headers(token))
    if code == 200 and isinstance(realms, list) and any(r.get("realm") == REALM for r in realms):
        print(f"realm exists: {REALM}")
        # enforce long-lived tokens
        _http(
            "PUT",
            f"{KEYCLOAK_URL}/auth/admin/realms/{REALM}",
            data=json.dumps(
                {
                    "realm": REALM,
                    "enabled": True,
                    "sslRequired": "none",
                    "accessTokenLifespan": TOKEN_LIFESPAN_SECS,
                    "ssoSessionIdleTimeout": TOKEN_LIFESPAN_SECS,
                    "ssoSessionMaxLifespan": TOKEN_LIFESPAN_SECS,
                    "clientSessionIdleTimeout": TOKEN_LIFESPAN_SECS,
                    "clientSessionMaxLifespan": TOKEN_LIFESPAN_SECS,
                }
            ).encode(),
            headers=auth_headers(token),
        )
        return
    # Prefer import file if present
    import_path = Path(__file__).resolve().parent / "mosip-realm.json"
    if import_path.is_file():
        code, body = _http(
            "POST",
            f"{KEYCLOAK_URL}/auth/admin/realms",
            data=import_path.read_bytes(),
            headers=auth_headers(token),
        )
        if code in (201, 204, 409):
            print(f"realm imported/created: {REALM} ({code})")
            return
        print(f"realm import POST status={code} body={body}")
    raise SystemExit(f"failed to ensure realm {REALM}")


def ensure_roles(token: str) -> None:
    code, existing = _http(
        "GET", f"{KEYCLOAK_URL}/auth/admin/realms/{REALM}/roles", headers=auth_headers(token)
    )
    have = {r["name"] for r in existing} if code == 200 and isinstance(existing, list) else set()
    for role in REALM_ROLES:
        if role in have:
            continue
        c, b = _http(
            "POST",
            f"{KEYCLOAK_URL}/auth/admin/realms/{REALM}/roles",
            data=json.dumps({"name": role}).encode(),
            headers=auth_headers(token),
        )
        if c not in (201, 204, 409):
            print(f"warn: create role {role}: {c} {b}")
    print(f"roles ensured ({len(REALM_ROLES)})")


def ensure_client(token: str, client_id: str, client_secret: str) -> str:
    q = urllib.parse.urlencode({"clientId": client_id})
    code, clients = _http(
        "GET",
        f"{KEYCLOAK_URL}/auth/admin/realms/{REALM}/clients?{q}",
        headers=auth_headers(token),
    )
    if code == 200 and isinstance(clients, list) and clients:
        cid = clients[0]["id"]
        # Drop read-only / masked fields that break secret updates
        body = {k: v for k, v in clients[0].items() if k not in ("secret", "access")}
        body.update(
            {
                "secret": client_secret,
                "clientAuthenticatorType": "client-secret",
                "serviceAccountsEnabled": True,
                "directAccessGrantsEnabled": True,
                "publicClient": False,
                "attributes": {
                    **(clients[0].get("attributes") or {}),
                    "access.token.lifespan": str(TOKEN_LIFESPAN_SECS),
                },
            }
        )
        _http(
            "PUT",
            f"{KEYCLOAK_URL}/auth/admin/realms/{REALM}/clients/{cid}",
            data=json.dumps(body).encode(),
            headers=auth_headers(token),
        )
        # Verify secret works; if Keycloak ignored it, delete + recreate
        probe = _http(
            "POST",
            f"{KEYCLOAK_URL}/auth/realms/{REALM}/protocol/openid-connect/token",
            form={
                "grant_type": "client_credentials",
                "client_id": client_id,
                "client_secret": client_secret,
            },
        )
        if probe[0] == 200:
            print(f"client updated: {client_id} id={cid}")
            return cid
        print(f"client secret mismatch for {client_id} — recreating client")
        _http(
            "DELETE",
            f"{KEYCLOAK_URL}/auth/admin/realms/{REALM}/clients/{cid}",
            headers=auth_headers(token),
        )

    payload = {
        "clientId": client_id,
        "enabled": True,
        "protocol": "openid-connect",
        "publicClient": False,
        "clientAuthenticatorType": "client-secret",
        "secret": client_secret,
        "serviceAccountsEnabled": True,
        "directAccessGrantsEnabled": True,
        "standardFlowEnabled": False,
        "fullScopeAllowed": True,
        "defaultClientScopes": ["web-origins", "acr", "profile", "roles", "email"],
        "optionalClientScopes": ["address", "phone", "offline_access", "microprofile-jwt"],
        "attributes": {"access.token.lifespan": str(TOKEN_LIFESPAN_SECS)},
    }
    c, b = _http(
        "POST",
        f"{KEYCLOAK_URL}/auth/admin/realms/{REALM}/clients",
        data=json.dumps(payload).encode(),
        headers=auth_headers(token),
    )
    if c not in (201, 204):
        raise SystemExit(f"create client {client_id} failed: {c} {b}")
    code, clients = _http(
        "GET",
        f"{KEYCLOAK_URL}/auth/admin/realms/{REALM}/clients?{q}",
        headers=auth_headers(token),
    )
    if code != 200 or not clients:
        raise SystemExit(f"client {client_id} created but not found")
    print(f"client created: {client_id}")
    return clients[0]["id"]


def assign_service_account_roles(token: str, client_uuid: str, client_id: str) -> None:
    code, sa = _http(
        "GET",
        f"{KEYCLOAK_URL}/auth/admin/realms/{REALM}/clients/{client_uuid}/service-account-user",
        headers=auth_headers(token),
    )
    if code != 200 or not isinstance(sa, dict):
        raise SystemExit(f"service-account-user failed for {client_id}: {code} {sa}")
    user_id = sa["id"]

    role_reps: List[Dict[str, Any]] = []
    for name in REALM_ROLES:
        c, role = _http(
            "GET",
            f"{KEYCLOAK_URL}/auth/admin/realms/{REALM}/roles/{urllib.parse.quote(name)}",
            headers=auth_headers(token),
        )
        if c == 200 and isinstance(role, dict):
            role_reps.append(role)

    c, b = _http(
        "POST",
        f"{KEYCLOAK_URL}/auth/admin/realms/{REALM}/users/{user_id}/role-mappings/realm",
        data=json.dumps(role_reps).encode(),
        headers=auth_headers(token),
    )
    if c not in (204, 201, 409):
        print(f"warn: role-mappings {client_id}: {c} {b}")
    else:
        print(f"service-account roles assigned: {client_id} ({len(role_reps)})")

    # api-test Keycloak admin client needs realm-management roles to create users
    if client_id == "mosip-testrig-client":
        assign_realm_management_roles(token, user_id, client_id)


def assign_realm_management_roles(token: str, user_id: str, client_id: str) -> None:
    q = urllib.parse.urlencode({"clientId": "realm-management"})
    code, clients = _http(
        "GET",
        f"{KEYCLOAK_URL}/auth/admin/realms/{REALM}/clients?{q}",
        headers=auth_headers(token),
    )
    if code != 200 or not isinstance(clients, list) or not clients:
        print(f"warn: realm-management client not found for {client_id}")
        return
    rm_uuid = clients[0]["id"]
    code, available = _http(
        "GET",
        f"{KEYCLOAK_URL}/auth/admin/realms/{REALM}/users/{user_id}/role-mappings/clients/{rm_uuid}/available",
        headers=auth_headers(token),
    )
    if code != 200 or not isinstance(available, list):
        print(f"warn: available realm-management roles for {client_id}: {code} {available}")
        return
    wanted = {
        "realm-admin",
        "manage-users",
        "view-users",
        "query-users",
        "manage-realm",
        "view-realm",
        "manage-clients",
        "query-clients",
        "view-clients",
        "manage-roles",
        "view-roles",
        "query-roles",
    }
    to_assign = [r for r in available if r.get("name") in wanted]
    if not to_assign:
        # Fallback: assign all available client roles
        to_assign = available
    c, b = _http(
        "POST",
        f"{KEYCLOAK_URL}/auth/admin/realms/{REALM}/users/{user_id}/role-mappings/clients/{rm_uuid}",
        data=json.dumps(to_assign).encode(),
        headers=auth_headers(token),
    )
    if c not in (204, 201, 409):
        print(f"warn: realm-management role-mappings {client_id}: {c} {b}")
    else:
        print(f"realm-management roles assigned: {client_id} ({len(to_assign)})")


def mint_client_token(client_id: str = CLIENT_ID, client_secret: str = CLIENT_SECRET) -> Dict[str, Any]:
    code, body = _http(
        "POST",
        f"{KEYCLOAK_URL}/auth/realms/{REALM}/protocol/openid-connect/token",
        form={
            "grant_type": "client_credentials",
            "client_id": client_id,
            "client_secret": client_secret,
            "scope": "openid profile email",
        },
    )
    if code != 200 or not isinstance(body, dict) or "access_token" not in body:
        raise SystemExit(f"client_credentials failed for {client_id}: {code} {body}")
    return body


def write_wiremock_stubs(access_token: str, expires_in: int) -> None:
    # authmanager clientidsecretkey stub
    auth_body = {
        "request": {
            "method": "POST",
            "urlPathPattern": "/v1/authmanager/authenticate/clientidsecretkey",
        },
        "response": {
            "status": 200,
            "headers": {
                "Content-Type": "application/json",
                "access-control-expose-headers": "set-cookie",
                "authorization": access_token,
                "set-cookie": f"Authorization={access_token}; Path=/; HttpOnly",
            },
            "jsonBody": {
                "id": "string",
                "version": "v1",
                "responsetime": time.strftime("%Y-%m-%dT%H:%M:%S.000Z", time.gmtime()),
                "metadata": None,
                "response": {
                    "status": "Success",
                    "message": "Clientid and Token combination had been validated successfully",
                },
                "errors": None,
            },
        },
    }
    AUTH_CLIENT_TOKEN.parent.mkdir(parents=True, exist_ok=True)
    AUTH_CLIENT_TOKEN.write_text(json.dumps(auth_body, indent=2) + "\n", encoding="utf-8")
    print(f"wrote {AUTH_CLIENT_TOKEN}")

    oidc_body = {
        "request": {
            "method": "POST",
            "urlPathPattern": "/auth/realms/.*/protocol/openid-connect/token",
        },
        "response": {
            "status": 200,
            "headers": {"Content-Type": "application/json"},
            "jsonBody": {
                "access_token": access_token,
                "expires_in": expires_in,
                "refresh_expires_in": 0,
                "token_type": "Bearer",
                "not-before-policy": 0,
                "scope": "email profile",
            },
        },
    }
    OIDC_TOKEN.write_text(json.dumps(oidc_body, indent=2) + "\n", encoding="utf-8")
    print(f"wrote {OIDC_TOKEN}")


def decode_exp(token: str) -> None:
    try:
        payload_b64 = token.split(".")[1]
        pad = "=" * (-len(payload_b64) % 4)
        import base64

        payload = json.loads(base64.urlsafe_b64decode(payload_b64 + pad))
        exp = payload.get("exp")
        iat = payload.get("iat")
        years = (exp - iat) / (365.25 * 24 * 3600) if exp and iat else None
        print(
            f"token azp={payload.get('azp')} aud={payload.get('aud')} iss={payload.get('iss')} "
            f"exp={exp} lifespan≈{years:.1f}y roles={len((payload.get('realm_access') or {}).get('roles') or [])}"
        )
    except Exception as e:  # noqa: BLE001
        print(f"token decode skipped: {e}")


def main() -> int:
    print(f"KEYCLOAK_URL={KEYCLOAK_URL} REALM={REALM} clients={[c[0] for c in LOCAL_CLIENTS]}")
    wait_ready()
    token = admin_token()
    ensure_realm(token)
    ensure_roles(token)
    for client_id, client_secret in LOCAL_CLIENTS:
        client_uuid = ensure_client(token, client_id, client_secret)
        assign_service_account_roles(token, client_uuid, client_id)
        minted = mint_client_token(client_id, client_secret)
        decode_exp(minted["access_token"])
        if WRITE_WIREMOCK_STUBS and client_id == CLIENT_ID:
            write_wiremock_stubs(
                minted["access_token"],
                int(minted.get("expires_in") or TOKEN_LIFESPAN_SECS),
            )
            print(
                "wrote static WireMock stubs — recreate mock-service: "
                "docker compose up -d --force-recreate --no-deps mock-service"
            )
    if not WRITE_WIREMOCK_STUBS:
        print(
            "skipped WireMock stub write (live path: mock-service -> auth-token-bridge -> Keycloak)"
        )
    print("done")
    return 0


if __name__ == "__main__":
    sys.exit(main())
