#!/usr/bin/env bash
# Run api-test against local docker-compose without touching Idrepo.properties (server/QA config).
# Usage: ./run-local-smoke.sh [smoke|smokeAndRegression]
#   default: smokeAndRegression
# Output: console + logs/run-local-<testLevel>-<timestamp>.log
# Prereqs: local-dev-setup stack healthy; WireMock mappings for apitest-proxy-* loaded.
# Secrets: export env vars, or copy run-local.env.example to .env.local (gitignored).
set -euo pipefail

API_TEST_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$API_TEST_DIR"

TEST_LEVEL="${1:-smokeAndRegression}"

REQUIRED_ENV_KEYS=(
  postgres-password
  keycloak_Password
  mosip_idrepo_client_secret
  mosip_admin_client_secret
  mosip_testrig_client_secret
  mosip_partner_client_secret
  mosip_pms_client_secret
  mosip_resident_client_secret
  mosip_reg_client_secret
  mosip_hotlist_client_secret
  mosip_regproc_client_secret
  mpartner_default_mobile_secret
  AuthClientSecret
  mosip_crvs1_client_secret
)

DOTENV_KEYS=()
DOTENV_VALS=()

load_dotenv_file() {
  local file="$1"
  [[ -f "$file" ]] || return 0
  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line%$'\r'}"
    [[ -z "$line" || "$line" =~ ^[[:space:]]*# ]] && continue
    local key="${line%%=*}"
    local val="${line#*=}"
    key="${key#"${key%%[![:space:]]*}"}"
    key="${key%"${key##*[![:space:]]}"}"
    [[ -z "$key" ]] && continue
    DOTENV_KEYS+=("$key")
    DOTENV_VALS+=("$val")
  done < "$file"
}

dotenv_get() {
  local want="$1"
  local i
  for i in "${!DOTENV_KEYS[@]}"; do
    if [[ "${DOTENV_KEYS[$i]}" == "$want" ]]; then
      printf '%s' "${DOTENV_VALS[$i]}"
      return 0
    fi
  done
  return 1
}

resolve_env_value() {
  local key="$1"
  local val
  val="$(printenv "$key" 2>/dev/null || true)"
  if [[ -n "$val" ]]; then
    printf '%s' "$val"
    return 0
  fi
  if val="$(dotenv_get "$key")"; then
    printf '%s' "$val"
    return 0
  fi
  return 1
}

load_dotenv_file "$API_TEST_DIR/.env.local"

missing=()
ENV_ARGS=()
for key in "${REQUIRED_ENV_KEYS[@]}"; do
  if val="$(resolve_env_value "$key")" && [[ -n "$val" ]]; then
    ENV_ARGS+=("${key}=${val}")
  else
    missing+=("$key")
  fi
done

if [[ ${#missing[@]} -gt 0 ]]; then
  echo "ERROR: missing required local-run secrets:" >&2
  printf '  - %s\n' "${missing[@]}" >&2
  echo "Export them, or copy run-local.env.example to .env.local and fill values from README (Local secrets)." >&2
  exit 1
fi

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
set +e
env \
  "${ENV_ARGS[@]}" \
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
