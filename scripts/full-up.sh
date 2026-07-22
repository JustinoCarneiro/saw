#!/usr/bin/env bash
# Sobe a stack INTEIRA containerizada (infra + backend + frontend) — pra testar o
# sistema "como vai rodar em produção" antes de dar deploy (front e back atrás da
# mesma origem, path-based via Nginx, igual à config real do Coolify).
#
# NÃO é o fluxo de dev diário — pra isso use scripts/dev-up.sh (roda nativo, hot-reload
# de verdade). Não rode os dois ao mesmo tempo: as portas 8080/5432/6379 colidem.
#
# Uso: ./scripts/full-up.sh

set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
info()  { echo -e "${BLUE}==>${NC} $1"; }
ok()    { echo -e "${GREEN}✓${NC} $1"; }
warn()  { echo -e "${YELLOW}!${NC} $1"; }
fail()  { echo -e "${RED}✗${NC} $1"; exit 1; }

COMPOSE="docker compose -f docker-compose.yml -f docker-compose.full.yml"

# Falha rápido e com mensagem clara se a porta já estiver ocupada por outra coisa
# (o caso mais comum: o ambiente nativo do dev-up.sh ainda rodando) — em vez de deixar
# buildar as imagens (demorado) só pra falhar no fim com um erro genérico do Docker.
check_port_free_for_container() {
  local port="$1" container="$2" label="$3"
  if lsof -i ":$port" -sTCP:LISTEN >/dev/null 2>&1; then
    if docker ps --filter "name=^${container}$" --filter "status=running" --format '{{.Ports}}' | grep -q ":$port->"; then
      return 0
    fi
    fail "Porta $port já está em uso (necessária pra $label) e não é o container $container. Se o ambiente nativo estiver no ar, rode ./scripts/dev-down.sh primeiro."
  fi
}

info "Checando se as portas estão livres..."
check_port_free_for_container 8080 "sawhub_backend" "o backend"
check_port_free_for_container 8081 "sawhub_frontend" "o frontend"
ok "Portas livres (ou já são os próprios containers da stack completa)."

info "Buildando e subindo a stack completa (isso pode demorar na primeira vez)..."
if ! $COMPOSE up -d --build; then
  fail "docker compose up falhou. Veja os logs com: $COMPOSE logs"
fi

info "Aguardando backend responder em :8080..."
waited=0
until curl -s -o /dev/null "http://localhost:8080/api/v1/auth/me"; do
  if [ "$waited" -ge 120 ]; then
    warn "Backend não respondeu em 120s. Últimas linhas do log:"
    $COMPOSE logs --tail=40 backend
    fail "Timeout esperando o backend."
  fi
  sleep 2
  waited=$((waited + 2))
done
ok "Backend pronto."

info "Aguardando frontend responder em :8081..."
waited=0
until curl -s -o /dev/null "http://localhost:8081/"; do
  if [ "$waited" -ge 60 ]; then
    warn "Frontend não respondeu em 60s. Últimas linhas do log:"
    $COMPOSE logs --tail=40 frontend
    fail "Timeout esperando o frontend."
  fi
  sleep 1
  waited=$((waited + 1))
done
ok "Frontend pronto."

echo
ok "Stack completa no ar (tudo em container)."
echo
echo "  Frontend:  http://localhost:8081/login"
echo "  Backend:   http://localhost:8080/api/v1"
echo "  pgAdmin:   http://localhost:5050"
echo
echo "  Login de teste (Fundador): admin@sawhub.com.br / trocar-no-primeiro-login"
echo
echo "  Logs:  docker compose -f docker-compose.yml -f docker-compose.full.yml logs -f [serviço]"
echo "  Parar: ./scripts/full-down.sh"
