#!/usr/bin/env bash
# run-local-stack.sh — one entrypoint for Mac, Linux, and Windows (Git Bash / WSL).
#
# Starts / manages the id-repository local docker-compose stack.
#
# Usage (from anywhere):
#   ./run-local-stack.sh              # prep missing files + docker compose up -d
#   ./run-local-stack.sh up           # same as default
#   ./run-local-stack.sh restart      # deps healthy, then recreate id-repository-service
#   ./run-local-stack.sh down         # stop containers (keep volumes)
#   ./run-local-stack.sh wipe         # down -v then up (re-runs init.sql)
#   ./run-local-stack.sh prep         # PKCS12 + auth-adapter + BioSDK zip only
#   ./run-local-stack.sh build        # Maven package id-repository-service
#   ./run-local-stack.sh status       # docker compose ps
#   ./run-local-stack.sh smoke        # health / UIN / idschema curls
#   ./run-local-stack.sh logs [svc]   # follow logs (default: id-repository-service)
#   ./run-local-stack.sh help
#
# Windows: run with Git Bash or WSL:
#   bash run-local-stack.sh up

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_DIR="$SCRIPT_DIR/docker-compose"
MAVEN_PARENT="$(cd "$SCRIPT_DIR/.." && pwd)"
KEYS_DIR="$SCRIPT_DIR/keys"
P12_FILE="$KEYS_DIR/mosip-idrepo-ks.p12"
P12_PASS="qwerty@1234"
ADAPTER_DIR="$COMPOSE_DIR/additional_jars"
ADAPTER_JAR="$ADAPTER_DIR/kernel-auth-adapter.jar"
ADAPTER_VERSION="1.3.1"
MOCK_SDK_ZIP="$COMPOSE_DIR/wiremock/__files/mock-sdk.zip"
BIOSDK_JAR_URL="https://repo1.maven.org/maven2/io/mosip/mock/sdk/mock-sdk/1.3.0-beta.1/mock-sdk-1.3.0-beta.1-jar-with-dependencies.jar"
BIOSDK_JAR_NAME="mock-sdk-1.3.0-beta.1-jar-with-dependencies.jar"

# ---------------------------------------------------------------------------
# OS detection (informational + path helpers)
# ---------------------------------------------------------------------------
detect_os() {
  case "$(uname -s 2>/dev/null || echo unknown)" in
    Darwin*)  echo "macos" ;;
    Linux*)
      if grep -qi microsoft /proc/version 2>/dev/null; then
        echo "wsl"
      else
        echo "linux"
      fi
      ;;
    MINGW*|MSYS*|CYGWIN*) echo "windows" ;;
    *) echo "unknown" ;;
  esac
}

OS_KIND="$(detect_os)"

m2_repo() {
  if [[ -n "${M2_REPO:-}" ]]; then
    echo "$M2_REPO"
    return
  fi
  # Git Bash on Windows: prefer Windows user profile Maven cache
  if [[ "$OS_KIND" == "windows" && -n "${USERPROFILE:-}" ]]; then
    local win_m2
    win_m2="$(cygpath -u "$USERPROFILE/.m2/repository" 2>/dev/null || echo "")"
    if [[ -n "$win_m2" && -d "$win_m2" ]]; then
      echo "$win_m2"
      return
    fi
  fi
  echo "${HOME}/.m2/repository"
}

log()  { printf '%s\n' "$*"; }
info() { printf '[INFO] %s\n' "$*"; }
warn() { printf '[WARN] %s\n' "$*" >&2; }
die()  { printf '[ERROR] %s\n' "$*" >&2; exit 1; }

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "Required command not found: $1"
}

# ---------------------------------------------------------------------------
# Prerequisites
# ---------------------------------------------------------------------------
check_docker() {
  need_cmd docker
  if ! docker info >/dev/null 2>&1; then
    die "Docker daemon is not running. Start Docker Desktop (or dockerd) and retry."
  fi
  if ! docker compose version >/dev/null 2>&1; then
    die "Docker Compose V2 required (docker compose). Update Docker Desktop / install compose plugin."
  fi
}

compose() {
  (cd "$COMPOSE_DIR" && docker compose "$@")
}

# ---------------------------------------------------------------------------
# Prep: keystore, auth adapter, BioSDK zip
# ---------------------------------------------------------------------------
ensure_p12() {
  mkdir -p "$KEYS_DIR"
  if [[ -f "$P12_FILE" ]]; then
    info "PKCS12 already present: $P12_FILE"
    return 0
  fi
  need_cmd keytool
  info "Creating PKCS12 keystore (password: $P12_PASS)..."
  keytool -genkeypair -alias bootstrap -keyalg RSA -keysize 2048 -storetype PKCS12 \
    -keystore "$P12_FILE" \
    -storepass "$P12_PASS" -keypass "$P12_PASS" \
    -dname "CN=mosip-idrepo-local" -validity 3650 \
    >/dev/null
  info "Created $P12_FILE"
}

