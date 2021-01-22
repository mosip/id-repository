FROM openjdk:11

# can be passed during Docker build as build time environment for github branch to pickup configuration from.
ARG spring_config_label

# can be passed during Docker build as build time environment for spring profiles active 
ARG active_profile

# can be passed during Docker build as build time environment for config server URL 
ARG spring_config_url

# can be passed during Docker build as build time environment for glowroot 
ARG is_glowroot

# can be passed during Docker build as build time environment for artifactory URL
ARG artifactory_url

# can be passed during Docker build as build time environment for extraction format
ARG extraction_format=mock

#ARG bio_sdk_folder=mock/0.9
ARG biosdk_zip_path

ARG server_port=8100

# can be passed during Docker build as build time environment for github branch to pickup configuration from.
ARG container_user=mosip

# can be passed during Docker build as build time environment for github branch to pickup configuration from.
ARG container_user_group=mosip

# can be passed during Docker build as build time environment for github branch to pickup configuration from.
ARG container_user_uid=1001

# can be passed during Docker build as build time environment for github branch to pickup configuration from.
ARG container_user_gid=1001

# environment variable to pass active profile such as DEV, QA etc at docker runtime
ENV active_profile_env=${active_profile}

# environment variable to pass github branch to pickup configuration from, at docker runtime
ENV spring_config_label_env=${spring_config_label}

# environment variable to pass spring configuration url, at docker runtime
ENV spring_config_url_env=${spring_config_url}

# environment variable to pass glowroot, at docker runtime
ENV is_glowroot_env=${is_glowroot}

# environment variable to pass artifactory url, at docker runtime
ENV artifactory_url_env=${artifactory_url}
# environment variable to pass iam_adapter url, at docker runtime
ENV iam_adapter_url_env=${iam_adapter_url}

# environment variable to pass extration format, at docker runtime
ENV extraction_format_env=${extraction_format}

ENV server_port_env=${server_port}

ARG biosdk_local_dir=biosdk-client

ENV biosdk_local_dir_name=${biosdk_local_dir}

#ENV bio_sdk_folder_env=${bio_sdk_folder}
ENV biosdk_zip_file_path=${biosdk_zip_path}

#ENV work_dir_env=/home/${container_user}

ENV current_module_env=bioextractor-service

# install packages and create user
RUN apt-get -y update \
&& apt-get install -y unzip \
&& groupadd -g ${container_user_gid} ${container_user_group} \
&& useradd -u ${container_user_uid} -g ${container_user_group} -s /bin/sh -m ${container_user} \
&& adduser ${container_user} sudo \
&& echo "%sudo ALL=(ALL) NOPASSWD:/home/${container_user}/${biosdk_local_dir}/install.sh" >> /etc/sudoers

# set working directory for the user
WORKDIR /home/${container_user}

ENV work_dir=/home/${container_user}

COPY ./target/${current_module_env}-*.jar ${current_module_env}.jar

ADD configure_start.sh configure_start.sh

RUN chmod +x configure_start.sh

EXPOSE ${server_port_env}

# change permissions of file inside working dir
RUN chown -R ${container_user}:${container_user} /home/${container_user}

# select container user for all tasks
USER ${container_user_uid}:${container_user_gid}

ENTRYPOINT [ "./configure_start.sh" ]

CMD if [ "$is_glowroot_env" = "present" ]; then \
    wget -q --show-progress "${artifactory_url_env}"/artifactory/libs-snapshot-local/io/mosip/testing/glowroot.zip ; \
    wget -q --show-progress "${iam_adapter_url_env}" -O kernel-auth-adapter.jar; \
    unzip glowroot.zip ; \
    rm -rf glowroot.zip ; \
    sed -i "s/<service_name>/${current_module_env}/g" glowroot/glowroot.properties ; \
    java -jar -javaagent:glowroot/glowroot.jar -Dspring.cloud.config.label="${spring_config_label_env}" -Dspring.profiles.active="${active_profile_env}" -Dspring.cloud.config.uri="${spring_config_url_env}" -DextractionFormat="${extraction_format_env}" -Dloader.path="${loader_path_env}",./kernel-auth-adapter.jar ${current_module_env}.jar ; \
    else \
    wget "${iam_adapter_url_env}" -O kernel-auth-adapter.jar; \
    java -jar -Dspring.cloud.config.label="${spring_config_label_env}" -Dspring.profiles.active="${active_profile_env}" -Dspring.cloud.config.uri="${spring_config_url_env}" -DextractionFormat="${extraction_format_env}" -Dloader.path="${loader_path_env}",./kernel-auth-adapter.jar ${current_module_env}.jar ; \
    fi
