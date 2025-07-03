# ID Repository
## Deployment in K8 cluster with other MOSIP services:
### Pre-requisites
* Set KUBECONFIG variable to point to existing K8 cluster kubeconfig file:
    ```
    export KUBECONFIG=~/.kube/<k8s-cluster.config>
    ```
### Install Id-repository
 ```
    $ ./install.sh
   ```
### Delete
  ```
    $ ./delete.sh
   ```
### Restart
  ```
    $ ./restart.sh
   ```
### Install Keycloak client
  ```
    cd deploy/keycloak
    $ ./keycloak_init.sh
   ```

### Install Apitestrig
```
    cd deploy/apitest-idrepo
    $ ./install.sh
```