# IDRepo Credential Request Service

Helm chart for installing IDrepo Credential Request Service

## TL;DR

```console
$ helm repo add mosip https://mosip.github.io
$ helm -n idrepo install my-release mosip/credentialrequest
```
## Prerequisites

- Kubernetes 1.12+
- Helm 3.1.0
- PV provisioner support in the underlying infrastructure
- ReadWriteMany volumes for deployment scaling

