#!/usr/bin/env bash
# Local keymanager bootstrap.
# Default: static SQL seed inlined in deps/init.sql — skip keys-generator
# and generateMasterKey. Set KM_STATIC_SEED=false to use the old jar + API path.
set -euo pipefail

CONFIG_DIR="${KM_CONFIG_DIR:-/home/mosip/config}"
# Only used when KM_STATIC_SEED=false (optional fallback). Not mounted by default.
KG_JAR="${KEYS_GENERATOR_JAR:-/bootstrap/keys-generator.jar}"
READY_MARKER="${CONFIG_DIR}/.km-local-ready"
KM_URL="${KM_URL:-http://127.0.0.1:8088}"
LABEL="${SPRING_CLOUD_CONFIG_LABEL:-develop}"
# Default true: crypto rows come from deps/init.sql (keymgr seed) via postgres init.
# When true, keys-generator.jar is never read or required.
KM_STATIC_SEED="${KM_STATIC_SEED:-true}"

log() { echo "keymanager-bootstrap: $*"; }

run_keys_generator() {
  if [[ ! -f "$KG_JAR" ]]; then
    log "ERROR: missing $KG_JAR — run local-dev prep to copy keys-generator.jar"
    exit 1
  fi
  log "Running keys-generator (IDENTITY_CACHE + data_encrypt_keystore)..."
  java -XX:+UseZGC -Dfile.encoding=UTF-8 \
    -Dspring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration \
    -Dspring.cloud.config.uri=http://config-server:51000/config \
    -Dspring.cloud.config.label="${LABEL}" \
    -Dspring.profiles.active=default \
    -Dspring.cloud.config.name=keymanager,kernel \
    -Dmosip.kernel.database.hostname=postgres \
    -Dmosip.kernel.database.port=5432 \
    -Ddb.dbuser.password=mosip123 \
    -Dkeymanager_database_url=jdbc:postgresql://postgres:5432/mosip_keymgr \
    -Dkeymanager_database_username=keymgruser \
    -Dkeymanager_database_password=mosip123 \
    -Dmosip.kernel.keymanager.hsm.keystore-type=PKCS12 \
    -Dmosip.kernel.keymanager.hsm.config-path="${CONFIG_DIR}/mosip-idrepo-ks.p12" \
    -Dmosip.kernel.keymanager.hsm.keystore-pass=1234 \
    -Dmosip.kernel.keymanager.autogen.appids.list=ROOT,KERNEL:SIGN,ID_REPO,KERNEL:IDENTITY_CACHE,IDA \
    -Dmosip.kernel.keymanager.autogen.basekeys.list=IDA:PUBLIC_KEY \
    -Dzkcrypto.random.key.generate.count=1000 \
    -Dmosip.auth.filter_disable=true \
    -Dspring.security.csrf.enabled=false \
    -Dspring.cloud.loadbalancer.enabled=false \
    -jar "$KG_JAR"
  log "keys-generator finished"
}

start_keymanager() {
  local jar
  jar="$(ls ./kernel-keymanager-service*.jar 2>/dev/null | head -n 1 || true)"
  if [[ -z "$jar" ]]; then
    log "ERROR: kernel-keymanager-service jar not found in image cwd"
    exit 1
  fi
  log "Starting keymanager: $jar"
  java -XX:+UseZGC -Dfile.encoding=UTF-8 \
    -Dspring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration \
    -Dspring.cloud.config.uri=http://config-server:51000/config \
    -Dspring.cloud.config.label="${LABEL}" \
    -Dspring.profiles.active=default \
    -Dspring.cloud.config.name=keymanager,kernel \
    -Dmosip.kernel.database.hostname=postgres \
    -Dmosip.kernel.database.port=5432 \
    -Ddb.dbuser.password=mosip123 \
    -Dkeymanager_database_url=jdbc:postgresql://postgres:5432/mosip_keymgr \
    -Dkeymanager_database_username=keymgruser \
    -Dkeymanager_database_password=mosip123 \
    -Dmosip.kernel.keymanager.hsm.keystore-type=PKCS12 \
    -Dmosip.kernel.keymanager.hsm.config-path="${CONFIG_DIR}/mosip-idrepo-ks.p12" \
    -Dmosip.kernel.keymanager.hsm.keystore-pass=1234 \
    -Dmosip.kernel.authmanager.url=http://mock-service:8082 \
    -Dauth.server.validate.url=http://mock-service:8082/v1/authmanager/authorize/admin/validateToken \
    -Dauth.server.admin.validate.url=http://mock-service:8082/v1/authmanager/authorize/admin/validateToken \
    -Dauth.server.admin.offline.comp.token.validate=false \
    -Dmosip.auth.filter_disable=true \
    -Dmosip.admin.client.secret=local-dev-secret \
    -Dmosip.iam.adapter.appid=admin \
    -Dmosip.iam.adapter.clientid=mosip-admin-client \
    -Dmosip.iam.adapter.clientsecret=local-dev-secret \
    -Dmosip.authmanager.client-token-endpoint=http://mock-service:8082/v1/authmanager/authenticate/clientidsecretkey \
    -Dmosip.iam.adapter.self-token-renewal-enable=false \
    -Dmosip.security.csrf-enable=false \
    -Dspring.security.csrf.enabled=false \
    -Dkeycloak.internal.url=http://mock-service:8082 \
    -Dkeycloak.external.url=http://mock-service:8082 \
    -Dauth.server.admin.issuer.internal.uri=http://mock-service:8082/auth/realms/ \
    -Dauth.server.admin.issuer.uri=http://mock-service:8082/auth/realms/ \
    -Dauth.server.admin.audience.claim.validate=false \
    -Dauth.server.admin.issuer.domain.validate=false \
    -Dspring.cloud.loadbalancer.enabled=false \
    -jar "$jar" &
  KM_PID=$!
}

