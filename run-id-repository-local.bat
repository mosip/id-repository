@echo off
setlocal EnableExtensions EnableDelayedExpansion

REM =============================================================================
REM  MOSIP ID-Repository - local Windows runner
REM  All runtime config (DB, Keycloak, URLs) comes from your local config server.
REM  Prerequisites: kernel-config-server on http://localhost:51000, JDK 21, Maven.
REM  Optional env overrides (no file needed): SPRING_CLOUD_CONFIG_URI, SPRING_CLOUD_CONFIG_LABEL
REM  MOSIP_KERNEL_UIN_SALT — overrides default UIN crypto salt below
REM  MOSIP_KERNEL_PARTNERCODE_SALT — overrides default partner-code crypto salt below
REM  IDREPO_WEBSUB_VID_CREDENTIAL_UPDATE_SECRET — WebSub VID credential update HMAC secret
REM  MOSIP_IDREPO_BIOSDK_URL — biosdk base host (no /biosdk-service path; config appends it)
REM  Also sets -Dmosip.api.internal.host and -Dmosip.biosdk.default.service.url (required for BioAPIFactory init)
REM  Object store: matches mc alias "local" — http://localhost:9000, bucket idrepo, accessKey admin
REM  MOSIP_OBJECT_STORE_S3_URL / MOSIP_IDREPO_OBJECTSTORE_BUCKET_NAME / MOSIP_OBJECT_STORE_S3_* optional overrides
REM  PMS/audit: application-default uses api.dev2 for mosip.pms.partnermanager.url; local run overrides
REM  mosip.pms.partnermanager.url + mosip.kernel.auditmanager.url to api-internal (see JVM_ARGS).
REM  PARTNER_POLICY / PARTNER_EXTRACTION_POLICY paths come from id-repository-default.properties on config server.
REM  KER-ATH-403 on PMS = service-account token rejected (mosip-creser-client audience/roles on partnermanager).
REM  Bio-SDK: biosdk-client 1.4.0-SNAPSHOT + Util.java shadow (Spring 7; Maven SNAPSHOT != GitHub develop fix).
REM  WebSub: kernel-websubclient-api shadows (SubscriberClientImpl/PublisherClientImpl).
REM  kernel-auth: TokenHelper/ValidateTokenHelper/TokenValidationHelper shadows + IdRepoKernelAuthHelperConfig.
REM  IAM: auth.server.admin.offline.comp.token.validate=false in application.properties (local profile offline auth is deprecated).
REM  LoadBalancer: iam.dev2.mosip.net is external DNS, not a K8s service — JVM disables LB (see application.properties).
REM  Self-token: client id / app id come from config server (mosip.iam.adapter.*.id-repository).
REM  Optional: MOSIP_IAM_ADAPTER_CLIENTSECRET if secret is not in config server overrides.
REM  ID object ref validator: id-repository/lib/kernel-ref-idobjectvalidator-1.3.1-rc.1.jar (system scope in service pom).
REM =============================================================================

cd /d "%~dp0"
set "MAVEN_MODULE_DIR=%CD%\id-repository"

