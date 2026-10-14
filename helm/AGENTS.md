# helm

```text
helm/
├─ idrepo/          umbrella (saltgen.enabled, service.enabled)
├─ identity/        id-repository-service :8090, health /idrepository/v1/identity/actuator/health
└─ idrepo-saltgen/  one-shot salt Job
Order: DB deploy → saltgen (--wait --wait-for-jobs) → identity
```

## Rules

- Salt is a Job chart, never a scaled Deployment.
- Values follow the consolidated `id-repository-service`. No secrets in `values.yaml`.
- Port or health-path change → update probes and smoke tests. Publishing → bump `Chart.yaml` + README.
