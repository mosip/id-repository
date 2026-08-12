@echo off
REM Run api-test against local docker-compose without touching Idrepo.properties (server/QA config).
REM Usage: run-local-smoke.bat [smoke|smokeAndRegression]
REM   default: smokeAndRegression
REM Output: console + logs\run-local-<testLevel>-<timestamp>.log
REM Prereqs: local-dev-setup stack healthy; WireMock mappings for apitest-proxy-* loaded.
setlocal EnableExtensions EnableDelayedExpansion

set "API_TEST_DIR=%~dp0"
cd /d "%API_TEST_DIR%"

set "TEST_LEVEL=%~1"
if "%TEST_LEVEL%"=="" set "TEST_LEVEL=smokeAndRegression"

REM --- local-dev secrets as env (ConfigManager prefers getenv over properties file) ---
set "postgres-password=mosip123"
set "keycloak_Password=admin"
set "mosip_idrepo_client_secret=QTGizTYN4US0XHOU"
set "mosip_admin_client_secret=local-dev-secret"
set "mosip_testrig_client_secret=local-dev-testrig-secret"
set "mosip_partner_client_secret=local-dev-secret"
set "mosip_pms_client_secret=local-dev-secret"
set "mosip_resident_client_secret=local-dev-secret"
set "mosip_reg_client_secret=local-dev-secret"
set "mosip_hotlist_client_secret=local-dev-secret"
set "mosip_regproc_client_secret=local-dev-secret"
set "mpartner_default_mobile_secret=local-dev-secret"
set "AuthClientSecret=local-dev-secret"
set "mosip_crvs1_client_secret=local-dev-secret"

set "authCertsPath=%API_TEST_DIR%target\local-authcerts"
if not exist "%authCertsPath%" mkdir "%authCertsPath%"

set "LOG_DIR=%API_TEST_DIR%logs"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"
for /f %%I in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd_HHmmss"') do set "STAMP=%%I"
set "LOG_FILE=%LOG_DIR%\run-local-%TEST_LEVEL%-%STAMP%.log"

set "JAR="
for %%F in ("%API_TEST_DIR%target\apitest-idrepo-*-jar-with-dependencies.jar") do set "JAR=%%~fF"
if not defined JAR (
  echo Building api-test jar...
  call mvn clean install "-Dgpg.skip=true" "-Dmaven.gitcommitid.skip=true"
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
  echo Endpoint: http://localhost:8082  ^(WireMock gateway^)
  echo Properties: Idrepo-local.properties
  echo testLevel: %TEST_LEVEL%
  echo authCertsPath: %authCertsPath%
  echo Log file: %LOG_FILE%
  echo ================================================
) > "%LOG_FILE%"

type "%LOG_FILE%"

REM Tee java stdout/stderr to console and append to log; preserve java exit code.
REM Pass paths via env so quoting stays simple inside PowerShell.
set "LOG_FILE=%LOG_FILE%"
set "JAR=%JAR%"
set "TEST_LEVEL=%TEST_LEVEL%"

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Continue';" ^
  "$log=$env:LOG_FILE;" ^
  "$jar=$env:JAR;" ^
  "$tl=$env:TEST_LEVEL;" ^
  "$args=@('-Didrepo.propertiesFile=Idrepo-local.properties','-Didrepo.skipPartnerSetup=true','-Dmodules=idrepo','-Denv.user=api-internal.local','-Denv.endpoint=http://localhost:8082',('-Denv.testLevel=' + $tl),'-jar',$jar);" ^
  "& java @args 2>&1 | ForEach-Object { $line = $_.ToString(); Write-Host $line; Add-Content -LiteralPath $log -Value $line };" ^
  "$code = $LASTEXITCODE; if ($null -eq $code) { $code = 0 };" ^
  "Add-Content -LiteralPath $log -Value ('===== run-local-smoke end exit=' + $code + ' =====');" ^
  "Write-Host ('Log written: ' + $log);" ^
  "exit $code"

set "RC=!ERRORLEVEL!"
endlocal & exit /b %RC%
