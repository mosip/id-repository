#!/usr/bin/env bash
# Bring local Docker deps up (healthy order). Project: idrepo-local.
# id-repository-service runs on the host — after this, start:
#   ../run-idrepo-local.sh

set -euo pipefail
cd "$(dirname "$0")"

echo "[1/2] Starting postgres + config-server..."
docker compose up -d --remove-orphans \
  postgres config-server

echo "[2/2] Starting keymanager (bootstrap) + remaining deps..."
docker compose up -d --remove-orphans \
  mock-service minio minio-init \
  keymanager-service

echo
echo "Deps are up. Start ID-Repository on the host:"
echo "  ../run-idrepo-local.sh"
echo "  (rebuilds then starts; use --no-build to skip Maven)"
echo "Health: http://localhost:8090/actuator/health"
docker compose ps
