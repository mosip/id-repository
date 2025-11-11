FROM eclipse-temurin:11-jre

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

# can be passed during Docker build as build time environment for config server URL 
ARG spring_config_name

# can be passed during Docker build as build time environment for spring server port
ARG server_port

# can be passed during Docker build as build time environment for schema name for Salt Generator 
ARG salt_gen_db_alias

# can be passed during Docker build as build time environment for schema name for Salt Generator 
ARG salt_gen_schema_name

# can be passed during Docker build as build time environment for table name for Salt Generator
ARG salt_gen_table_name

# environment variable to pass active profile such as DEV, QA etc at docker runtime
ENV active_profile_env=${active_profile}

# environment variable to pass github branch to pickup configuration from, at docker runtime
ENV spring_config_label_env=${spring_config_label}

# environment variable to pass github branch to pickup configuration from, at docker runtime
ENV spring_config_name_env=${spring_config_name}

# environment variable to pass spring configuration url, at docker runtime
ENV spring_config_url_env=${spring_config_url}

# environment variable to pass spring server port, at docker runtime
ENV spring_server_port=${server_port}

# environment variable to pass schema name to salt generator, at docker runtime
ENV db_alias=${salt_gen_db_alias}

# environment variable to pass schema name to salt generator, at docker runtime
ENV schema_name=${salt_gen_schema_name}

# environment variable to pass table name to salt generator, at docker runtime
ENV table_name=${salt_gen_table_name}

# can be passed during Docker build as build time environment for github branch to pickup configuration from.
ARG container_user=mosip

# can be passed during Docker build as build time environment for github branch to pickup configuration from.
ARG container_user_group=mosip

# can be passed during Docker build as build time environment for github branch to pickup configuration from.
ARG container_user_uid=1001

# can be passed during Docker build as build time environment for github branch to pickup configuration from.
ARG container_user_gid=1001

# install packages and create user
RUN apt-get -y update \
&& apt-get install -y unzip \
&& groupadd -g ${container_user_gid} ${container_user_group} \
&& useradd -u ${container_user_uid} -g ${container_user_group} -s /bin/sh -m ${container_user}

# set working directory for the user
WORKDIR /home/${container_user}

ENV work_dir=/home/${container_user}

ARG loader_path=${work_dir}/additional_jars/

RUN mkdir -p ${loader_path}

ENV loader_path_env=${loader_path}

ARG olv_partner_ids=mpartner-default-auth

#Comma seperated list of Online_Verification_Partner partner IDs
ENV olv_partner_ids_env=${olv_partner_ids}

ARG skip_existing_cred_requests_for_partner=true

# This is for optimization. Set to true to skip existing credential requests for the partner found in the chunk of query result.
# Set to false if credentials to be requested again even if the request entry already exist for partner.
ENV skip_existing_cred_requests_for_partner_env=${skip_existing_cred_requests_for_partner}

COPY ./target/id-repository-credentials-feeder-*.jar id-repository-credentials-feeder.jar

# change permissions of file inside working dir
RUN chown -R ${container_user}:${container_user} /home/${container_user}

# select container user for all tasks
USER ${container_user_uid}:${container_user_gid}
#EXPOSE 8092

CMD wget -q --show-progress "${iam_adapter_url_env}" -O "${loader_path_env}"/kernel-auth-adapter.jar; \
    java -Dloader.path="${loader_path_env}" -Dspring.cloud.config.label="${spring_config_label_env}" -Dspring.profiles.active="${active_profile_env}" -Dspring.cloud.config.uri="${spring_config_url_env}" -Dserver.port="${spring_server_port}" -Donline-verification-partner-ids="${olv_partner_ids_env}" -Dskip-requesting-existing-credentials-for-partners="${skip_existing_cred_requests_for_partner_env}" -jar id-repository-credentials-feeder.jar;