ensure_auth_adapter() {
  mkdir -p "$ADAPTER_DIR"
  if [[ -f "$ADAPTER_JAR" ]]; then
    info "Auth adapter already present: $ADAPTER_JAR"
    return 0
  fi

  local src
  src="$(m2_repo)/io/mosip/kernel/kernel-auth-adapter/${ADAPTER_VERSION}/kernel-auth-adapter-${ADAPTER_VERSION}.jar"
  if [[ ! -f "$src" ]]; then
    warn "Maven cache missing: $src"
    warn "Run: $0 build   (or mvn -pl id-repository-service -am package -DskipTests -Dgpg.skip=true)"
    die "Cannot copy kernel-auth-adapter.jar — build first, then re-run prep/up."
  fi
  cp "$src" "$ADAPTER_JAR"
  info "Copied kernel-auth-adapter.jar → $ADAPTER_JAR"
}

ensure_biosdk_zip() {
  if [[ -f "$MOCK_SDK_ZIP" ]]; then
    info "BioSDK zip already present: $MOCK_SDK_ZIP"
    return 0
  fi
  need_cmd curl
  local temp_dir jar_path zip_path
  temp_dir="$(mktemp -d 2>/dev/null || mktemp -d -t biosdk)"
  jar_path="$temp_dir/$BIOSDK_JAR_NAME"
  zip_path="$temp_dir/mock-sdk.zip"

  cleanup_temp() { rm -rf "$temp_dir"; }
  trap cleanup_temp EXIT

  info "Downloading BioSDK mock JAR (~110 MB)..."
  curl -L --progress-bar "$BIOSDK_JAR_URL" -o "$jar_path"

  info "Packaging mock-sdk.zip..."
  if command -v zip >/dev/null 2>&1; then
    (cd "$temp_dir" && zip -q "mock-sdk.zip" "$BIOSDK_JAR_NAME")
  elif command -v powershell.exe >/dev/null 2>&1; then
    # Git Bash / Windows without zip.exe
    local win_jar win_zip
    win_jar="$(cygpath -w "$jar_path")"
    win_zip="$(cygpath -w "$zip_path")"
    powershell.exe -NoProfile -Command \
      "Compress-Archive -Path '$win_jar' -DestinationPath '$win_zip' -Force"
  elif command -v python3 >/dev/null 2>&1 || command -v python >/dev/null 2>&1; then
    local py
    py="$(command -v python3 || command -v python)"
    "$py" - "$jar_path" "$zip_path" <<'PY'
import sys, zipfile
jar, zpath = sys.argv[1], sys.argv[2]
with zipfile.ZipFile(zpath, "w", zipfile.ZIP_DEFLATED) as zf:
    zf.write(jar, arcname=__import__("os").path.basename(jar))
PY
  else
    die "Need 'zip', PowerShell, or Python to create mock-sdk.zip"
  fi

  mkdir -p "$(dirname "$MOCK_SDK_ZIP")"
  cp "$zip_path" "$MOCK_SDK_ZIP"
  trap - EXIT
  cleanup_temp
  info "Wrote $MOCK_SDK_ZIP"
}

cmd_prep() {
  log "========================================"
  log " Local stack prep ($OS_KIND)"
  log "========================================"
  ensure_p12
  ensure_auth_adapter
  ensure_biosdk_zip
  info "Prep complete."
}

# ---------------------------------------------------------------------------
# Build
# ---------------------------------------------------------------------------
cmd_build() {
  need_cmd mvn
  info "Building id-repository-service (skip tests)..."
  (cd "$MAVEN_PARENT" && mvn -pl id-repository-service -am package -DskipTests -Dgpg.skip=true)
  local jar
  jar="$(ls -1 "$MAVEN_PARENT"/id-repository-service/target/id-repository-service-*.jar 2>/dev/null \
    | grep -vE 'sources|javadoc|tests' | head -n 1 || true)"
  [[ -n "$jar" ]] || die "Build finished but jar not found under id-repository-service/target/"
  info "Jar ready: $jar"
  # Refresh adapter from cache if missing
  if [[ ! -f "$ADAPTER_JAR" ]]; then
    ensure_auth_adapter
  fi
}

