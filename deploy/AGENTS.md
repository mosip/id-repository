# AGENTS.md — `deploy/`

> Cluster-side shell installers that wrap Helm installs, ConfigMap copies, and lifecycle for ID-Repository.  
> Parent guide: [repo root `AGENTS.md`](../AGENTS.md).  
> Charts: [`helm/AGENTS.md`](../helm/AGENTS.md). Java app: [`id-repository/AGENTS.md`](../id-repository/AGENTS.md).

---

## 1. Purpose

Use these scripts on a Kubernetes cluster (typically via Rancher / ops host) to install, restart, or tear down id-repository components. They copy shared ConfigMaps and invoke published Helm charts from the MOSIP Helm repo.

---

## 2. Layout

```
deploy/
├── copy_cm_func.sh           # shared helper: copy ConfigMap across namespaces
├── idrepo/                   # primary ID-Repository install
│   ├── install.sh
│   ├── delete.sh
│   ├── restart.sh
│   └── README.md
├── idrepo-apitestrig/        # functional API test rig on cluster
│   ├── install.sh
│   ├── delete.sh
│   ├── values.yaml
│   └── README.md
└── credential-feeder/        # legacy credential feeder helpers
    ├── install.sh
    ├── delete.sh
    └── README.md
```

---

## 3. `deploy/idrepo` (primary)

| Script | Role |
|--------|------|
| `install.sh [kubeconfig]` | Create `idrepo` NS, Istio label, copy ConfigMaps, Helm install saltgen + services |
| `delete.sh` | Tear down installs |
| `restart.sh` | Rolling restart of deployments |

Typical flow inside `install.sh`:

1. `kubectl create ns idrepo` + Istio injection label  
2. Copy ConfigMaps via `copy_cm_func.sh` (`global`, `artifactory-share`, `config-server-share`)  
3. `helm install idrepo-saltgen` (Job — wait for completion)  
4. Helm install identity / related charts (chart set evolves with consolidation)  
5. Wait for rollout  

Namespace default: `idrepo`. Chart version is pinned in the script (`CHART_VERSION`).

Coordinate chart names/versions with [`helm/`](../helm/AGENTS.md) and the published `mosip` Helm repo (`https://mosip.github.io`).

```bash
cd deploy/idrepo
./install.sh                 # uses current kube context
./install.sh /path/to/kubeconfig
./restart.sh
./delete.sh
```

---

## 4. `deploy/idrepo-apitestrig`

Installs the API test rig against a deployed cluster. Review `values.yaml` for enabled modules before install. See folder README for self-signed SSL / init-container prompts and manual CronJob → Job runs.

```bash
cd deploy/idrepo-apitestrig
./install.sh
./delete.sh
```

Functional test sources live under repo `api-test/` (see root AGENTS).

---

## 5. `deploy/credential-feeder`

Legacy helper for credential feeder workloads. Prefer consolidated `id-repository-service` paths for new work unless an environment still requires the feeder.

---

## 6. Agent rules

### Do

1. Prefer `deploy/idrepo/install.sh` as the primary cluster entry for id-repo.
2. Keep `CHART_VERSION` and chart names aligned with published Helm charts / [`helm/`](../helm/AGENTS.md).
3. Run saltgen **before** (or as part of install before) relying on HTTP crypto that needs salts.
4. Copy required ConfigMaps (`global`, artifactory, config-server) into the target namespace.
5. After install, smoke-test identity retrieve + credential issuance when salt or schema changed.

### Do not

1. Deploy salt-generator as a long-lived scaled Deployment via these scripts.
2. Change REST paths or WebSub topics in app code without updating `api-test` and docs.
3. Hardcode secrets into install scripts — use cluster secrets / env / sealed secrets patterns already used by the environment.
4. Drift local `helm/` chart values from what `install.sh` deploys without documenting the intentional difference.

---

*Last updated: 2026-07-28.*
