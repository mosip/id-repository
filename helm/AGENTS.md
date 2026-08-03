# AGENTS.md — `helm/`

> Kubernetes Helm charts for MOSIP ID-Repository (HTTP service + salt Job).  
> Parent guide: [repo root `AGENTS.md`](../AGENTS.md).  
> Cluster installers: [`deploy/AGENTS.md`](../deploy/AGENTS.md). Salt Java: [`id-repository-salt-generator/AGENTS.md`](../id-repository/id-repository-salt-generator/AGENTS.md).

---

## 1. Charts

| Chart | Path | Deploys |
|-------|------|---------|
| **identity** | `helm/identity/` | Consolidated HTTP service (`id-repository-service`) |
| **idrepo-saltgen** | `helm/idrepo-saltgen/` | One-shot salt **Job** |

```
helm/
├── identity/            # Deployment, Service, probes, Istio VS, ServiceMonitor
│   ├── Chart.yaml
│   ├── values.yaml
│   ├── templates/
│   └── README.md
└── idrepo-saltgen/      # Job + SA templates
    ├── Chart.yaml
    ├── values.yaml
    ├── templates/job.yaml
    └── README.md
```

---

## 2. Consolidated deployment model

Same Docker image family for the HTTP deployable (`mosipqa/id-repository-service` in chart values). Salt is a **separate** image/Job chart.

| Workload | Chart | Notes |
|----------|-------|-------|
| HTTP API | `helm/identity` | Port `8090`; health on `/idrepository/v1/identity/actuator/health` |
| Salt population | `helm/idrepo-saltgen` | K8s **Job** only — run after DB deploy |

```console
helm repo add mosip https://mosip.github.io
helm -n idrepo install idrepo-saltgen mosip/idrepo-saltgen --wait --wait-for-jobs
helm -n idrepo install identity mosip/identity
```

Prerequisites: Kubernetes 1.12+, Helm 3.1.0+, PV provisioner as required by values.

---

## 3. Key values (`identity`)

| Area | Location / keys |
|------|-----------------|
| Image | `image.repository`, `image.tag` (default develop tags in chart) |
| Replicas | `replicaCount` (+ HPA if configured in env overlays) |
| Service port | `springServicePort: 8090` |
| Probes | `startupProbe` / liveness / readiness → actuator health |
| Istio | `istio` / virtualservice templates |
| Extra objects | `extraDeploy` |

Prefer environment overlays / deploy scripts for cluster-specific ConfigMap and secret wiring rather than committing secrets into `values.yaml`.

---

## 4. Salt generator Job (`idrepo-saltgen`)

- Template: `templates/job.yaml` — **Job**, not Deployment.
- Run after `db_scripts` (or salt DDL changes) so `uin_hash_salt` / `uin_encrypt_salt` in **idrepo** and **idmap** are populated.
- Ops path: often invoked from [`deploy/idrepo/install.sh`](../deploy/AGENTS.md) with `--wait --wait-for-jobs`.

---

## 5. Agent rules

### Do

1. Keep Helm values aligned with the consolidated single HTTP deployable (`id-repository-service`).
2. Deploy salt as a Job; wait for completion before relying on salted crypto paths.
3. After schema/salt DDL changes: DB deploy → saltgen Job → HTTP service.
4. Update chart `README.md` / `Chart.yaml` version metadata when publishing chart changes.
5. Point Java/runtime questions to [`id-repository/AGENTS.md`](../id-repository/AGENTS.md); DB to [`db_scripts/AGENTS.md`](../db_scripts/AGENTS.md).

### Do not

1. Deploy salt-generator as a scaled long-lived Deployment.
2. Merge idrepo/idmap concerns into one DB assumption in chart docs.
3. Commit environment secrets into chart values.
4. Change service ports or health paths without updating probes and deploy smoke checks.

---

## 6. Related install path

Cluster operators usually run [`deploy/idrepo/install.sh`](../deploy/idrepo/install.sh), which copies ConfigMaps and installs charts from the MOSIP Helm repo. Local `helm/` trees are the source for chart development; keep them consistent with what deploy scripts install.

---

*Last updated: 2026-07-28.*
