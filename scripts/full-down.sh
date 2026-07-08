#!/usr/bin/env bash
# Derruba a stack completa subida por scripts/full-up.sh (todos os containers,
# incluindo infra). Passe -v pra também apagar o volume do Postgres (dados perdidos).
#
# Uso: ./scripts/full-down.sh [-v]

set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

COMPOSE="docker compose -f docker-compose.yml -f docker-compose.full.yml"

if [ "${1:-}" = "-v" ]; then
  $COMPOSE down -v
else
  $COMPOSE down
fi