wait_health() {
  log "Waiting for keymanager health..."
  local i
  for i in $(seq 1 90); do
    if curl -sf "${KM_URL}/v1/keymanager/actuator/health" >/dev/null 2>&1; then
      log "keymanager healthy"
      return 0
    fi
    if ! kill -0 "$KM_PID" 2>/dev/null; then
      log "ERROR: keymanager process exited before healthy"
      wait "$KM_PID" || true
      exit 1
    fi
    sleep 2
  done
  log "ERROR: keymanager health timeout"
  exit 1
}

gen_master() {
  local app_id="$1"
  local ref_id="${2:-}"
  local now
  now="$(date -u +%Y-%m-%dT%H:%M:%S.000Z)"
  log "generateMasterKey applicationId=${app_id} referenceId=${ref_id:-empty}"
  curl -sS -o /tmp/km-gen.out -w "HTTP %{http_code}\n" -X POST \
    "${KM_URL}/v1/keymanager/generateMasterKey/certificate" \
    -H "Content-Type: application/json" \
    -d "{\"id\":\"mosip.keymanager.generate\",\"version\":\"v1\",\"requesttime\":\"${now}\",\"request\":{\"applicationId\":\"${app_id}\",\"createNewCertifcate\":true,\"referenceId\":\"${ref_id}\",\"commonName\":\"www.mosip.io\",\"organizationUnit\":\"MOSIP-TECH-CENTER\",\"organization\":\"IITB\",\"location\":\"BANGALORE\",\"state\":\"KA\",\"country\":\"IN\"}}" \
    || true
  cat /tmp/km-gen.out; echo
}

invoke_master_keys() {
  log "Invoking master-key APIs (former keymanager-init)..."
  gen_master ROOT ""
  gen_master ID_REPO ""
  gen_master ID_REPO identity_data
  gen_master IDA ""
  log "Ensuring IDA:PUBLIC_KEY via getCertificate..."
  curl -sS -o /tmp/km-ida-pub.out -w "HTTP %{http_code}\n" \
    "${KM_URL}/v1/keymanager/getCertificate?applicationId=IDA&referenceId=PUBLIC_KEY" || true
  cat /tmp/km-ida-pub.out; echo
}

verify_static_seed() {
  log "Verifying static seed decrypt path (ID_REPO identity_data certificate)..."
  local code
  code="$(curl -sS -o /tmp/km-idrepo-cert.out -w "%{http_code}" \
    "${KM_URL}/v1/keymanager/getCertificate?applicationId=ID_REPO&referenceId=identity_data" || echo 000)"
  log "getCertificate ID_REPO/identity_data HTTP ${code}"
  head -c 400 /tmp/km-idrepo-cert.out; echo
  if [[ "$code" != "200" ]]; then
    log "ERROR: static seed / PKCS12 mismatch — keep keys/mosip-idrepo-ks.p12 (password 1234) that matches init.sql keymgr seed"
    log "       Or set KM_STATIC_SEED=false and wipe DB after aligning the p12 to regenerate via keys-generator."
    exit 1
  fi
  # Dump seed may omit IDA; create if missing (writes into the same PKCS12).
  log "Ensuring IDA master + PUBLIC_KEY exist (no-op if already present)..."
  gen_master IDA ""
  curl -sS -o /tmp/km-ida-pub.out -w "HTTP %{http_code}\n" \
    "${KM_URL}/v1/keymanager/getCertificate?applicationId=IDA&referenceId=PUBLIC_KEY" || true
  cat /tmp/km-ida-pub.out; echo
}

rm -f "$READY_MARKER"
if [[ "${KM_STATIC_SEED}" == "true" || "${KM_STATIC_SEED}" == "1" ]]; then
  log "KM_STATIC_SEED=true — skipping keys-generator and generateMasterKey (SQL seed from init.sql)"
  if [[ ! -f "${CONFIG_DIR}/mosip-idrepo-ks.p12" ]]; then
    log "ERROR: missing ${CONFIG_DIR}/mosip-idrepo-ks.p12 — static seed requires the matching PKCS12"
    exit 1
  fi
else
  run_keys_generator
fi
start_keymanager
wait_health
if [[ "${KM_STATIC_SEED}" == "true" || "${KM_STATIC_SEED}" == "1" ]]; then
  verify_static_seed
else
  invoke_master_keys
fi
touch "$READY_MARKER"
log "ready marker written: $READY_MARKER"
wait "$KM_PID"
