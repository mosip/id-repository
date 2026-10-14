@echo off
REM Bring local Docker deps up (healthy order). Project: idrepo-local.
REM id-repository-service runs on the host — after this, start:
REM   ..\run-idrepo-local.bat

setlocal
cd /d "%~dp0"

echo [1/2] Starting postgres + config-server...
docker compose up -d --remove-orphans ^
  postgres config-server
if errorlevel 1 (
  echo ERROR: postgres/config startup failed.
  exit /b 1
)

echo [2/2] Starting keymanager (bootstrap) + remaining deps...
docker compose up -d --remove-orphans ^
  mock-service minio minio-init ^
  keymanager-service
if errorlevel 1 (
  echo ERROR: dependency startup failed.
  exit /b 1
)

echo.
echo Deps are up. Start ID-Repository on the host:
echo   ..\run-idrepo-local.bat
echo   (rebuilds then starts; use --no-build to skip Maven)
echo Health: http://localhost:8090/actuator/health
docker compose ps
endlocal
