@echo off
REM Host JVM + local Maven/Docker helpers (Windows cmd).
REM Prerequisites: Docker deps (this script's "up" / "wipe", or bash run-local-stack.sh up)
REM
REM Usage (from local-dev-setup):
REM   run-idrepo-local.bat                 rebuild jar, then start :8090
REM   run-idrepo-local.bat run             same as default
REM   run-idrepo-local.bat start           start existing jar (no Maven)
REM   run-idrepo-local.bat rebuild         mvn package only (do not start)
REM   run-idrepo-local.bat stop            stop the host JVM
REM   run-idrepo-local.bat smoke           health / UIN / idschema curls
REM   run-idrepo-local.bat test            unit tests (mvn test)
REM   run-idrepo-local.bat sonar           unit tests + jacoco + sonar profile
REM   run-idrepo-local.bat wipe            clear DB (compose down -v) then start deps
REM   run-idrepo-local.bat wipe-run        wipe, then rebuild + start
REM   run-idrepo-local.bat up              start Docker deps (keep DB)
REM   run-idrepo-local.bat --no-build      alias for start
REM   run-idrepo-local.bat help

setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"

set "MAVEN_PARENT=%~dp0.."
set "SERVICE_DIR=%MAVEN_PARENT%\id-repository-service"
set "TARGET_DIR=%SERVICE_DIR%\target"
set "DEPS_DIR=%~dp0deps"
set "LOADER_PATH=%DEPS_DIR%\additional_jars"
set "IDOBJECTVALIDATOR_JAR=%LOADER_PATH%\kernel-ref-idobjectvalidator.jar"
set "CONFIG_URI=http://localhost:51100/config"
set "JAVA_OPTS=-Xms512m -Xmx1536m"
set "DO_BUILD=1"
set "AFTER_WIPE="

set "CMD=%~1"
if "%CMD%"=="" set "CMD=run"

if /i "%CMD%"=="-h" set "CMD=help"
if /i "%CMD%"=="--help" set "CMD=help"
if "%CMD%"=="/?" set "CMD=help"
if /i "%CMD%"=="--build" set "CMD=run"
if /i "%CMD%"=="--no-build" set "CMD=start"
if /i "%CMD%"=="build" set "CMD=rebuild"
if /i "%CMD%"=="cleardb" set "CMD=wipe"
if /i "%CMD%"=="clear-db" set "CMD=wipe"
if /i "%CMD%"=="reset" set "CMD=wipe"

call :print_help
if /i "%CMD%"=="help" (
  endlocal
  exit /b 0
)
echo Selected: %CMD%
echo.

if /i "%CMD%"=="stop" (
  call :stop_app
  endlocal
  exit /b 0
)
if /i "%CMD%"=="rebuild" goto :rebuild
if /i "%CMD%"=="test" goto :unit_test
if /i "%CMD%"=="sonar" goto :sonar
if /i "%CMD%"=="smoke" goto :smoke
if /i "%CMD%"=="up" goto :up_deps
if /i "%CMD%"=="wipe" goto :wipe
if /i "%CMD%"=="wipe-run" (
  set "AFTER_WIPE=run"
  goto :wipe
)
if /i "%CMD%"=="start" (
  set "DO_BUILD=0"
  goto :run
)
if /i "%CMD%"=="run" goto :run

echo ERROR: unknown command "%CMD%". Use one of the commands above.
endlocal
exit /b 1

:print_help
echo.
echo run-idrepo-local.bat [command]
echo.
echo App ^(host JVM on http://localhost:8090^):
echo   run              Rebuild jar, then start  ^(default^)
echo   start            Start existing jar, no Maven
echo   rebuild          mvn clean package, skip tests  ^(do not start^)
echo   stop             Stop the host JVM
echo.
echo Docker deps ^(Postgres, config-server, WireMock, MinIO, keymanager^):
echo   up               Start deps, keep DB volumes
echo   wipe             Stop app, clear DB ^(compose down -v^), start deps  - PKCS12 kept
echo   wipe-run         wipe, then rebuild + start app
echo.
echo Checks:
echo   smoke            GET health / UIN / RID / idschema
echo   test             mvn test on id-repository-service
echo   sonar            mvn verify -Psonar  ^(needs SONAR_TOKEN for SonarCloud^)
echo.
echo   help             Show this list and exit
echo.
echo Aliases: --build=run  --no-build=start  build=rebuild  cleardb / clear-db / reset=wipe  -h / --help / /?=help
echo.
echo Wipe and rerun api-test:
echo   1^) run-idrepo-local.bat wipe-run      ^(wait for the app to start^)
echo   2^) ..\..\api-test\run-local-smoke.bat smokeAndRegression
echo.
exit /b 0

