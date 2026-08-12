#!/usr/bin/env bash
# Run api-test against local docker-compose without touching Idrepo.properties (server/QA config).
# Usage: ./run-local-smoke.sh [smoke|smokeAndRegression]
#   default: smokeAndRegression
# Output: console + logs/run-local-<testLevel>-<timestamp>.log
# Prereqs: local-dev-setup stack healthy; WireMock mappings for apitest-proxy-* loaded.
set -euo pipefail

API_TEST_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$API_TEST_DIR"

TEST_LEVEL="${1:-smokeAndRegression}"

shopt -s nullglob
jars=(target/apitest-idrepo-*-jar-with-dependencies.jar)
if [[ ${#jars[@]} -eq 0 ]]; then
  echo "Building api-test jar..."
  mvn clean install -Dgpg.skip=true -Dmaven.gitcommitid.skip=true
  jars=(target/apitest-idrepo-*-jar-with-dependencies.jar)
fi
if [[ ${#jars[@]} -eq 0 ]]; then
  echo "ERROR: apitest-idrepo jar-with-dependencies not found under target/" >&2
  exit 1
fi
JAR="${jars[0]}"

LOG_DIR="$API_TEST_DIR/logs"
mkdir -p "$LOG_DIR"
LOG_FILE="$LOG_DIR/run-local-${TEST_LEVEL}-$(date +%Y%m%d_%H%M%S).log"

AUTH_CERTS_DIR="$API_TEST_DIR/target/local-authcerts"
mkdir -p "$AUTH_CERTS_DIR"

{
  echo "===== run-local-smoke start $(date -Iseconds) ====="
  echo "Using JAR: $JAR"
  echo "Endpoint: http://localhost:8082 (WireMock gateway)"
  echo "Properties: Idrepo-local.properties"
  echo "testLevel: $TEST_LEVEL"
  echo "authCertsPath: $AUTH_CERTS_DIR"
  echo "Log file: $LOG_FILE"
  echo "================================================"
} | tee "$LOG_FILE"

# Hyphenated keys must be passed via env (bash cannot export names with '-').
# Values match local-dev-setup keycloak bootstrap / auth-token-bridge defaults.
set +e
env \
  "postgres-password=mosip123" \
  "keycloak_Password=admin" \
  "mosip_idrepo_client_secret=QTGizTYN4US0XHOU" \
  "mosip_admin_client_secret=local-dev-secret" \
  "mosip_testrig_client_secret=local-dev-testrig-secret" \
  "mosip_partner_client_secret=local-dev-secret" \
  "mosip_pms_client_secret=local-dev-secret" \
  "mosip_resident_client_secret=local-dev-secret" \
  "mosip_reg_client_secret=local-dev-secret" \
  "mosip_hotlist_client_secret=local-dev-secret" \
  "mosip_regproc_client_secret=local-dev-secret" \
  "mpartner_default_mobile_secret=local-dev-secret" \
  "AuthClientSecret=local-dev-secret" \
  "mosip_crvs1_client_secret=local-dev-secret" \
  "authCertsPath=$AUTH_CERTS_DIR" \
  java -Didrepo.propertiesFile=Idrepo-local.properties \
    -Didrepo.skipPartnerSetup=true \
    -Dmodules=idrepo \
    -Denv.user=api-internal.local \
    -Denv.endpoint=http://localhost:8082 \
    -Denv.testLevel="$TEST_LEVEL" \
    -jar "$JAR" 2>&1 | tee -a "$LOG_FILE"
rc=${PIPESTATUS[0]}
set -e

{
  echo "===== run-local-smoke end exit=$rc ====="
  echo "Log written: $LOG_FILE"
} | tee -a "$LOG_FILE"

exit "$rc"
