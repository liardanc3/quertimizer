#!/usr/bin/env bash
set -Eeuo pipefail

bash ./scripts/deploy-server.sh
bash ./scripts/deploy-client.sh