:rebuild
call :mvn_package
if errorlevel 1 (
  endlocal
  exit /b 1
)
call :find_jar
if errorlevel 1 (
  endlocal
  exit /b 1
)
echo Jar ready: %JAR%
endlocal
exit /b 0

:unit_test
echo Running unit tests...
pushd "%MAVEN_PARENT%"
call mvn -pl id-repository-service -am test -Dgpg.skip=true -Dmaven.javadoc.skip=true
set "RC=!errorlevel!"
popd
if not "!RC!"=="0" (
  echo ERROR: Maven test failed.
  endlocal
  exit /b 1
)
echo Unit tests passed.
endlocal
exit /b 0

:sonar
if not defined SONAR_TOKEN (
  echo NOTE: SONAR_TOKEN is not set. SonarCloud upload will fail unless you set it.
  echo       Local jacoco report still runs under target\site\jacoco
)
echo Running verify + sonar profile...
pushd "%MAVEN_PARENT%"
call mvn -pl id-repository-service -am verify -Psonar -Dgpg.skip=true -Dmaven.javadoc.skip=true
set "RC=!errorlevel!"
popd
if not "!RC!"=="0" (
  echo ERROR: sonar / verify failed.
  endlocal
  exit /b 1
)
echo Sonar step finished.
endlocal
exit /b 0

:smoke
set "SMOKE_FAIL="
call :smoke_one "id-repo health" "http://localhost:8090/actuator/health"
call :smoke_one "keymanager health" "http://localhost:8088/v1/keymanager/actuator/health"
call :smoke_one "idgenerator UIN" "http://localhost:8082/v1/idgenerator/uin"
call :smoke_one "ridgenerator RID" "http://localhost:8082/v1/ridgenerator/generate/rid/10001/10001"
call :smoke_one "masterdata idschema" "http://localhost:8082/v1/masterdata/idschema/latest?schemaVersion=0"
if defined SMOKE_FAIL (
  echo One or more smoke checks failed.
  endlocal
  exit /b 1
)
echo All smoke checks passed.
endlocal
exit /b 0

:up_deps
call "%DEPS_DIR%\restart-idrepo.bat"
endlocal
exit /b %ERRORLEVEL%

:wipe
call :stop_app
echo Wiping Docker volumes ^(empty DB, re-runs init.sql^). PKCS12 under keys\ is kept.
pushd "%DEPS_DIR%"
docker compose down -v --remove-orphans
set "RC=!errorlevel!"
popd
if not "!RC!"=="0" (
  echo ERROR: docker compose down failed.
  endlocal
  exit /b 1
)
call "%DEPS_DIR%\restart-idrepo.bat"
if errorlevel 1 (
  endlocal
  exit /b 1
)
if /i "%AFTER_WIPE%"=="run" goto :run
echo.
echo DB wiped. Start the host app:
echo   run-idrepo-local.bat run
endlocal
exit /b 0

