#!/bin/bash

#installs the pre-requisites.
set -e

echo "Downloading pre-requisites install scripts"

wget --no-check-certificate --no-cache --no-cookies $artifactory_url_env/artifactory/libs-release-local/deployment/docker/id-authentication/configure_biosdk.sh -O configure_biosdk.sh
wget -q --show-progress "${kernel_ref_idobjectvalidator_url}" -O "${loader_path_env}"/kernel-ref-idobjectvalidator.jar; \

echo "Downloaded kernel ref-idobject validator"; \

wget -q --show-progress "${iam_adapter_url_env}" -O "${loader_path_env}"/kernel-auth-adapter.jar; \

echo "Downloaded kernel auth adapter"; \

echo "Installating pre-requisites.."
chmod +x configure_biosdk.sh
./configure_biosdk.sh

echo "Installating pre-requisites completed."

exec "$@"
