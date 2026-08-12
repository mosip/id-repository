# Local Keycloak (IAM) for laptop stack

Issues ~10-year `mosip-idrepo-client` tokens. WireMock does **not** embed a hardcoded JWT.

## Live token path

```
POST /v1/authmanager/authenticate/clientidsecretkey
  → mock-service (proxy)
  → auth-token-bridge
  → Keycloak client_credentials
```

OIDC (`/auth/realms/.../token|userinfo|certs`) is also proxied from WireMock to Keycloak when callers hit `mock-service:8082`.

## Quick use

```powershell
cd id-repository\local-dev-setup\docker-compose
.\mint-local-iam-token.bat
```

Or full stack: `docker compose up -d` runs `keycloak` → `keycloak-init` → `auth-token-bridge` → `mock-service`.

## Admin UI

- URL: http://localhost:8081/auth
- User / password: `admin` / `admin`
- Realm: `mosip`
- Client: `mosip-idrepo-client` / secret `QTGizTYN4US0XHOU`
- Access token lifespan: `315360000` seconds (~10 years)