ensure_jar() {
  local jar
  jar="$(ls -1 "$MAVEN_PARENT"/id-repository-service/target/id-repository-service-*.jar 2>/dev/null \
    | grep -vE 'sources|javadoc|tests' | head -n 1 || true)"
  if [[ -z "$jar" ]]; then
    warn "Service jar missing under id-repository-service/target/"
    die "Run: $0 build"
  fi
}

# ---------------------------------------------------------------------------
# Docker lifecycle
# ---------------------------------------------------------------------------
cmd_up() {
  check_docker
  cmd_prep
  ensure_jar
  info "Starting full stack (docker compose up -d)..."
  compose up -d
  compose ps
  log ""
  info "Smoke: $0 smoke"
  info "API:   http://localhost:8090/actuator/health"
}

cmd_restart() {
  check_docker
  ensure_jar
  [[ -f "$P12_FILE" ]] || die "Missing $P12_FILE — run: $0 prep"
  [[ -f "$ADAPTER_JAR" ]] || die "Missing $ADAPTER_JAR — run: $0 prep"
  [[ -f "$MOCK_SDK_ZIP" ]] || die "Missing $MOCK_SDK_ZIP — run: $0 prep"

  info "[1/3] Seeding ZK keys (PKCS12 before keymanager load)..."
  compose up -d \
    database config-server keys-generator keys-generator-expiry

  info "[2/3] Starting keymanager + remaining deps..."
  compose up -d \
    uin-generator mock-service minio minio-init biosdk-service \
    keymanager-service keymanager-init datashare-service

  info "[3/3] Recreating id-repository-service..."
  compose up -d --force-recreate id-repository-service
  compose ps
  info "Done. Check: curl http://localhost:8090/actuator/health"
}

cmd_down() {
  check_docker
  info "Stopping stack (volumes kept)..."
  compose down
}

cmd_wipe() {
  check_docker
  warn "Removing containers AND volumes (DB re-init on next up)..."
  compose down -v
  cmd_up
}

cmd_status() {
  check_docker
  compose ps -a
}

cmd_logs() {
  check_docker
  local svc="${1:-id-repository-service}"
  compose logs -f "$svc"
}

cmd_smoke() {
  need_cmd curl
  local ok=0
  smoke_one() {
    local name="$1" url="$2"
    local code
    code="$(curl -s -o /tmp/idrepo-smoke.out -w '%{http_code}' "$url" || echo "000")"
    if [[ "$code" == "200" ]]; then
      info "OK  $name (HTTP $code) — $url"
    else
      warn "FAIL $name (HTTP $code) — $url"
      ok=1
    fi
  }
  smoke_one "id-repo health"      "http://localhost:8090/actuator/health"
  smoke_one "keymanager health"   "http://localhost:8088/v1/keymanager/actuator/health"
  smoke_one "idgenerator UIN"     "http://localhost:8082/v1/idgenerator/uin"
  smoke_one "masterdata idschema" "http://localhost:8082/v1/masterdata/idschema/latest?schemaVersion=0"
  if [[ "$ok" -eq 0 ]]; then
    info "All smoke checks passed."
  else
    die "One or more smoke checks failed. Try: $0 status | $0 logs"
  fi
}

cmd_help() {
  cat <<EOF
id-repository local stack runner (OS: $OS_KIND)

  $0 [command]

Commands:
  up        Prep missing files + docker compose up -d   (default)
  restart   Ordered recreate of id-repository-service
  down      docker compose down
  wipe      docker compose down -v && up
  prep      Create PKCS12, copy auth-adapter, download BioSDK zip
  build     mvn package id-repository-service (-DskipTests)
  status    docker compose ps -a
  smoke     HTTP health / UIN / idschema checks
  logs      Follow logs (optional service name)
  help      Show this help

Windows: use Git Bash or WSL →  bash $0 up
Docs:    $SCRIPT_DIR/LOCAL-DEV-SETUP.md
EOF
}

# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
main() {
  local cmd="${1:-up}"
  shift || true
  case "$cmd" in
    up|start)     cmd_up ;;
    restart)      cmd_restart ;;
    down|stop)    cmd_down ;;
    wipe|reset)   cmd_wipe ;;
    prep|prepare) cmd_prep ;;
    build)        cmd_build ;;
    status|ps)    cmd_status ;;
    smoke|check)  cmd_smoke ;;
    logs)         cmd_logs "${1:-}" ;;
    help|-h|--help) cmd_help ;;
    *)
      die "Unknown command: $cmd (try: $0 help)"
      ;;
  esac
}

main "$@"
