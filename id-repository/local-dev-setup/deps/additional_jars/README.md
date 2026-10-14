# Additional jars (`-Dloader.path`)

Only jars that are **not** Maven dependencies belong here. The host JVM
(`run-idrepo-local.*`) passes `kernel-ref-idobjectvalidator.jar` to
`-Dloader.path`. Docker (`configure_start.sh`) wget’s the same artifact into
the image loader path.

| Jar | Required locally |
|-----|------------------|
| `kernel-ref-idobjectvalidator.jar` | **Yes** |

`*.jar` in this folder is gitignored. Cluster images wget the validator from
`kernel_ref_idobjectvalidator_url` (artifactory-share ConfigMap) — do not pin it
in `id-repository/pom.xml`.

Place `kernel-ref-idobjectvalidator.jar` here, or from `local-dev-setup/`:

```bash
bash run-local-stack.sh prep
```
