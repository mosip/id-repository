@echo off
REM Run api-test against local id-repository-service without touching Idrepo.properties.
REM Usage: run-local-smoke.bat [smoke|smokeAndRegression]
REM   default: smokeAndRegression
REM Output: console + logs\run-local-<testLevel>-<timestamp>.log
REM Prereqs: Docker deps up; host id-repository-service on :8090; WireMock IAM on :8082.
REM Secrets: optional .env.local; otherwise local-dev-setup defaults are used.
REM Skip health preflight: set SKIP_PREFLIGHT=1
setlocal EnableExtensions EnableDelayedExpansion

set "API_TEST_DIR=%~dp0"
cd /d "%API_TEST_DIR%"

set "TEST_LEVEL=%~1"
if "%TEST_LEVEL%"=="" set "TEST_LEVEL=smokeAndRegression"

if exist "%API_TEST_DIR%.env.local" (
  for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%API_TEST_DIR%.env.local") do (
    if not "%%A"=="" if not defined %%A set "%%A=%%B"
  )
)

if not defined postgres-password set "postgres-password=mosip123"
if not defined keycloak_Password set "keycloak_Password=admin"
if not defined mosip_idrepo_client_secret set "mosip_idrepo_client_secret=QTGizTYN4US0XHOU"
if not defined mosip_testrig_client_secret set "mosip_testrig_client_secret=local-dev-testrig-secret"
if not defined mosip_admin_client_secret set "mosip_admin_client_secret=local-dev-secret"
if not defined mosip_partner_client_secret set "mosip_partner_client_secret=local-dev-secret"
if not defined mosip_pms_client_secret set "mosip_pms_client_secret=local-dev-secret"
if not defined mosip_resident_client_secret set "mosip_resident_client_secret=local-dev-secret"
if not defined mosip_reg_client_secret set "mosip_reg_client_secret=local-dev-secret"
if not defined mosip_hotlist_client_secret set "mosip_hotlist_client_secret=local-dev-secret"
if not defined mosip_regproc_client_secret set "mosip_regproc_client_secret=local-dev-secret"
if not defined mpartner_default_mobile_secret set "mpartner_default_mobile_secret=local-dev-secret"
if not defined AuthClientSecret set "AuthClientSecret=local-dev-secret"
if not defined mosip_crvs1_client_secret set "mosip_crvs1_client_secret=local-dev-secret"

if /I not "%SKIP_PREFLIGHT%"=="1" (
  echo Checking local stack ^(id-repository-service + WireMock + keymanager^)...
  set "PREFLIGHT_FAIL="
  curl.exe -sf --max-time 5 http://localhost:8082/__admin/health >nul 2>&1
  if errorlevel 1 (
    echo ERROR: WireMock local IAM is not reachable: http://localhost:8082/__admin/health
        echo        Start Docker deps: id-repository\local-dev-setup  ^(bash run-local-stack.sh up^)
    set "PREFLIGHT_FAIL=1"
  ) else (
    echo OK  WireMock local IAM  http://localhost:8082
  )
  curl.exe -sf --max-time 5 http://localhost:8090/actuator/health >nul 2>&1
  if errorlevel 1 (
    echo ERROR: id-repository-service is not reachable: http://localhost:8090/actuator/health
    echo        Start the host app: id-repository\local-dev-setup\run-idrepo-local.bat
    set "PREFLIGHT_FAIL=1"
  ) else (
    echo OK  id-repository-service  http://localhost:8090
  )
  curl.exe -sf --max-time 5 http://localhost:8088/v1/keymanager/actuator/health >nul 2>&1
  if errorlevel 1 (
    echo ERROR: keymanager-service is not reachable: http://localhost:8088/v1/keymanager/actuator/health
    echo        Wait for keymanager bootstrap, or recreate the container.
    set "PREFLIGHT_FAIL=1"
  ) else (
    echo OK  keymanager-service  http://localhost:8088
  )
  if defined PREFLIGHT_FAIL (
    echo Set SKIP_PREFLIGHT=1 to bypass ^(tests will still fail if the service is down^).
    exit /b 1
  )
)

set "authCertsPath=%API_TEST_DIR%target\local-authcerts"
if not exist "%authCertsPath%" mkdir "%authCertsPath%"

set "LOG_DIR=%API_TEST_DIR%logs"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"
for /f %%I in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd_HHmmss"') do set "STAMP=%%I"
set "LOG_FILE=%LOG_DIR%\run-local-%TEST_LEVEL%-%STAMP%.log"

