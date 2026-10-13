# Postman — ID-Repository local APIs

Import into Postman:

1. [`Id-Repository-Local.postman_collection.json`](Id-Repository-Local.postman_collection.json)
2. [`Id-Repository-Local.postman_environment.json`](Id-Repository-Local.postman_environment.json)

Select environment **Id-Repository-Local**.

## Prerequisites

```bash
cd id-repository/local-dev-setup
./run-local-stack.sh up
./run-idrepo-local.sh
```

## Suggested order

1. **00 - Auth** → `Client token (idrepo)` — copy response `Authorization` header or `response.token` into env `accessToken`
2. **01 - Dependencies** → `Generate UIN` — set env `uin`
3. **02 - Identity** → `Add Identity` (uses `{{uin}}`)
4. Draft / VID / Credential folders as needed

## Variables

| Variable | Default | Role |
|----------|---------|------|
| `idrepoBaseUrl` | `http://localhost:8090` | Host id-repository-service |
| `wiremockBaseUrl` | `http://localhost:8082` | IAM, idgenerator, idschema |
| `accessToken` | _(empty)_ | JWT from auth stubs |
| `uin` / `vid` / `registrationId` / `requestId` | placeholders | Path/body params |

## Sources

- TestNG report `api-test/testng-report/mosip-api-internal.local-idrepo-2026-08-20_19-59-full-report_*.html` (sample bodies)
- JMeter `IDRepo_Test_Script_v04_jannaLocal.jmx` (endpoint inventory)
- `api-docs/` OpenAPI (extra identity/credential paths)

Regenerate (optional): from repo, run the builder script if present under `api-test/`, or recreate from those sources.
