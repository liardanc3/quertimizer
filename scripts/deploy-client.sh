#!/usr/bin/env bash
set -Eeuo pipefail

SERVER_USER="${SERVER_USER:-quertimizer}"
SERVER_HOST="${SERVER_HOST:-quertimizer.com}"

APP_BASE="${APP_BASE:-/home/quertimizer/apps}"
CLIENT_BASE="${CLIENT_BASE:-$APP_BASE/client}"

LOCAL_CLIENT_DIR="${LOCAL_CLIENT_DIR:-client}"
LOCAL_DIST_DIR="${LOCAL_DIST_DIR:-$LOCAL_CLIENT_DIR/dist}"

RELEASE="$(date +%Y_%m_%d_%H_%M_%S)"

echo "[client] build start"
npm ci --prefix "$LOCAL_CLIENT_DIR"
npm run build --prefix "$LOCAL_CLIENT_DIR"

if [[ ! -d "$LOCAL_DIST_DIR" ]]; then
  echo "[client] dist directory not found: $LOCAL_DIST_DIR"
  exit 1
fi

ssh "${SERVER_USER}@${SERVER_HOST}" "
  set -Eeuo pipefail
  mkdir -p '$CLIENT_BASE/releases/$RELEASE'
"

scp -r "$LOCAL_DIST_DIR"/* "${SERVER_USER}@${SERVER_HOST}:$CLIENT_BASE/releases/$RELEASE/"

ssh "${SERVER_USER}@${SERVER_HOST}" "
  set -Eeuo pipefail
  ln -sfn '$CLIENT_BASE/releases/$RELEASE' '$CLIENT_BASE/current'
  sudo nginx -t
  sudo systemctl reload nginx
"

echo '[client] done'