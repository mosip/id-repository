@echo off
REM Bring dependencies up first, then start/recreate id-repository-service.
REM Prefer this over "docker compose restart id-repository-service"
REM (restart does not re-check depends_on health).

setlocal
cd /d "%~dp0"

echo [1/3] Seeding ZK keys (keys-generator writes PKCS12 before keymanager loads it)...
docker compose up -d ^
  database config-server keys-generator keys-generator-expiry
if errorlevel 1 (
  echo ERROR: keys-generator startup failed.
  exit /b 1
)

echo [2/3] Starting keymanager + remaining deps (Keycloak mints WireMock auth JWT first)...
docker compose up -d ^
  keycloak keycloak-init auth-token-bridge uin-generator websub-mock mock-service minio minio-init biosdk-service ^
  keymanager-service keymanager-init datashare-service
if errorlevel 1 (
  echo ERROR: dependency startup failed.
  exit /b 1
)

echo [3/3] Starting id-repository-service (waits for all depends_on)...
docker compose up -d --force-recreate id-repository-service
if errorlevel 1 (
  echo ERROR: id-repository-service failed to start.
  exit /b 1
)

echo.
docker compose ps
echo.
echo Done. Check: curl http://localhost:8090/actuator/health
endlocal
