# deploy

Cluster wrappers around Helm (`helm/`) + ConfigMap copy.

```text
deploy/
├─ idrepo/install.sh [kubeconfig]  NS idrepo, Istio label, copy CMs, saltgen Job, then identity (preferred)
├─ idrepo/delete.sh · restart.sh   teardown · rolling restart
├─ idrepo-apitestrig/              cluster API-test rig (values.yaml)
├─ credential-feeder/              legacy; use id-repository-service
└─ copy_cm_func.sh                 copies global, artifactory-share, config-server-share
```

## Rules

- `CHART_VERSION` is pinned in scripts; keep it aligned with published `mosip` charts.
- Saltgen must finish before the HTTP service uses salted crypto.
- No secrets in scripts. Document any drift between `helm/` values and what `install.sh` deploys.
