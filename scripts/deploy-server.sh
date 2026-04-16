#!/usr/bin/env bash
set -Eeuo pipefail

SERVER_USER="${SERVER_USER:-quertimizer}"
SERVER_HOST="${SERVER_HOST:-quertimizer.com}"

APP_BASE="${APP_BASE:-/home/quertimizer/apps}"
SERVER_BASE="${SERVER_BASE:-$APP_BASE/server}"

LOCAL_SERVER_DIR="${LOCAL_SERVER_DIR:-.}"
GRADLE_CMD="${GRADLE_CMD:-./gradlew}"
GRADLE_TASK="${GRADLE_TASK:-bootJar}"

RELEASE="$(date +%Y_%m_%d_%H_%M_%S)"

echo "[server] build start"
"$GRADLE_CMD" "$GRADLE_TASK"

JAR_FILE="$(find "$LOCAL_SERVER_DIR/build/libs" -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | sort | head -n 1)"

if [[ -z "${JAR_FILE:-}" ]]; then
  echo "[server] executable jar not found in $LOCAL_SERVER_DIR/build/libs"
  exit 1
fi

ssh "${SERVER_USER}@${SERVER_HOST}" "
  set -Eeuo pipefail
  mkdir -p '$SERVER_BASE/releases/$RELEASE'
  test -f '$APP_BASE/environment.env'
"

scp "$JAR_FILE" "${SERVER_USER}@${SERVER_HOST}:$SERVER_BASE/releases/$RELEASE/app.jar"

ssh "${SERVER_USER}@${SERVER_HOST}" "
  set -Eeuo pipefail
  ln -sfn '$SERVER_BASE/releases/$RELEASE' '$SERVER_BASE/current'
  sudo systemctl restart quertimizer-server
  sudo systemctl --no-pager --full status quertimizer-server
"

echo '[server] done'