# IDRepo Salt Generator

Helm chart to run IDRepo salt generator.

## TL;DR

```console
$ helm repo add mosip https://mosip.github.io
$ helm install my-release mosip/idrepo-saltgen
```

## Prerequisites

- Kubernetes 1.12+
- Helm 3.1.0
- PV provisioner support in the underlying infrastructure
- ReadWriteMany volumes for deployment scaling

