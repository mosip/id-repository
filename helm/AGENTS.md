# helm

```text
helm/
├─ identity/        id-repository-service :8090, health /idrepository/v1/identity/actuator/health; Service identity.idrepo serves all four prefixes
└─ idrepo-saltgen/  one-shot salt Job
Order: DB deploy → saltgen (--wait --wait-for-jobs) → identity
```

## Rules

- Salt is a Job chart, never a scaled Deployment.
- Values follow the consolidated `id-repository-service`. No secrets in `values.yaml`.
- No `vid` / `credential` / `credentialrequest` charts; callers reach them via `identity.idrepo` (`mosip.idrepo.*.url`).
- Port or health-path change → update probes and smoke tests. Publishing → bump `Chart.yaml` + README.
