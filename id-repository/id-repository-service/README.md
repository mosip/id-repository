# id-repository-service

HTTP deployable (**8090**). Identity, VID, credential, and credreq **domain live here**. `id-repository-core` is the IDA-facing `core.*` library only.

Local: [`../local-dev-setup/README.md`](../local-dev-setup/README.md). Agent rules: [`AGENTS.md`](AGENTS.md).

**Build:** `cd id-repository && mvn -pl id-repository-service -am package -DskipTests -Dgpg.skip=true`

## URLs (contracts)

- `/idrepository/v1/identity/*`
- `/idrepository/v1/vid/*`
- `/v1/credentialservice/*`
- `/v1/credentialrequest/*`

Salt is a **separate** Job (`id-repository-salt-generator` / `helm/idrepo-saltgen`). Local salts are in `local-dev-setup/deps/init.sql`.
