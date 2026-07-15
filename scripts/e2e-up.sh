#!/usr/bin/env bash
# Sobe uma stack ISOLADA só pra rodar os testes E2E (Playwright): backend na porta 8090
# (banco sawhub_db_e2e, Redis índice 1) + frontend na porta 5183. Reusa o mesmo container
# de Postgres/Redis do scripts/dev-up.sh (banco/índice lógico diferente, não container
# diferente), então pode rodar com o ambiente de dev interativo no ar ao lado sem os
# testes poluírem os dados que você está navegando/demonstrando em :5173.
#
# Uso: ./scripts/e2e-up.sh
# Depois:  cd frontend && E2E_BASE_URL=http://localhost:5183 npm run test:e2e
# Pra parar: ./scripts/e2e-down.sh

set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEV_DIR="$ROOT_DIR/.dev"
LOG_DIR="$DEV_DIR/logs"
PID_DIR="$DEV_DIR/pids"
mkdir -p "$LOG_DIR" "$PID_DIR"

BACKEND_PORT=8090
FRONTEND_PORT=5183
DB_NAME=sawhub_db_e2e
BACKEND_LOG="$LOG_DIR/backend-e2e.log"
FRONTEND_LOG="$LOG_DIR/frontend-e2e.log"
BACKEND_PID_FILE="$PID_DIR/backend-e2e.pid"
FRONTEND_PID_FILE="$PID_DIR/frontend-e2e.pid"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
info()  { echo -e "${BLUE}==>${NC} $1"; }
ok()    { echo -e "${GREEN}✓${NC} $1"; }
warn()  { echo -e "${YELLOW}!${NC} $1"; }
fail()  { echo -e "${RED}✗${NC} $1"; exit 1; }

port_open() {
  (exec 3<>"/dev/tcp/localhost/$1") 2>/dev/null && exec 3>&- 3<&-
}

is_pid_alive() {
  [ -f "$1" ] && kill -0 "$(cat "$1")" 2>/dev/null
}

# ---------------------------------------------------------------------------
# 1. Infra Docker (mesma do dev-up.sh — idempotente, sobe se ainda não estiver no ar)
# ---------------------------------------------------------------------------
info "Garantindo infra Docker (Postgres, Redis) no ar..."
cd "$ROOT_DIR"
if ! docker compose up -d db redis 2>&1 | tee -a "$LOG_DIR/docker-compose.log"; then
  fail "docker compose up falhou. Veja $LOG_DIR/docker-compose.log (o Docker está rodando?)"
fi
until docker compose exec -T db pg_isready -U "${POSTGRES_USER:-sawhub_user}" >/dev/null 2>&1; do sleep 1; done
ok "Postgres pronto."

# ---------------------------------------------------------------------------
# 2. Banco de dados isolado (mesmo container, banco lógico separado)
# ---------------------------------------------------------------------------
info "Garantindo banco '$DB_NAME' (isolado do sawhub_db de dev)..."
EXISTE=$(docker compose exec -T db psql -U "${POSTGRES_USER:-sawhub_user}" -d "${POSTGRES_DB:-sawhub_db}" -tAc \
  "SELECT 1 FROM pg_database WHERE datname = '$DB_NAME'")
if [ "$EXISTE" != "1" ]; then
  docker compose exec -T db psql -U "${POSTGRES_USER:-sawhub_user}" -d "${POSTGRES_DB:-sawhub_db}" -c "CREATE DATABASE $DB_NAME" \
    || fail "Não consegui criar o banco $DB_NAME."
  ok "Banco '$DB_NAME' criado."
else
  ok "Banco '$DB_NAME' já existe."
fi

# ---------------------------------------------------------------------------
# 3. Backend isolado (porta 8090, banco sawhub_db_e2e, Redis índice 1)
# ---------------------------------------------------------------------------
if is_pid_alive "$BACKEND_PID_FILE" || port_open "$BACKEND_PORT"; then
  warn "Backend E2E já parece estar rodando na porta $BACKEND_PORT — pulando."
