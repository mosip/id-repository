#!/bin/sh
# Bootstrap MOSIP keymanager master keys for local docker-compose (ROOT + ID_REPO).
set -e
KM_URL="${KEYMANAGER_URL:-http://keymanager-service:8088}"
NOW="$(date -u +%Y-%m-%dT%H:%M:%S.000Z)"

gen_key() {
  APP_ID="$1"
  REF_ID="${2:-}"
  echo "Generating master key for applicationId=${APP_ID} referenceId=${REF_ID:-<empty>}"
  HTTP_CODE=$(curl -s -o /tmp/km-bootstrap.out -w "%{http_code}" -X POST \
    "${KM_URL}/v1/keymanager/generateMasterKey/certificate" \
    -H "Content-Type: application/json" \
    -d "{
      \"id\": \"mosip.keymanager.generate\",
      \"version\": \"v1\",
      \"requesttime\": \"${NOW}\",
      \"request\": {
        \"applicationId\": \"${APP_ID}\",
        \"createNewCertifcate\": true,
        \"referenceId\": \"${REF_ID}\",
        \"commonName\": \"www.mosip.io\",
        \"organizationUnit\": \"MOSIP-TECH-CENTER\",
        \"organization\": \"IITB\",
        \"location\": \"BANGALORE\",
        \"state\": \"KA\",
        \"country\": \"IN\"
      }
    }")
  echo "  HTTP ${HTTP_CODE}"
  cat /tmp/km-bootstrap.out
  echo
  case "${HTTP_CODE}" in
    200) ;;
    *) echo "WARN: unexpected status for ${APP_ID}" >&2 ;;
  esac
}

# ROOT domain cert first, then ID_REPO app + identity_data ref (id-repo encrypt referenceId)
gen_key ROOT ""
gen_key ID_REPO ""
gen_key ID_REPO "identity_data"
echo "Keymanager bootstrap finished."
