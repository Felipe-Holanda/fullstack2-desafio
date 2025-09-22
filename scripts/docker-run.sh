#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="${SCRIPT_DIR}/.."
COMPOSE_FILE="${ROOT_DIR}/docker-compose.yml"

usage() {
  cat <<EOF
Usage: $(basename "$0") <command>

Commands:
  up         Build and start containers in detached mode
  down       Stop and remove containers, networks
  logs       Tail logs for all services
  ps         List running containers
  rebuild    Force rebuild images and recreate containers

Examples:
  $(basename "$0") up
  $(basename "$0") logs
EOF
}

cmd=${1:-}
if [[ -z "$cmd" ]]; then
  usage
  exit 1
fi

case "$cmd" in
  up)
    docker compose -f "$COMPOSE_FILE" up -d --build
    ;;
  down)
    docker compose -f "$COMPOSE_FILE" down
    ;;
  logs)
    docker compose -f "$COMPOSE_FILE" logs -f --tail=200
    ;;
  ps)
    docker compose -f "$COMPOSE_FILE" ps
    ;;
  rebuild)
    docker compose -f "$COMPOSE_FILE" build --no-cache
    docker compose -f "$COMPOSE_FILE" up -d --force-recreate
    ;;
  *)
    usage
    exit 1
    ;;

esac
