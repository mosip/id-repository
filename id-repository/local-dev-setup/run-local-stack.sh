#!/usr/bin/env bash
# run-local-stack.sh — one entrypoint for Mac, Linux, and Windows (Git Bash / WSL).
#
# Starts / manages Docker deps (deps/) for local id-repository.
# id-repository-service runs on the host via run-idrepo-local.sh / .bat
#
# Usage (from anywhere):
#   ./run-local-stack.sh              # prep + start Docker deps
#   ./run-local-stack.sh up           # same as default
#   ./run-local-stack.sh restart      # ordered recreate of Docker deps
#   ./run-local-stack.sh down         # stop containers (keep volumes)
#   ./run-local-stack.sh wipe         # down -v then up (re-runs init.sql)
#   ./run-local-stack.sh prep         # PKCS12 (+ keys-generator.jar only if no static seed)
#   ./run-local-stack.sh build        # Maven package id-repository-service
#   ./run-local-stack.sh run          # rebuild + start id-repo on host (run-idrepo-local.sh)
#   ./run-local-stack.sh status       # docker compose ps
#   ./run-local-stack.sh smoke        # health / UIN / idschema curls
#   ./run-local-stack.sh logs [svc]   # follow logs (default: keymanager-service)
#   ./run-local-stack.sh help
#
# Windows: run with Git Bash or WSL:
#   bash run-local-stack.sh up
#   run-idrepo-local.bat

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_DIR="$SCRIPT_DIR/deps"
MAVEN_PARENT="$(cd "$SCRIPT_DIR/.." && pwd)"
KEYS_DIR="$SCRIPT_DIR/keys"
P12_FILE="$KEYS_DIR/mosip-idrepo-ks.p12"
P12_PASS="1234"
ADDITIONAL_JARS_DIR="$COMPOSE_DIR/additional_jars"
IDOBJECTVALIDATOR_JAR="$ADDITIONAL_JARS_DIR/kernel-ref-idobjectvalidator.jar"
IDOBJECTVALIDATOR_VERSION="1.4.1-SNAPSHOT"
IDOBJECTVALIDATOR_GIT_URL="${IDOBJECTVALIDATOR_GIT_URL:-https://github.com/mosip/mosip-ref-impl.git}"
IDOBJECTVALIDATOR_GIT_REF="${IDOBJECTVALIDATOR_GIT_REF:-issue-1633}"
KM_BOOTSTRAP_DIR="$COMPOSE_DIR/keymanager"
KEYS_GENERATOR_JAR="$KM_BOOTSTRAP_DIR/keys-generator.jar"
KEYS_GENERATOR_IMAGE="${KEYS_GENERATOR_IMAGE:-mosipid/keys-generator:1.3.0}"

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

# Windows JDKs under /mnt/c only ship java.exe (no bin/java).
jdk_has_launcher() {
  local home="$1"
  [[ -n "$home" ]] && { [[ -x "${home}/bin/java" ]] || [[ -x "${home}/bin/java.exe" ]]; }
}

# Resolve a Windows JDK 21 path as seen from WSL (/mnt/c/...).
discover_windows_jdk_unix() {
  local win_jh="" unix_jh="" d
  if command -v cmd.exe >/dev/null 2>&1; then
    win_jh="$(cmd.exe /c 'echo %JAVA_HOME%' 2>/dev/null | tr -d '\r' || true)"
  fi
  if [[ -n "$win_jh" && "$win_jh" != "%JAVA_HOME%" ]]; then
    if command -v wslpath >/dev/null 2>&1; then
      unix_jh="$(wslpath -u "$win_jh" 2>/dev/null || true)"
    elif command -v cygpath >/dev/null 2>&1; then
      unix_jh="$(cygpath -u "$win_jh" 2>/dev/null || true)"
    fi
  fi
  if jdk_has_launcher "$unix_jh"; then
    printf '%s\n' "$unix_jh"
    return 0
  fi
  if [[ -d "/mnt/c/Program Files/Java" ]]; then
    while IFS= read -r d; do
      if jdk_has_launcher "$d"; then
        printf '%s\n' "$d"
        return 0
      fi
    done < <(find "/mnt/c/Program Files/Java" -maxdepth 1 -type d \( -name 'jdk-21*' -o -name 'graalvm-jdk-21*' \) 2>/dev/null | sort -r)
  fi
  return 1
}

