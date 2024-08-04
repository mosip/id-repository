# IDRepo Modular chart

Helm chart for installing IDrepo services:
* idrepo-saltgen
* identity
* credential
* credential-request generator
* vid

## TL;DR

```console
$ helm repo add mosip https://mosip.github.io
$ helm -n idrepo install my-release mosip/idrepo
```
## Prerequisites

- Kubernetes 1.12+
- Helm 3.1.0
- PV provisioner support in the underlying infrastructure
- ReadWriteMany volumes for deployment scaling
