#!/usr/bin/env bash
# Sobe o sistema inteiro do SAW HUB pra desenvolvimento local:
# infra Docker (Postgres + Redis + pgAdmin) -> backend (Spring Boot) -> frontend (Vite).
#
# Uso: ./scripts/dev-up.sh
# Pra parar tudo: ./scripts/dev-down.sh

set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEV_DIR="$ROOT_DIR/.dev"
LOG_DIR="$DEV_DIR/logs"
PID_DIR="$DEV_DIR/pids"
mkdir -p "$LOG_DIR" "$PID_DIR"

BACKEND_PORT=8080
FRONTEND_PORT=5173
BACKEND_LOG="$LOG_DIR/backend.log"
FRONTEND_LOG="$LOG_DIR/frontend.log"
BACKEND_PID_FILE="$PID_DIR/backend.pid"
FRONTEND_PID_FILE="$PID_DIR/frontend.pid"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
info()  { echo -e "${BLUE}==>${NC} $1"; }
ok()    { echo -e "${GREEN}✓${NC} $1"; }
warn()  { echo -e "${YELLOW}!${NC} $1"; }
fail()  { echo -e "${RED}✗${NC} $1"; exit 1; }

port_open() {
  (exec 3<>"/dev/tcp/localhost/$1") 2>/dev/null && exec 3>&- 3<&-
}

wait_for() {
  local desc="$1" timeout_s="$2" check_cmd="$3"
  local waited=0
  until eval "$check_cmd" >/dev/null 2>&1; do
    if [ "$waited" -ge "$timeout_s" ]; then
      fail "$desc não respondeu em ${timeout_s}s."
    fi
    sleep 1
    waited=$((waited + 1))
  done
}

is_pid_alive() {
  [ -f "$1" ] && kill -0 "$(cat "$1")" 2>/dev/null
}

# ---------------------------------------------------------------------------
# 1. Infra Docker (Postgres + Redis + pgAdmin)
# ---------------------------------------------------------------------------
info "Subindo infra Docker (Postgres, Redis, pgAdmin)..."
cd "$ROOT_DIR"
if ! docker compose up -d 2>&1 | tee -a "$LOG_DIR/docker-compose.log"; then
  fail "docker compose up falhou. Veja $LOG_DIR/docker-compose.log (o Docker está rodando?)"
fi

info "Aguardando Postgres aceitar conexões..."
wait_for "Postgres" 60 "docker compose exec -T db pg_isready -U \${POSTGRES_USER:-sawhub_user} >/dev/null"
ok "Postgres pronto."

info "Aguardando Redis responder..."
wait_for "Redis" 30 "docker compose exec -T redis redis-cli -a \${REDIS_PASSWORD:-sawhub_redis_pass} ping 2>/dev/null | grep -q PONG"
ok "Redis pronto."

# ---------------------------------------------------------------------------
# 2. Backend (Spring Boot)
# ---------------------------------------------------------------------------
if is_pid_alive "$BACKEND_PID_FILE" || port_open "$BACKEND_PORT"; then
  warn "Backend já parece estar rodando na porta $BACKEND_PORT — pulando."
else
  info "Subindo backend (Spring Boot, porta $BACKEND_PORT)..."
  cd "$ROOT_DIR/backend"
  # SEED_DEMO_DATA, BOOTSTRAP_FUNDADOR_SENHA e PGCRYPTO_KEY não têm default no application.yml
  # (achado H1 da revisão de segurança / pass transversal de pgcrypto da Fase 5 — fail-closed em
  # produção). Pro dev/demo local continuar funcionando sem fricção, exportamos explicitamente aqui.
  SEED_DEMO_DATA=true BOOTSTRAP_FUNDADOR_SENHA=trocar-no-primeiro-login \
    PGCRYPTO_KEY=chave-de-desenvolvimento-nunca-usar-em-producao \
    EMAIL_PERMITIR_FALLBACK_LOG=true \
    nohup ./mvnw -q spring-boot:run > "$BACKEND_LOG" 2>&1 &
  echo $! > "$BACKEND_PID_FILE"

  info "Aguardando backend responder em /api/v1/auth/me (pode demorar no primeiro boot)..."
  waited=0
  until curl -s -o /dev/null "http://localhost:$BACKEND_PORT/api/v1/auth/me"; do
    if ! is_pid_alive "$BACKEND_PID_FILE"; then
      warn "O processo do backend morreu. Últimas linhas do log:"
      tail -40 "$BACKEND_LOG"
      fail "Backend não subiu. Log completo em $BACKEND_LOG"
    fi
    if [ "$waited" -ge 120 ]; then
      warn "Backend não respondeu em 120s. Últimas linhas do log:"
      tail -40 "$BACKEND_LOG"
      fail "Timeout esperando o backend. Log completo em $BACKEND_LOG"
    fi
    sleep 2
    waited=$((waited + 2))
  done
  ok "Backend no ar (PID $(cat "$BACKEND_PID_FILE"))."
fi

# ---------------------------------------------------------------------------
# 3. Frontend (Vite)
# ---------------------------------------------------------------------------
if is_pid_alive "$FRONTEND_PID_FILE" || port_open "$FRONTEND_PORT"; then
  warn "Frontend já parece estar rodando na porta $FRONTEND_PORT — pulando."
else
  cd "$ROOT_DIR/frontend"
  if [ ! -d node_modules ]; then
    info "node_modules ausente — rodando npm install..."
    npm install 2>&1 | tee -a "$LOG_DIR/npm-install.log" || fail "npm install falhou. Veja $LOG_DIR/npm-install.log"
  fi

  info "Subindo frontend (Vite, porta $FRONTEND_PORT)..."
  nohup npm run dev > "$FRONTEND_LOG" 2>&1 &
  echo $! > "$FRONTEND_PID_FILE"

  info "Aguardando frontend responder..."
  waited=0
  until curl -s -o /dev/null "http://localhost:$FRONTEND_PORT/"; do
    if ! is_pid_alive "$FRONTEND_PID_FILE"; then
      warn "O processo do frontend morreu. Últimas linhas do log:"
      tail -40 "$FRONTEND_LOG"
      fail "Frontend não subiu. Log completo em $FRONTEND_LOG"
    fi
    if [ "$waited" -ge 60 ]; then
      warn "Frontend não respondeu em 60s. Últimas linhas do log:"
      tail -40 "$FRONTEND_LOG"
      fail "Timeout esperando o frontend. Log completo em $FRONTEND_LOG"
    fi
    sleep 1
    waited=$((waited + 1))
  done
  ok "Frontend no ar (PID $(cat "$FRONTEND_PID_FILE"))."
fi

# ---------------------------------------------------------------------------
echo
ok "Sistema no ar."
echo
echo "  Frontend:  http://localhost:$FRONTEND_PORT/login"
echo "  Backend:   http://localhost:$BACKEND_PORT/api/v1"
echo "  pgAdmin:   http://localhost:5050"
echo
echo "  Login de teste (Fundador): matheus@sawhub.com.br / trocar-no-primeiro-login"
echo
echo "  Logs:  $LOG_DIR/{backend,frontend}.log"
echo "  Parar: ./scripts/dev-down.sh"