if not defined BUILD_FIRST set "BUILD_FIRST=1"
if not defined MOSIP_IDREPO_BIOSDK_URL set "MOSIP_IDREPO_BIOSDK_URL=https://api-internal.dev2.mosip.net"
REM BioSDK Client_V_1_0.init uses mosip.biosdk.default.service.url (often http://${mosip.api.internal.host}/biosdk-service).
REM Config server may leave the host as the literal "mosip.api.internal.host" — override both keys for local runs.
set "MOSIP_API_INTERNAL_HOST=%MOSIP_IDREPO_BIOSDK_URL:https://=%"
set "MOSIP_API_INTERNAL_HOST=%MOSIP_API_INTERNAL_HOST:http://=%"
if not defined MOSIP_BIOSDK_DEFAULT_SERVICE_URL set "MOSIP_BIOSDK_DEFAULT_SERVICE_URL=%MOSIP_IDREPO_BIOSDK_URL%/biosdk-service"
if not defined MOSIP_OBJECT_STORE_S3_URL set "MOSIP_OBJECT_STORE_S3_URL=http://localhost:9000"
if not defined MOSIP_IDREPO_OBJECTSTORE_BUCKET_NAME set "MOSIP_IDREPO_OBJECTSTORE_BUCKET_NAME=idrepo"
if not defined MOSIP_OBJECT_STORE_S3_ACCESSKEY set "MOSIP_OBJECT_STORE_S3_ACCESSKEY=admin"
if not defined MOSIP_OBJECT_STORE_S3_SECRETKEY set "MOSIP_OBJECT_STORE_S3_SECRETKEY=en2oJa2nuE"
if not defined IDREPO_LOG_DIR set "IDREPO_LOG_DIR=%~dp0logs"
if not exist "%IDREPO_LOG_DIR%" mkdir "%IDREPO_LOG_DIR%"

echo.
echo ============================================================
echo   MOSIP ID-Repository - Local Run
echo ============================================================
echo   Config server : http://localhost:51000/config
echo   Config label  : 1.1.2 (override: set SPRING_CLOUD_CONFIG_LABEL)
echo   Profile       : default (override: set SPRING_PROFILES_ACTIVE)
echo   HTTP port     : 8090 (from bootstrap / config server)
echo   Bio-SDK URL   : %MOSIP_BIOSDK_DEFAULT_SERVICE_URL%
echo   API host      : %MOSIP_API_INTERNAL_HOST%
echo   Object store  : %MOSIP_OBJECT_STORE_S3_URL% bucket=%MOSIP_IDREPO_OBJECTSTORE_BUCKET_NAME% user=%MOSIP_OBJECT_STORE_S3_ACCESSKEY%
echo   Logs          : console + file
echo   Log file      : %IDREPO_LOG_DIR%\id-repository.log
echo.

if defined JAVA_HOME (
  set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
  set "JAVA_EXE=java"
)
"%JAVA_EXE%" -version 2>nul
if errorlevel 1 goto :no_java

where mvn >nul 2>&1
if errorlevel 1 goto :no_maven

if not exist "%MAVEN_MODULE_DIR%\pom.xml" goto :no_pom
goto :checks_ok

:no_java
echo [ERROR] Java missing. Install JDK 21 and set JAVA_HOME.
exit /b 1
:no_maven
echo [ERROR] Maven (mvn) missing on PATH.
exit /b 1
:no_pom
echo [ERROR] Maven project missing: %MAVEN_MODULE_DIR%
exit /b 1
:checks_ok

if not defined MOSIP_KERNEL_UIN_SALT set "MOSIP_KERNEL_UIN_SALT=9cl3KUcCASLyUYLD"
if not defined MOSIP_KERNEL_PARTNERCODE_SALT set "MOSIP_KERNEL_PARTNERCODE_SALT=4exm0iwskVVkV4vg"
if not defined IDREPO_WEBSUB_VID_CREDENTIAL_UPDATE_SECRET set "IDREPO_WEBSUB_VID_CREDENTIAL_UPDATE_SECRET=qbcNVWL7FzhTxHEi"
REM Datashare: config server uses K8s host datashare.datashare — unreachable from a laptop.
REM Point at the same api-internal host used for keymanager/biosdk (https).
set "JVM_ARGS=-Dspring.cloud.bootstrap.enabled=false -Dspring.cloud.config.uri=http://localhost:51000/config -Dspring.profiles.active=default -Dspring.cloud.loadbalancer.enabled=false -Dspring.autoconfigure.exclude=org.springframework.cloud.loadbalancer.config.LoadBalancerAutoConfiguration,org.springframework.cloud.loadbalancer.config.BlockingLoadBalancerClientAutoConfiguration,org.springframework.cloud.loadbalancer.config.LoadBalancerCacheAutoConfiguration,org.springframework.cloud.loadbalancer.config.LoadBalancerStatsAutoConfiguration,org.springframework.cloud.loadbalancer.security.OAuth2LoadBalancerClientAutoConfiguration -Dlogging.file.path=%IDREPO_LOG_DIR% -Dmosip.kernel.uin.salt=%MOSIP_KERNEL_UIN_SALT% -Dmosip.kernel.partnercode.salt=%MOSIP_KERNEL_PARTNERCODE_SALT% -Didrepo.websub.vid.credential.update.secret=%IDREPO_WEBSUB_VID_CREDENTIAL_UPDATE_SECRET% -Dmosip.api.internal.host=%MOSIP_API_INTERNAL_HOST% -Dmosip.idrepo.biosdk.url=%MOSIP_IDREPO_BIOSDK_URL% -Dmosip.biosdk.default.service.url=%MOSIP_BIOSDK_DEFAULT_SERVICE_URL% -Dmosip.kernel.auditmanager.url=%MOSIP_IDREPO_BIOSDK_URL% -Dmosip.pms.partnermanager.url=%MOSIP_IDREPO_BIOSDK_URL% -Dmosip.data.share.internal.domain.name=%MOSIP_API_INTERNAL_HOST% -Dmosip.data.share.protocol=https -Dobject.store.s3.url=%MOSIP_OBJECT_STORE_S3_URL% -Dobject.store.s3.accesskey=%MOSIP_OBJECT_STORE_S3_ACCESSKEY% -Dobject.store.s3.secretkey=%MOSIP_OBJECT_STORE_S3_SECRETKEY% -Dmosip.idrepo.objectstore.bucket-name=%MOSIP_IDREPO_OBJECTSTORE_BUCKET_NAME% -Ds3.pretext.value= -Dobject.store.s3.bucket-name-prefix= -Dobject.store.client.execution.timeout=60000"
if defined SPRING_CLOUD_CONFIG_URI set "JVM_ARGS=%JVM_ARGS% -Dspring.cloud.config.uri=%SPRING_CLOUD_CONFIG_URI%"
if defined SPRING_CLOUD_CONFIG_LABEL set "JVM_ARGS=%JVM_ARGS% -Dspring.cloud.config.label=%SPRING_CLOUD_CONFIG_LABEL%"
if defined MOSIP_IAM_ADAPTER_CLIENTSECRET set "JVM_ARGS=%JVM_ARGS% -Dmosip.iam.adapter.clientsecret=%MOSIP_IAM_ADAPTER_CLIENTSECRET% -Dmosip.iam.adapter.clientsecret.id-repository=%MOSIP_IAM_ADAPTER_CLIENTSECRET%"
if defined SPRING_PROFILES_ACTIVE set "JVM_ARGS=%JVM_ARGS% -Dspring.profiles.active=%SPRING_PROFILES_ACTIVE%"

if not defined MOSIP_IAM_ADAPTER_CLIENTSECRET (
  echo [WARN] MOSIP_IAM_ADAPTER_CLIENTSECRET not set — using mosip.iam.adapter.clientsecret from config server.
  echo        Set MOSIP_IAM_ADAPTER_CLIENTSECRET only if config server does not expose the secret locally.
)

echo --- Endpoints after startup ---
echo   Health        : http://localhost:8090/actuator/health
echo   Actuator      : http://localhost:8090/actuator/info  (also /prometheus, /refresh, /restart)
echo   Swagger Identity : http://localhost:8090/idrepository/v1/identity/swagger-ui/index.html
echo   Swagger VID      : http://localhost:8090/idrepository/v1/swagger-ui/index.html
echo   Swagger Cred     : http://localhost:8090/v1/credentialservice/swagger-ui/index.html
echo   Swagger CredReq  : http://localhost:8090/v1/credentialrequest/swagger-ui/index.html
echo   OpenAPI Identity : http://localhost:8090/idrepository/v1/identity/v3/api-docs
echo   OpenAPI VID      : http://localhost:8090/idrepository/v1/v3/api-docs
echo   OpenAPI Cred     : http://localhost:8090/v1/credentialservice/v3/api-docs
echo   OpenAPI CredReq  : http://localhost:8090/v1/credentialrequest/v3/api-docs
echo   Identity      : http://localhost:8090/idrepository/v1/identity/
echo   Identity draft: http://localhost:8090/idrepository/v1/identity/draft/
echo   VID           : http://localhost:8090/idrepository/v1/vid/
echo   Credential    : http://localhost:8090/v1/credentialservice/
echo   Cred request  : http://localhost:8090/v1/credentialrequest/
echo.
echo Press Ctrl+C to stop.
echo ============================================================
echo.

cd /d "%MAVEN_MODULE_DIR%"

if "%BUILD_FIRST%"=="1" goto :do_build
goto :run_app
:do_build
echo [BUILD] mvn clean install -pl id-repository-service -am ...
call mvn -q clean install -pl id-repository-service -am -Dmaven.test.skip=true -Dgpg.skip=true -Dmaven.javadoc.skip=true
if errorlevel 1 goto :build_failed
echo [OK] Build complete.
echo.
:run_app

echo [RUN] Starting IdRepositoryBootApplication ...
call mvn -pl id-repository-service spring-boot:run ^
  -Dspring-boot.run.jvmArguments="%JVM_ARGS%" ^
  -Dgpg.skip=true ^
  -Dmaven.test.skip=true

set "EXIT_CODE=%ERRORLEVEL%"
echo.
if "%EXIT_CODE%"=="0" goto :exit_ok
echo [ERROR] Application exited with code %EXIT_CODE%
goto :exit_end
:build_failed
echo [ERROR] Maven build failed.
set "EXIT_CODE=1"
goto :exit_end
:exit_ok
echo [OK] Application stopped.
:exit_end
endlocal & exit /b %EXIT_CODE%
