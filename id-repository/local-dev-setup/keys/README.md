# Local PKCS12 keystores (ID-Repository)

Key Manager in this docker-compose uses a **PKCS12** file under `keys/` instead of SoftHSM.

`.p12` files are **gitignored**. Create them on your machine before the first `docker compose up`.

## Required for keymanager-service

| File | Password | Purpose |
|------|----------|---------|
| `mosip-idrepo-ks.p12` | `qwerty@1234` | HSM substitute for inventory / encrypt / decrypt / sign |

### Create an empty keystore (recommended for fresh clones)

Compose mounts this directory at `/home/mosip/config` inside `keymanager-service`. After the store exists, `keymanager-init` generates ROOT + ID_REPO master keys into it.

**Windows (PowerShell)** — from git repo root:

```powershell
keytool -genkeypair -alias bootstrap -keyalg RSA -keysize 2048 -storetype PKCS12 `
  -keystore id-repository\local-dev-setup\keys\mosip-idrepo-ks.p12 `
  -storepass "qwerty@1234" -keypass "qwerty@1234" `
  -dname "CN=mosip-idrepo-local" -validity 3650
```

**macOS / Linux:**

```bash
keytool -genkeypair -alias bootstrap -keyalg RSA -keysize 2048 -storetype PKCS12 \
  -keystore id-repository/local-dev-setup/keys/mosip-idrepo-ks.p12 \
  -storepass 'qwerty@1234' -keypass 'qwerty@1234' \
  -dname 'CN=mosip-idrepo-local' -validity 3650
```

Password must match compose: `qwerty@1234`.

## Optional (partner testing)

| File | Password | Purpose |
|------|----------|---------|
| `partner-chain.p12` | — | Partner certificate chain |
| `partner-mosip-signed.p12` | `1234` | MOSIP-signed partner keystore |

See the human walkthrough: [`../LOCAL-DEV-SETUP.md`](../LOCAL-DEV-SETUP.md).
