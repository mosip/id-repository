# ID-Repository Service

Helm chart for the merged MOSIP ID-Repository deployable (`id-repository-service`): identity, credential store, and credential-request APIs in one pod.

## TL;DR

```console
$ helm repo add mosip https://mosip.github.io
$ helm -n idrepo install my-release mosip/identity
```
## Prerequisites

- Kubernetes 1.12+
- Helm 3.1.0
- PV provisioner support in the underlying infrastructure
- ReadWriteMany volumes for deployment scaling