else
  info "Subindo backend E2E (Spring Boot, porta $BACKEND_PORT, banco $DB_NAME)..."
  cd "$ROOT_DIR/backend"
  # SEED_DEMO_DATA=true sempre: cada subida do backend E2E parte de um banco vazio (ou já
  # seedado antes) e reseeda o dataset curado — os testes esperam esse estado conhecido.
  # APP_RATE_LIMIT_LEAD bem mais alto que o default de produção (5/10min, ver
  # LeadRateLimitFilter): a suíte inteira soma mais de 5 submissões reais do formulário público
  # de "solicitar acesso" (comercial.spec.ts + mentorados.spec.ts + import-export), todas do
  # mesmo IP — no ambiente isolado de E2E o limite não faz sentido, quem preserva a proteção
  # real (dev/produção) é o default do application.yml, intocado.
  SEED_DEMO_DATA=true BOOTSTRAP_FUNDADOR_SENHA=trocar-no-primeiro-login \
    PGCRYPTO_KEY=chave-de-desenvolvimento-nunca-usar-em-producao \
    POSTGRES_DB="$DB_NAME" SERVER_PORT="$BACKEND_PORT" REDIS_DATABASE=1 \
    CORS_ALLOWED_ORIGINS="http://localhost:$FRONTEND_PORT" APP_RATE_LIMIT_LEAD=1000 \
    nohup ./mvnw -q spring-boot:run > "$BACKEND_LOG" 2>&1 &
  echo $! > "$BACKEND_PID_FILE"

  info "Aguardando backend E2E responder (pode demorar no primeiro boot)..."
  waited=0
  until curl -s -o /dev/null "http://localhost:$BACKEND_PORT/api/v1/auth/me"; do
    if ! is_pid_alive "$BACKEND_PID_FILE"; then
      warn "O processo do backend E2E morreu. Últimas linhas do log:"
      tail -40 "$BACKEND_LOG"
      fail "Backend E2E não subiu. Log completo em $BACKEND_LOG"
    fi
    if [ "$waited" -ge 120 ]; then
      warn "Backend E2E não respondeu em 120s. Últimas linhas do log:"
      tail -40 "$BACKEND_LOG"
      fail "Timeout esperando o backend E2E. Log completo em $BACKEND_LOG"
    fi
    sleep 2
    waited=$((waited + 2))
  done
  ok "Backend E2E no ar (PID $(cat "$BACKEND_PID_FILE"))."
fi

# ---------------------------------------------------------------------------
# 4. Frontend isolado (porta 5183, proxy pro backend E2E)
# ---------------------------------------------------------------------------
if is_pid_alive "$FRONTEND_PID_FILE" || port_open "$FRONTEND_PORT"; then
  warn "Frontend E2E já parece estar rodando na porta $FRONTEND_PORT — pulando."
else
  cd "$ROOT_DIR/frontend"
  if [ ! -d node_modules ]; then
    info "node_modules ausente — rodando npm install..."
    npm install 2>&1 | tee -a "$LOG_DIR/npm-install.log" || fail "npm install falhou. Veja $LOG_DIR/npm-install.log"
  fi

  info "Subindo frontend E2E (Vite, porta $FRONTEND_PORT, proxy -> :$BACKEND_PORT)..."
  BACKEND_PORT="$BACKEND_PORT" nohup npm run dev -- --port "$FRONTEND_PORT" --strictPort > "$FRONTEND_LOG" 2>&1 &
  echo $! > "$FRONTEND_PID_FILE"

  info "Aguardando frontend E2E responder..."
  waited=0
  until curl -s -o /dev/null "http://localhost:$FRONTEND_PORT/"; do
    if ! is_pid_alive "$FRONTEND_PID_FILE"; then
      warn "O processo do frontend E2E morreu. Últimas linhas do log:"
      tail -40 "$FRONTEND_LOG"
      fail "Frontend E2E não subiu. Log completo em $FRONTEND_LOG"
    fi
    if [ "$waited" -ge 60 ]; then
      warn "Frontend E2E não respondeu em 60s. Últimas linhas do log:"
      tail -40 "$FRONTEND_LOG"
      fail "Timeout esperando o frontend E2E. Log completo em $FRONTEND_LOG"
    fi
    sleep 1
    waited=$((waited + 1))
  done
  ok "Frontend E2E no ar (PID $(cat "$FRONTEND_PID_FILE"))."
fi

# ---------------------------------------------------------------------------
echo
ok "Stack E2E isolada no ar — o ambiente de dev em :5173 (se estiver no ar) fica intocado."
echo
echo "  Frontend E2E:  http://localhost:$FRONTEND_PORT"
echo "  Backend E2E:   http://localhost:$BACKEND_PORT/api/v1"
echo
echo "  Rodar os testes:  cd frontend && E2E_BASE_URL=http://localhost:$FRONTEND_PORT npm run test:e2e"
echo "  Logs:  $LOG_DIR/{backend,frontend}-e2e.log"
echo "  Parar: ./scripts/e2e-down.sh"