# Maven under WSL bash + Windows mvn + java.exe breaks (Unix classpath vs PE java).
# Always build through a Windows .cmd when we are on WSL (avoids quote mangling).
cmd_build_via_windows_cmd() {
  local jdk_unix jdk_win proj_win bat
  jdk_unix="$(discover_windows_jdk_unix)" \
    || die "No Windows JDK 21 found under Program Files\\Java. Install JDK 21 or use run-idrepo-local.bat from cmd."
  jdk_win="$(wslpath -w "$jdk_unix")"
  proj_win="$(wslpath -w "$MAVEN_PARENT")"
  bat="$SCRIPT_DIR/.wsl-mvn-build.cmd"
  # CRLF .cmd so cmd.exe parses it reliably.
  printf '%s\r\n' \
    '@echo off' \
    "set \"JAVA_HOME=${jdk_win}\"" \
    "cd /d \"${proj_win}\"" \
    'if errorlevel 1 exit /b 1' \
    'echo [INFO] JAVA_HOME=%JAVA_HOME%' \
    'echo [INFO] Building id-repository-service (skip tests)...' \
    'call mvn -pl id-repository-service -am package -DskipTests -Dgpg.skip=true' \
    'exit /b %ERRORLEVEL%' >"$bat"
  info "WSL detected — building via Windows cmd ($(wslpath -w "$bat"))"
  cmd.exe /c "$(wslpath -w "$bat")" \
    || { rm -f "$bat"; die "Maven build failed. From Windows cmd use: run-idrepo-local.bat"; }
  rm -f "$bat"
}

# When `bash` is WSL, Windows JAVA_HOME is often unset. For native (Linux) mvn only.
ensure_java_home() {
  if [[ -n "${JAVA_HOME:-}" ]] && jdk_has_launcher "$JAVA_HOME"; then
    # Prefer real bin/java; never point Maven at a Windows JDK path from WSL bash.
    if [[ -x "${JAVA_HOME}/bin/java" ]]; then
      export PATH="${JAVA_HOME}/bin:${PATH}"
      return 0
    fi
  fi
  if [[ "$OS_KIND" != "wsl" && "$OS_KIND" != "windows" ]]; then
    return 0
  fi
  # Do not export /mnt/c/... as JAVA_HOME for bash Maven — use cmd_build_via_windows_cmd instead.
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
  local args=()
  if [[ "${LOW_MEM:-}" == "1" || "${LOW_MEM:-}" == "true" ]]; then
    args+=(-f docker-compose.yml -f docker-compose.low-mem.yml)
    info "LOW_MEM=1 — using docker-compose.low-mem.yml (~3GB container budget)"
  fi
  (cd "$COMPOSE_DIR" && docker compose "${args[@]}" "$@")
}

# ---------------------------------------------------------------------------
# Prep: keystore, idobjectvalidator loader jar, local IAM
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