set "JAR="
set "NEED_BUILD="
for %%F in ("%API_TEST_DIR%target\apitest-idrepo-*-jar-with-dependencies.jar") do set "JAR=%%~fF"
set "PROPS_FILE=%API_TEST_DIR%src\main\resources\config\Idrepo-local.properties"
set "LOG4J_FILE=%API_TEST_DIR%src\main\resources\log4j.properties"
set "POM_FILE=%API_TEST_DIR%pom.xml"
if not defined JAR set "NEED_BUILD=1"
if defined JAR if exist "%PROPS_FILE%" (
  for /f %%I in ('powershell -NoProfile -Command "if ((Get-Item -LiteralPath '%PROPS_FILE%').LastWriteTime -gt (Get-Item -LiteralPath '%JAR%').LastWriteTime) { '1' }"') do set "NEED_BUILD=%%I"
)
if not defined NEED_BUILD if defined JAR if exist "%LOG4J_FILE%" (
  for /f %%I in ('powershell -NoProfile -Command "if ((Get-Item -LiteralPath '%LOG4J_FILE%').LastWriteTime -gt (Get-Item -LiteralPath '%JAR%').LastWriteTime) { '1' }"') do set "NEED_BUILD=%%I"
)
if not defined NEED_BUILD if defined JAR if exist "%POM_FILE%" (
  for /f %%I in ('powershell -NoProfile -Command "if ((Get-Item -LiteralPath '%POM_FILE%').LastWriteTime -gt (Get-Item -LiteralPath '%JAR%').LastWriteTime) { '1' }"') do set "NEED_BUILD=%%I"
)
if not defined NEED_BUILD if defined JAR (
  for /f %%I in ('powershell -NoProfile -Command "$jar=(Get-Item -LiteralPath '%JAR%').LastWriteTime; if (Get-ChildItem -LiteralPath '%API_TEST_DIR%src\main\resources' -Recurse -File | Where-Object { $_.LastWriteTime -gt $jar } | Select-Object -First 1) { '1' }"') do set "NEED_BUILD=%%I"
)
if not defined NEED_BUILD if defined JAR (
  for /f %%I in ('powershell -NoProfile -Command "$jar=(Get-Item -LiteralPath '%JAR%').LastWriteTime; if (Get-ChildItem -LiteralPath '%API_TEST_DIR%src\main\java' -Recurse -Filter *.java | Where-Object { $_.LastWriteTime -gt $jar } | Select-Object -First 1) { '1' }"') do set "NEED_BUILD=%%I"
)
if defined NEED_BUILD (
  echo Building api-test jar ^(pom/sources/resources newer than jar, or jar missing^)...
  call mvn clean install "-Dgpg.skip=true" "-Dmaven.gitcommitid.skip=true" "-Dmaven.javadoc.skip=true"
  if errorlevel 1 exit /b 1
  for %%F in ("%API_TEST_DIR%target\apitest-idrepo-*-jar-with-dependencies.jar") do set "JAR=%%~fF"
)

if not defined JAR (
  echo ERROR: apitest-idrepo jar-with-dependencies not found under target\
  exit /b 1
)

REM Patch extracted MosipTestResource copy so a pre-built jar still picks up local authCertsPath
set "EXTRACTED_PROPS=%API_TEST_DIR%target\MosipTestResource\MosipTemporaryTestResource\config\Idrepo-local.properties"
if exist "%EXTRACTED_PROPS%" (
  powershell -NoProfile -Command "(Get-Content -Raw '%EXTRACTED_PROPS%') -replace '(?m)^authCertsPath\s*=.*$','authCertsPath = target/local-authcerts' | Set-Content -NoNewline '%EXTRACTED_PROPS%'"
)

(
  echo ===== run-local-smoke start %DATE% %TIME% =====
  echo Using JAR: %JAR%
  echo Idrepo service: http://localhost:8090
  echo WireMock IAM / stubs: http://localhost:8082
  echo Properties: Idrepo-local.properties
  echo testLevel: %TEST_LEVEL%
  echo authCertsPath: %authCertsPath%
  echo Log file: %LOG_FILE%
  echo ================================================
) > "%LOG_FILE%"

type "%LOG_FILE%"

set "LOG_FILE=%LOG_FILE%"
set "LOG_DIR=%LOG_DIR%"
set "JAR=%JAR%"
set "TEST_LEVEL=%TEST_LEVEL%"

REM One PowerShell -Command string (do not split across extra quoted argv; that
REM made java print its usage page). MosipTestRunner sets api.test.log.dir if unset.
REM Optional JVM extras: set JAVA_TOOL_OPTIONS (do not bake -XX:DisableIntrinsic=_inflateBytesBytes —
REM Oracle JDK 21.0.3 rejects that name and refuses to start).
REM Pass absolute authCertsPath so apitest-commons 1.8+ resolveCertsRootDir does not use a cwd-relative path.
powershell -NoProfile -ExecutionPolicy Bypass -Command "& { $ErrorActionPreference = 'Continue'; $log = $env:LOG_FILE; $env:authCertsPath = $env:authCertsPath; $javaArgs = @(('-Dapi.test.log.dir=' + $env:LOG_DIR), '-Didrepo.propertiesFile=Idrepo-local.properties', '-Didrepo.skipPartnerSetup=true', '-Dmodules=idrepo', '-Denv.user=api-internal.local', '-Denv.endpoint=http://localhost:8082', '-Denv.keycloak=http://localhost:8082', ('-Denv.testLevel=' + $env:TEST_LEVEL), '-jar', $env:JAR); & java @javaArgs 2>&1 | ForEach-Object { Write-Host $_; Add-Content -LiteralPath $log -Value $_.ToString() }; $code = $LASTEXITCODE; if ($null -eq $code) { $code = 0 }; Add-Content -LiteralPath $log -Value ('===== run-local-smoke end exit=' + $code + ' ====='); Write-Host ('Log written: ' + $log); exit $code }"

set "RC=!ERRORLEVEL!"
endlocal & exit /b %RC%
