#!/usr/bin/env bash
# Host JVM + local Maven/Docker helpers.
#
# Prerequisites:
#   Docker deps: ./run-local-stack.sh up   (or: $0 up)
#
# Usage (from local-dev-setup):
#   ./run-idrepo-local.sh                 rebuild jar, then start :8090
#   ./run-idrepo-local.sh run             same as default
#   ./run-idrepo-local.sh start           start existing jar (no Maven)
#   ./run-idrepo-local.sh rebuild         mvn package only (do not start)
#   ./run-idrepo-local.sh stop            stop the host JVM
#   ./run-idrepo-local.sh smoke           health / UIN / idschema curls
#   ./run-idrepo-local.sh test            unit tests (mvn test)
#   ./run-idrepo-local.sh sonar           unit tests + jacoco + sonar profile
#   ./run-idrepo-local.sh wipe            clear DB (compose down -v) then start deps
#   ./run-idrepo-local.sh wipe-run        wipe, then rebuild + start
#   ./run-idrepo-local.sh --no-build      alias for start

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MAVEN_PARENT="$(cd "$SCRIPT_DIR/.." && pwd)"
TARGET_DIR="$MAVEN_PARENT/id-repository-service/target"
LOADER_PATH="$SCRIPT_DIR/deps/additional_jars"
IDOBJECTVALIDATOR_JAR="$LOADER_PATH/kernel-ref-idobjectvalidator.jar"
CONFIG_URI="${SPRING_CLOUD_CONFIG_URI:-http://localhost:51100/config}"
JAVA_OPTS="${JAVA_OPTS:--Xms512m -Xmx1536m}"
STACK="$SCRIPT_DIR/run-local-stack.sh"

die() { echo "ERROR: $*" >&2; exit 1; }
info() { echo "$*"; }

print_help() {
  cat <<EOF

$0 [command]

App (host JVM on http://localhost:8090):
  run              Rebuild jar, then start  (default)
  start            Start existing jar, no Maven
  rebuild          mvn clean package, skip tests  (do not start)
  stop             Stop the host JVM

Docker deps (Postgres, config-server, WireMock, MinIO, keymanager):
  up               Start deps, keep DB volumes
  wipe             Stop app, clear DB (compose down -v), start deps  - PKCS12 kept
  wipe-run         wipe, then rebuild + start app

Checks:
  smoke            GET health / UIN / RID / idschema
  test             mvn test on id-repository-service
  sonar            mvn verify -Psonar  (needs SONAR_TOKEN for SonarCloud)

  help             Show this list and exit

Aliases: --build=run  --no-build=start  build=rebuild  cleardb / clear-db / reset=wipe  -h / --help=help

Wipe and rerun api-test:
  1) $0 wipe-run      (wait for the app to start)
  2) ../../api-test/run-local-smoke.sh smokeAndRegression

EOF
}

stop_app() {
  info "Stopping host id-repository-service if it is running..."
  local pids
  pids="$(pgrep -f 'id-repository-service.*\.jar' || true)"
  if [[ -n "$pids" ]]; then
    # shellcheck disable=SC2086
    kill $pids 2>/dev/null || true
    sleep 1
    pids="$(pgrep -f 'id-repository-service.*\.jar' || true)"
    if [[ -n "$pids" ]]; then
      # shellcheck disable=SC2086
      kill -9 $pids 2>/dev/null || true
    fi
  fi
}

find_jar() {
  JAR="$(ls -1 "$TARGET_DIR"/id-repository-service-*.jar 2>/dev/null \
    | grep -vE 'sources|javadoc|tests' | head -n 1 || true)"
  [[ -n "${JAR:-}" ]] || die "jar not found under $TARGET_DIR
Run: $0 rebuild"
}

mvn_package() {
  info "Building id-repository-service..."
  # Full package lifecycle so git-commit-id (validate) + spring-boot:build-info run.
  (cd "$MAVEN_PARENT" && mvn -pl id-repository-service clean package -Dmaven.test.skip=true -Dgpg.skip=true -Dmaven.javadoc.skip=true)
}