ensure_idobjectvalidator() {
  mkdir -p "$ADDITIONAL_JARS_DIR"
  if [[ -f "$IDOBJECTVALIDATOR_JAR" ]]; then
    info "Id object validator already present: $IDOBJECTVALIDATOR_JAR"
    return 0
  fi

  need_cmd git
  need_cmd mvn
  ensure_java_home
  local work built src
  work="${TMPDIR:-/tmp}/mosip-ref-impl-idobjectvalidator"
  info "Building kernel-ref-idobjectvalidator ${IDOBJECTVALIDATOR_VERSION} from ${IDOBJECTVALIDATOR_GIT_URL} (${IDOBJECTVALIDATOR_GIT_REF}) ..."
  rm -rf "$work"
  git clone --depth 1 --branch "$IDOBJECTVALIDATOR_GIT_REF" --filter=blob:none --sparse \
    "$IDOBJECTVALIDATOR_GIT_URL" "$work"
  git -C "$work" sparse-checkout set kernel
  (cd "$work/kernel" && mvn -pl kernel-ref-idobjectvalidator -am package -DskipTests -Dgpg.skip=true -Dmaven.javadoc.skip=true)
  built="$work/kernel/kernel-ref-idobjectvalidator/target/kernel-ref-idobjectvalidator-${IDOBJECTVALIDATOR_VERSION}.jar"
  if [[ -f "$built" ]]; then
    cp "$built" "$IDOBJECTVALIDATOR_JAR"
    info "Wrote $IDOBJECTVALIDATOR_JAR"
    return 0
  fi

  src="$(m2_repo)/io/mosip/kernel/kernel-ref-idobjectvalidator/${IDOBJECTVALIDATOR_VERSION}/kernel-ref-idobjectvalidator-${IDOBJECTVALIDATOR_VERSION}.jar"
  if [[ -f "$src" ]]; then
    warn "Git build did not produce $built — using Maven cache $src"
    cp "$src" "$IDOBJECTVALIDATOR_JAR"
    info "Copied kernel-ref-idobjectvalidator.jar → $IDOBJECTVALIDATOR_JAR"
    return 0
  fi
  die "Could not obtain kernel-ref-idobjectvalidator. See deps/additional_jars/README.md"
}

ensure_keys_generator_jar() {
  mkdir -p "$KM_BOOTSTRAP_DIR"
  if [[ -f "$KEYS_GENERATOR_JAR" ]]; then
    info "keys-generator.jar already present: $KEYS_GENERATOR_JAR"
    return 0
  fi
  need_cmd docker
  info "Copying keys-generator.jar from $KEYS_GENERATOR_IMAGE ..."
  docker pull "$KEYS_GENERATOR_IMAGE" >/dev/null
  local cid
  cid="$(docker create "$KEYS_GENERATOR_IMAGE")"
  docker cp "$cid:/home/mosip/keys-generator.jar" "$KEYS_GENERATOR_JAR"
  docker rm "$cid" >/dev/null
  [[ -f "$KEYS_GENERATOR_JAR" ]] || die "Failed to extract keys-generator.jar"
  info "Wrote $KEYS_GENERATOR_JAR"
}

cmd_prep() {
  log "========================================"
  log " Local stack prep ($OS_KIND)"
  log "========================================"
  ensure_p12
  local static_seed="$COMPOSE_DIR/init.sql"
  if grep -q "TRUNCATE TABLE keymgr.key_alias" "$static_seed" 2>/dev/null; then
    info "Static keymgr seed present in init.sql — keys-generator.jar not required"
    info "Keep keys/mosip-idrepo-ks.p12 in sync with that seed (wipe must NOT recreate a new PKCS12)"
    if [[ ! -f "$P12_FILE" ]]; then
      warn "PKCS12 missing — created empty bootstrap keystore; crypto will fail until you restore the matching p12"
    fi
  else
    ensure_keys_generator_jar
  fi
  ensure_idobjectvalidator
  info "WireMock stubs (IAM / BioSDK / idgenerator) are static in mappings/id-repository.json — no extension jar."
  info "Prep complete."
}

