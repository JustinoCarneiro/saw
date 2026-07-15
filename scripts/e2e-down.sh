#!/usr/bin/env bash
# Para o backend e o frontend isolados subidos por e2e-up.sh. A infra Docker (compartilhada
# com o dev-up.sh) e o banco sawhub_db_e2e continuam intactos — só os processos param.
#
# Uso: ./scripts/e2e-down.sh

set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_DIR="$ROOT_DIR/.dev/pids"
BACKEND_PORT=8090
FRONTEND_PORT=5183
IA_STUB_PORT=8091
OAUTH_STUB_PORT=8092
MP_STUB_PORT=8093

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

stop_port "Frontend E2E" "$FRONTEND_PORT"
stop_port "Backend E2E" "$BACKEND_PORT"
stop_port "Stub de IA E2E" "$IA_STUB_PORT"
stop_port "Stub de OAuth2 E2E" "$OAUTH_STUB_PORT"
stop_port "Stub do Mercado Pago E2E" "$MP_STUB_PORT"
rm -f "$PID_DIR/frontend-e2e.pid" "$PID_DIR/backend-e2e.pid" "$PID_DIR/ia-stub-e2e.pid" \
      "$PID_DIR/oauth-stub-e2e.pid" "$PID_DIR/mp-stub-e2e.pid"