cmd_rebuild() {
  mvn_package
  find_jar
  info "Jar ready: $JAR"
}

cmd_test() {
  info "Running unit tests..."
  (cd "$MAVEN_PARENT" && mvn -pl id-repository-service -am test -Dgpg.skip=true -Dmaven.javadoc.skip=true)
  info "Unit tests passed."
}

cmd_sonar() {
  if [[ -z "${SONAR_TOKEN:-}" ]]; then
    info "NOTE: SONAR_TOKEN is not set. SonarCloud upload will fail unless you set it."
    info "      Local jacoco report still runs under target/site/jacoco"
  fi
  info "Running verify + sonar profile..."
  (cd "$MAVEN_PARENT" && mvn -pl id-repository-service -am verify -Psonar -Dgpg.skip=true -Dmaven.javadoc.skip=true)
  info "Sonar step finished."
}

cmd_run() {
  local do_build="${1:-1}"
  stop_app
  if [[ "$do_build" -eq 1 ]]; then
    mvn_package
  fi
  find_jar
  [[ -f "$IDOBJECTVALIDATOR_JAR" ]] || die "missing $IDOBJECTVALIDATOR_JAR
kernel-ref-idobjectvalidator is loaded from additional_jars (not Maven). Run:
  $STACK prep
See deps/additional_jars/README.md"

  info "Waiting for config-server at $CONFIG_URI ..."
  ready=0
  for i in $(seq 1 60); do
    if curl -sf "$CONFIG_URI/id-repository/default" >/dev/null; then
      ready=1
      break
    fi
    sleep 2
  done
  [[ "$ready" -eq 1 ]] || die "config-server not reachable. Start deps first:
  $0 up
  or: $STACK up"

  info "config-server is ready."

  LOG_DIR="$SCRIPT_DIR/.local/logs"
  mkdir -p "$LOG_DIR"

  info ""
  info "Starting id-repository-service on http://localhost:8090"
  info "Jar: $JAR"
  info "loader.path: $IDOBJECTVALIDATOR_JAR"
  info "Logs: $LOG_DIR"
  info "Stop with Ctrl+C  or:  $0 stop"
  info ""

  # shellcheck disable=SC2086
  exec java $JAVA_OPTS \
    -Dfile.encoding=UTF-8 \
    --add-opens java.base/java.lang=ALL-UNNAMED \
    --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
    --add-opens java.base/java.io=ALL-UNNAMED \
    -Dloader.path="$IDOBJECTVALIDATOR_JAR" \
    -Dspring.cloud.config.uri="$CONFIG_URI" \
    -Dspring.config.import="configserver:$CONFIG_URI" \
    -Dspring.cloud.config.label="${SPRING_CLOUD_CONFIG_LABEL:-develop}" \
    -Dspring.profiles.active=default,local \
    -Dspring.cloud.config.override-none=true \
    -Dspring.cloud.loadbalancer.enabled=false \
    -Dserver.port=8090 \
    -Dlogging.file.path="$LOG_DIR" \
    -jar "$JAR"
}

CMD="${1:-run}"
case "$CMD" in
  -h|--help) CMD=help ;;
  --build) CMD=run ;;
  --no-build) CMD=start ;;
  build) CMD=rebuild ;;
  cleardb|clear-db|reset) CMD=wipe ;;
esac

print_help
[[ "$CMD" == "help" ]] && exit 0
echo "Selected: $CMD"
echo

case "$CMD" in
  run)        cmd_run 1 ;;
  start)      cmd_run 0 ;;
  rebuild)    cmd_rebuild ;;
  stop)       stop_app ;;
  smoke)      exec "$STACK" smoke ;;
  test)       cmd_test ;;
  sonar)      cmd_sonar ;;
  up)         exec "$STACK" up ;;
  wipe)       stop_app; exec "$STACK" wipe ;;
  wipe-run)   stop_app; "$STACK" wipe; cmd_run 1 ;;
  *)
    echo "ERROR: unknown command \"$CMD\". Use one of the commands above." >&2
    exit 1
    ;;
esac
