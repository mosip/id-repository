# Local PKCS12 keystores (ID-Repository)

Key Manager uses **PKCS12** under `keys/` (not SoftHSM). `.p12` files are gitignored.

## Required for keymanager-service

| File | Password | Purpose |
|------|----------|---------|
| `mosip-idrepo-ks.p12` | `1234` | HSM substitute (copy of `idrepo-ks.p12`) |

### Static SQL seed (default)

Paired with static keymgr DML inlined in [`../deps/init.sql`](../deps/init.sql).

**Keep this PKCS12 across `wipe`.** Regenerating it breaks decrypt of seeded `key_store` rows.

Config: `mosip.kernel.keymanager.hsm.keystore-pass=1234` in `deps/mosip-config/kernel-default.properties`.

### Fresh empty keystore (only if `KM_STATIC_SEED=false`)

```powershell
keytool -genkeypair -alias bootstrap -keyalg RSA -keysize 2048 -storetype PKCS12 `
  -keystore id-repository\local-dev-setup\keys\mosip-idrepo-ks.p12 `
  -storepass "1234" -keypass "1234" `
  -dname "CN=mosip-idrepo-local" -validity 3650
```

```bash
keytool -genkeypair -alias bootstrap -keyalg RSA -keysize 2048 -storetype PKCS12 \
  -keystore id-repository/local-dev-setup/keys/mosip-idrepo-ks.p12 \
  -storepass '1234' -keypass '1234' \
  -dname 'CN=mosip-idrepo-local' -validity 3650
```

See [`../deps/keymanager/README.md`](../deps/keymanager/README.md).