# ---------------------------------------------------------------------------
# Build
# ---------------------------------------------------------------------------
cmd_build() {
  # WSL bash + Windows Maven/JDK cannot share a classpath (java.exe + /mnt/c paths).
  if [[ "$OS_KIND" == "wsl" ]] && command -v cmd.exe >/dev/null 2>&1; then
    cmd_build_via_windows_cmd
  else
    need_cmd mvn
    ensure_java_home
    if ! command -v java >/dev/null 2>&1; then
      die "java not found. On Windows cmd prefer: run-idrepo-local.bat. Or install JDK 21 and set JAVA_HOME."
    fi
    info "Building id-repository-service (skip tests)..."
    (cd "$MAVEN_PARENT" && mvn -pl id-repository-service -am package -DskipTests -Dgpg.skip=true) \
      || die "Maven build failed"
  fi
  local jar
  jar="$(ls -1 "$MAVEN_PARENT"/id-repository-service/target/id-repository-service-*.jar 2>/dev/null \
    | grep -vE 'sources|javadoc|tests' | head -n 1 || true)"
  [[ -n "$jar" ]] || die "Build finished but jar not found under id-repository-service/target/"
  info "Jar ready: $jar"
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
  info "Starting Docker deps (docker compose up -d --remove-orphans)..."
  compose up -d --remove-orphans
  compose ps
  log ""
  info "Deps ready. Start ID-Repository on the host:"
  info "  ./run-idrepo-local.sh          # rebuild + start (Mac/Linux/Git Bash)"
  info "  run-idrepo-local.bat           # rebuild + start (Windows cmd)"
  info "  $0 run                         # same as run-idrepo-local.sh"
  info "Then: $0 smoke"
  info "API:  http://localhost:8090/actuator/health"
}

cmd_restart() {
  check_docker
  [[ -f "$P12_FILE" ]] || die "Missing $P12_FILE — run: $0 prep (static seed needs matching mosip-idrepo-ks.p12)"

  info "[1/2] Starting postgres + config-server..."
  compose up -d --remove-orphans postgres config-server

  info "[2/2] Starting keymanager (static-seed bootstrap) + remaining deps..."
  compose up -d --remove-orphans \
    mock-service minio minio-init \
    keymanager-service

  compose ps
  info "Deps ready. Restart the host app: ./run-idrepo-local.sh"
  info "Health: curl http://localhost:8090/actuator/health"
}

cmd_run() {
  local runner="$SCRIPT_DIR/run-idrepo-local.sh"
  [[ -f "$runner" ]] || die "Missing $runner"
  chmod +x "$runner" 2>/dev/null || true
  exec "$runner" "$@"
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
  local svc="${1:-keymanager-service}"
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
  smoke_one "ridgenerator RID"    "http://localhost:8082/v1/ridgenerator/generate/rid/10001/10001"
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
  up          Prep + start Docker deps (default)
  up-low-mem  Same as up with docker-compose.low-mem.yml (~3GB deps)
  restart     Ordered recreate of Docker deps
  restart-low-mem  restart with low-mem overlay
  run         Rebuild jar then start id-repository on the host (run-idrepo-local.sh)
  down        docker compose down
  wipe        docker compose down -v && up
  wipe-low-mem  wipe then up with low-mem overlay
  prep        Create PKCS12 (keys-generator.jar only if init.sql has no static seed)
  build       mvn package id-repository-service (-DskipTests)
  status      docker compose ps -a
  smoke       HTTP health / UIN / idschema checks (needs host app running)
  logs        Follow logs (default: keymanager-service)
  help        Show this help

Env:
  LOW_MEM=1   Merge docker-compose.low-mem.yml (heap + mem_limit caps)

Host app (Windows cmd):  run-idrepo-local.bat
Windows (Git Bash):      bash $0 up   then   bash $0 run
Docs:    $SCRIPT_DIR/README.md
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
    up-low-mem|low-mem)
      LOW_MEM=1
      cmd_up
      ;;
    restart-low-mem)
      LOW_MEM=1
      cmd_restart
      ;;
    wipe-low-mem|reset-low-mem)
      LOW_MEM=1
      cmd_wipe
      ;;
    restart)      cmd_restart ;;
    run|app)      cmd_run "$@" ;;
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
