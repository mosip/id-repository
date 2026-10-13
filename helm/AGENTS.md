# helm/

| Chart | Deploys |
|-------|---------|
| `helm/idrepo` | Umbrella (`saltgen.enabled`, `service.enabled`) |
| `helm/identity` | HTTP service (`id-repository-service`, port 8090, health `/idrepository/v1/identity/actuator/health`) |
| `helm/idrepo-saltgen` | One-shot salt **Job** |

Order after schema/salt DDL: DB deploy → saltgen Job (`--wait --wait-for-jobs`) → HTTP. Cluster scripts: `deploy/`.

## Rules

- Same HTTP image family; salt is a separate Job chart — never a scaled Deployment.
- Keep values aligned with consolidated `id-repository-service`. No secrets in `values.yaml`.
- Port/health path changes need probe + smoke updates. Bump `Chart.yaml` / README when publishing.
