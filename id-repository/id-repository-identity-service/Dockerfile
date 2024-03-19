FROM openjdk:11

ARG SOURCE
ARG COMMIT_HASH
ARG COMMIT_ID
ARG BUILD_TIME
LABEL source=${SOURCE}
LABEL commit_hash=${COMMIT_HASH}
LABEL commit_id=${COMMIT_ID}
LABEL build_time=${BUILD_TIME}

# can be passed during Docker build as build time environment for github branch to pickup configuration from | spring profiles active | config server URL | glowroot and artifactory URL respectively .
ARG spring_config_label 
ARG active_profile
ARG spring_config_url 
ARG is_glowroot
ARG artifactory_url

# environment variable to pass active profile such as DEV, QA etc at docker runtime | github branch to pickup configuration from, at docker runtime | spring configuration url, at docker runtime | glowroot, at docker runtime | artifactory url, at docker runtime and iam_adapter url, at docker runtime.
ENV active_profile_env=${active_profile}
ENV spring_config_label_env=${spring_config_label}
ENV spring_config_url_env=${spring_config_url}
ENV is_glowroot_env=${is_glowroot}
ENV artifactory_url_env=${artifactory_url}
ENV iam_adapter_url_env=${iam_adapter_url}

# can be passed during Docker build as build time environment for github branch to pickup configuration from.
ARG container_user=mosip
ARG container_user_group=mosip
ARG container_user_uid=1001
ARG container_user_gid=1001

#ARG bio_sdk_folder=mock/0.9
ARG biosdk_zip_path

ARG biosdk_local_dir=biosdk-client

ENV biosdk_local_dir_name=${biosdk_local_dir}

#ENV bio_sdk_folder_env=${bio_sdk_folder}
ENV biosdk_zip_file_path=${biosdk_zip_path}

# set working directory for the user
WORKDIR /home/${container_user}

ENV work_dir=/home/${container_user}

ARG loader_path=${work_dir}/additional_jars/

ENV loader_path_env=${loader_path}

ENV current_module_env=id-repository-identity-service

ADD target/${current_module_env}-*.jar ${current_module_env}.jar

ADD configure_start.sh configure_start.sh

#ADD http://13.71.87.138:8040/artifactory/libs-snapshot-local/io/mosip/kernel/kernel-ref-idobjectvalidator/1.0.9-SNAPSHOT/kernel-ref-idobjectvalidator-1.0.9-SNAPSHOT.jar kernel-ref-idobjectvalidator-1.0.9-SNAPSHOT.jar

## install packages, create user and change permissions of file inside working dir
RUN apt-get -y update \
&& apt-get install -y unzip sudo \
&& groupadd -g ${container_user_gid} ${container_user_group} \
&& useradd -u ${container_user_uid} -g ${container_user_group} -s /bin/sh -m ${container_user} \
&& adduser ${container_user} sudo \
&& echo "%sudo ALL=(ALL) NOPASSWD:/home/${container_user}/${biosdk_local_dir}/install.sh" >> /etc/sudoers \
&& mkdir -p ${loader_path} \
&& chmod +x configure_start.sh \
&& chown -R ${container_user}:${container_user} /home/${container_user}

# select container user for all tasks
USER ${container_user_uid}:${container_user_gid}

EXPOSE 8090

ENTRYPOINT [ "./configure_start.sh" ]

CMD if [ "$is_glowroot_env" = "present" ]; then \
    wget -q --show-progress "${artifactory_url_env}"/artifactory/libs-release-local/io/mosip/kernel/kernel-ref-idobjectvalidator/kernel-ref-idobjectvalidator.jar -O "${loader_path_env}"/kernel-ref-idobjectvalidator.jar ; \
    wget -q --show-progress "${artifactory_url_env}"/artifactory/libs-release-local/io/mosip/testing/glowroot.zip ; \
    unzip glowroot.zip ; \
    rm -rf glowroot.zip ; \
    sed -i 's/<service_name>/id-repository-identity-service/g' glowroot/glowroot.properties ; \
    wget -q --show-progress "${iam_adapter_url_env}" -O "${loader_path_env}"/kernel-auth-adapter.jar; \
    java -Dloader.path="${loader_path_env}" -jar -javaagent:glowroot/glowroot.jar -Dspring.cloud.config.label="${spring_config_label_env}" -Dspring.profiles.active="${active_profile_env}"  -Dspring.cloud.config.uri="${spring_config_url_env}" ${current_module_env}.jar ; \
    else \
    wget -q --show-progress "${artifactory_url_env}"/artifactory/libs-release-local/io/mosip/kernel/kernel-ref-idobjectvalidator/kernel-ref-idobjectvalidator.jar -O "${loader_path_env}"/kernel-ref-idobjectvalidator.jar ; \
    wget -q --show-progress "${iam_adapter_url_env}" -O "${loader_path_env}"/kernel-auth-adapter.jar; \
    java -Dloader.path="${loader_path_env}" -jar -Dspring.cloud.config.label="${spring_config_label_env}" -Dspring.profiles.active="${active_profile_env}"  -Dspring.cloud.config.uri="${spring_config_url_env}" ${current_module_env}.jar ; \
    fi
