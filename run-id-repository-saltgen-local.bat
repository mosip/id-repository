@echo off
setlocal EnableExtensions EnableDelayedExpansion

REM =============================================================================
REM  MOSIP ID-Repository salt generator - local one-shot job
REM  Config from local config server (profile local). No local-run.env.bat required.
REM =============================================================================

cd /d "%~dp0"
set "MAVEN_MODULE_DIR=%CD%\id-repository"

if not defined BUILD_FIRST set "BUILD_FIRST=1"

set "JVM_ARGS=-Dspring.cloud.config.uri=http://localhost:51000"
if defined SPRING_CLOUD_CONFIG_URI set "JVM_ARGS=%JVM_ARGS% -Dspring.cloud.config.uri=%SPRING_CLOUD_CONFIG_URI%"
if defined SPRING_CLOUD_CONFIG_LABEL set "JVM_ARGS=%JVM_ARGS% -Dspring.cloud.config.label=%SPRING_CLOUD_CONFIG_LABEL%"
if defined SPRING_PROFILES_ACTIVE set "JVM_ARGS=%JVM_ARGS% -Dspring.profiles.active=%SPRING_PROFILES_ACTIVE%"

cd /d "%MAVEN_MODULE_DIR%"

if "%BUILD_FIRST%"=="1" (
  call mvn -q install -pl id-repository-salt-generator -am -Dmaven.test.skip=true -Dgpg.skip=true -Dmaven.javadoc.skip=true
  if errorlevel 1 exit /b 1
)

echo [RUN] Salt generator (one-shot) ...
call mvn -pl id-repository-salt-generator spring-boot:run ^
  -Dspring-boot.run.jvmArguments="%JVM_ARGS%" ^
  -Dgpg.skip=true ^
  -Dmaven.test.skip=true

endlocal
