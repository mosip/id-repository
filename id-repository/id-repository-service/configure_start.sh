#!/bin/bash

#installs the pre-requisites.
set -e

echo "Downloading pre-requisites install scripts"
wget --no-check-certificate --no-cache --no-cookies $artifactory_url_env/artifactory/libs-release-local/deployment/docker/id-authentication/configure_biosdk.sh -O configure_biosdk.sh

echo "Installating pre-requisites.."
chmod +x configure_biosdk.sh
./configure_biosdk.sh

echo "Installating pre-requisites completed."

echo "Downloading kernel ref-idobject validator (auth adapter is a Maven dependency)"
wget -q --show-progress "${kernel_ref_idobjectvalidator_url}" -O "${loader_path_env}"/kernel-ref-idobjectvalidator.jar
echo "Downloaded kernel ref-idobject validator"

exec "$@"