:run
call :stop_app
if /i "%DO_BUILD%"=="1" (
  call :mvn_package
  if errorlevel 1 (
    endlocal
    exit /b 1
  )
)
call :find_jar
if errorlevel 1 (
  endlocal
  exit /b 1
)
if not exist "%IDOBJECTVALIDATOR_JAR%" (
  echo ERROR: missing %IDOBJECTVALIDATOR_JAR%
  echo kernel-ref-idobjectvalidator is loaded from additional_jars, not Maven.
  echo Run: bash run-local-stack.sh prep
  echo See deps\additional_jars\README.md
  endlocal
  exit /b 1
)

echo Waiting for config-server at %CONFIG_URI% ...
set /a _i=0
:wait_cfg
curl.exe -sf "%CONFIG_URI%/id-repository/default" >nul 2>&1
if not errorlevel 1 goto cfg_ok
set /a _i+=1
if !_i! geq 60 (
  echo ERROR: config-server not reachable. Start deps first:
  echo   run-idrepo-local.bat up
  echo   or: bash run-local-stack.sh up
  endlocal
  exit /b 1
)
timeout /t 2 /nobreak >nul
goto wait_cfg
:cfg_ok
echo config-server is ready.

set "LOG_DIR=%~dp0.local\logs"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

echo.
echo Starting id-repository-service on http://localhost:8090
echo Jar: %JAR%
echo loader.path: %IDOBJECTVALIDATOR_JAR%
echo Logs: %LOG_DIR%
echo Stop with Ctrl+C  or:  run-idrepo-local.bat stop
echo.

java %JAVA_OPTS% ^
  -Dfile.encoding=UTF-8 ^
  --add-opens java.base/java.lang=ALL-UNNAMED ^
  --add-opens java.base/java.lang.reflect=ALL-UNNAMED ^
  --add-opens java.base/java.io=ALL-UNNAMED ^
  -Dloader.path="%IDOBJECTVALIDATOR_JAR%" ^
  -Dspring.cloud.config.uri=%CONFIG_URI% ^
  -Dspring.config.import=configserver:%CONFIG_URI% ^
  -Dspring.cloud.config.label=develop ^
  -Dspring.profiles.active=default,local ^
  -Dspring.cloud.config.override-none=true ^
  -Dspring.cloud.loadbalancer.enabled=false ^
  -Dserver.port=8090 ^
  -Dlogging.file.path="%LOG_DIR%" ^
  -jar "%JAR%"

endlocal
exit /b %ERRORLEVEL%

:mvn_package
echo Building id-repository-service...
pushd "%MAVEN_PARENT%"
REM Full package lifecycle so git-commit-id (validate) + spring-boot:build-info run;
REM cherry-picked compile/jar/repackage skips META-INF/build-info.properties and git.properties.
call mvn -pl id-repository-service clean package -Dmaven.test.skip=true -Dgpg.skip=true -Dmaven.javadoc.skip=true
set "RC=!errorlevel!"
popd
if not "!RC!"=="0" (
  echo ERROR: Maven build failed.
  exit /b 1
)
exit /b 0

:find_jar
set "JAR="
for %%F in ("%TARGET_DIR%\id-repository-service-*.jar") do (
  echo %%~nxF | findstr /i /r "sources javadoc tests" >nul
  if errorlevel 1 set "JAR=%%~fF"
)
if not defined JAR (
  echo ERROR: id-repository-service jar not found under:
  echo   %TARGET_DIR%
  echo Run: run-idrepo-local.bat rebuild
  exit /b 1
)
exit /b 0

:smoke_one
set "NAME=%~1"
set "URL=%~2"
curl.exe -sf --max-time 8 "%URL%" >nul 2>&1
if errorlevel 1 (
  echo FAIL  %NAME%  %URL%
  set "SMOKE_FAIL=1"
) else (
  echo OK    %NAME%  %URL%
)
exit /b 0

:stop_app
echo Stopping host id-repository-service if it is running...
powershell -NoProfile -Command "Get-CimInstance Win32_Process | Where-Object { $_.Name -eq 'java.exe' -and $_.CommandLine -like '*id-repository-service*.jar*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }"
exit /b 0
