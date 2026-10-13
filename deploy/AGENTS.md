# deploy/

Cluster shell wrappers around Helm + ConfigMap copy. Charts: `helm/`.

| Path | Role |
|------|------|
| `idrepo/install.sh [kubeconfig]` | NS `idrepo`, Istio label, copy CMs, saltgen Job, then identity |
| `idrepo/delete.sh` / `restart.sh` | Teardown / rolling restart |
| `idrepo-apitestrig/` | Cluster API-test rig (`values.yaml`) |
| `credential-feeder/` | Legacy — prefer `id-repository-service` |
| `copy_cm_func.sh` | Copy `global`, `artifactory-share`, `config-server-share` |

`CHART_VERSION` is pinned in the script — keep it aligned with published `mosip` charts (`https://mosip.github.io`).

## Rules

- Prefer `deploy/idrepo/install.sh`. Saltgen must complete before salted HTTP crypto.
- No secrets in scripts. Do not deploy salt-generator as a Deployment.
- Do not drift local `helm/` values from what `install.sh` deploys without documenting it.
