FROM mosipid/openjdk-21-jre:21.0.4

ARG SOURCE
ARG COMMIT_HASH
ARG COMMIT_ID
ARG BUILD_TIME
LABEL source=${SOURCE}
LABEL commit_hash=${COMMIT_HASH}
LABEL commit_id=${COMMIT_ID}
LABEL build_time=${BUILD_TIME}

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

# environment variable to pass active profile such as DEV, QA etc at docker runtime
ENV active_profile_env=${active_profile}

# environment variable to pass github branch to pickup configuration from, at docker runtime
ENV spring_config_label_env=${spring_config_label}

# environment variable to pass spring configuration url, at docker runtime
ENV spring_config_url_env=${spring_config_url}

# environment variable to pass glowroot, at docker runtime
ENV is_glowroot_env=${is_glowroot}

# can be passed during Docker build as build time environment for github branch to pickup configuration from.
ARG container_user=mosip

# can be passed during Docker build as build time environment for github branch to pickup configuration from.
ARG container_user_group=mosip

# can be passed during Docker build as build time environment for github branch to pickup configuration from.
ARG container_user_uid=1002

# can be passed during Docker build as build time environment for github branch to pickup configuration from.
ARG container_user_gid=1001

# set working directory for the user
WORKDIR /home/${container_user}

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

# install packages and create user
RUN apt-get -y update \
&& apt-get install -y unzip sudo curl \
&& groupadd -g ${container_user_gid} ${container_user_group} \
&& useradd -u ${container_user_uid} -g ${container_user_group} -s /bin/bash -m ${container_user} \
&& adduser ${container_user} sudo \
&& echo "%sudo ALL=(ALL) NOPASSWD:/home/${container_user}/${biosdk_local_dir}/install.sh" >> /etc/sudoers \
&& mkdir -p ${loader_path} \
&& chmod +x configure_start.sh \
&& chown -R ${container_user}:${container_user} ${work_dir}

# select container user for all tasks
USER ${container_user_uid}:${container_user_gid}

EXPOSE 8090

ENTRYPOINT [ "./configure_start.sh" ]

CMD wget -q --show-progress "${kernel_ref_idobjectvalidator_url}" -O "${loader_path_env}"/kernel-ref-idobjectvalidator.jar; \
    wget "${iam_adapter_url_env}" -O "${loader_path_env}"/kernel-auth-adapter.jar; \
    echo "Downloaded kernel ref-idobject validator"; \
    java -XX:-UseG1GC -XX:+ExplicitGCInvokesConcurrent -XX:-UseParallelGC -XX:+UseZGC -XX:+ZGenerational -XX:-UseShenandoahGC -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+UseStringDeduplication -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8 -XX:+UseCompressedOops \
    -Dloader.path="${loader_path_env}" -Dspring.cloud.config.label="${spring_config_label_env}" -Dspring.profiles.active="${active_profile_env}"  -Dspring.cloud.config.uri="${spring_config_url_env}" --add-opens java.base/sun.reflect.annotation=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED --add-modules=ALL-SYSTEM --add-opens java.xml/jdk.xml.internal=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.lang.stream=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.time=ALL-UNNAMED --add-opens java.base/java.time.LocalDate=ALL-UNNAMED --add-opens java.base/java.time.LocalDateTime=ALL-UNNAMED --add-opens java.base/java.time.LocalDateTime.date=ALL-UNNAMED --add-opens java.base/jdk.internal.reflect.DirectMethodHandleAccessor=ALL-UNNAMED -jar ${current_module_env}.jar ; \
