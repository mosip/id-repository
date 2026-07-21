FROM mosipid/openjdk-21-jre:21.0.4

ARG SOURCE
ARG COMMIT_HASH
ARG COMMIT_ID
ARG BUILD_TIME
LABEL source=${SOURCE}
LABEL commit_hash=${COMMIT_HASH}
LABEL commit_id=${COMMIT_ID}
LABEL build_time=${BUILD_TIME}

ARG spring_config_label
ARG active_profile
ARG spring_config_url

ENV active_profile_env=${active_profile}
ENV spring_config_label_env=${spring_config_label}
ENV spring_config_url_env=${spring_config_url}

ARG container_user=mosip
ARG container_user_group=mosip
ARG container_user_uid=1002
ARG container_user_gid=1001

WORKDIR /home/${container_user}

ENV current_module_env=id-repository-salt-generator

ADD target/${current_module_env}-*.jar ${current_module_env}.jar

RUN apt-get -y update \
&& apt-get install -y curl \
&& groupadd -g ${container_user_gid} ${container_user_group} \
&& useradd -u ${container_user_uid} -g ${container_user_group} -s /bin/bash -m ${container_user} \
&& chown -R ${container_user}:${container_user} /home/${container_user}

USER ${container_user_uid}:${container_user_gid}

CMD java -Dspring.cloud.config.label="${spring_config_label_env}" \
  -Dspring.profiles.active="${active_profile_env}" \
  -Dspring.cloud.config.uri="${spring_config_url_env}" \
  -jar ${current_module_env}.jar
