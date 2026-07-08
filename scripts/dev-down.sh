#!/usr/bin/env bash
# Para o backend e o frontend subidos por dev-up.sh. Por padrão deixa a infra Docker
# (Postgres/Redis/pgAdmin) rodando — passe --infra pra parar os containers também
# (os dados continuam no volume, só os containers param).
#
# Mata por porta (não só pelo PID salvo): `npm run dev` roda o Vite como processo filho
# com outro PID, então matar só o PID do npm deixa o Vite órfão ainda escutando a porta.
#
# Uso: ./scripts/dev-down.sh [--infra]

set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_DIR="$ROOT_DIR/.dev/pids"
BACKEND_PORT=8080
FRONTEND_PORT=5173

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
info()  { echo -e "${BLUE}==>${NC} $1"; }
ok()    { echo -e "${GREEN}✓${NC} $1"; }
warn()  { echo -e "${YELLOW}!${NC} $1"; }

stop_port() {
  local name="$1" port="$2"
  local pids
  pids="$(lsof -ti "tcp:$port" 2>/dev/null || true)"
  if [ -z "$pids" ]; then
    warn "$name: nada rodando na porta $port."
    return
  fi
  info "Parando $name (porta $port, PID(s): $(echo "$pids" | tr '\n' ' '))..."
  # shellcheck disable=SC2086
  kill $pids 2>/dev/null

  for _ in $(seq 1 10); do
    pids="$(lsof -ti "tcp:$port" 2>/dev/null || true)"
    [ -z "$pids" ] && break
    sleep 1
  done

  pids="$(lsof -ti "tcp:$port" 2>/dev/null || true)"
  if [ -n "$pids" ]; then
    warn "$name não parou a tempo, forçando (kill -9)..."
    # shellcheck disable=SC2086
    kill -9 $pids 2>/dev/null
  fi
  ok "$name parado."
}

stop_port "Frontend" "$FRONTEND_PORT"
stop_port "Backend" "$BACKEND_PORT"
rm -f "$PID_DIR/frontend.pid" "$PID_DIR/backend.pid"

if [ "${1:-}" = "--infra" ]; then
  info "Parando containers Docker (dados preservados no volume)..."
  cd "$ROOT_DIR"
  docker compose stop
  ok "Infra parada."
else
  warn "Infra Docker (Postgres/Redis/pgAdmin) continua rodando. Use --infra pra parar também."
fi
