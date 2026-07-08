# Rodando o SAW HUB localmente

Duas formas de subir o sistema, pra dois propósitos diferentes. Não rode as duas ao
mesmo tempo — as portas 8080/5432/6379 colidem.

## 1. Dev diário — `scripts/dev-up.sh`

Backend e frontend rodam **nativos** (fora de container): hot-reload de verdade
(Vite HMR, recompilação incremental do Maven). Só a infra (Postgres, Redis, pgAdmin)
roda em Docker.

```bash
./scripts/dev-up.sh        # sobe infra + backend (:8080) + frontend (:5173)
./scripts/dev-down.sh      # para backend + frontend (infra continua no ar)
./scripts/dev-down.sh --infra   # para os containers de infra também (dados preservados)
```

- Logs: `.dev/logs/{backend,frontend}.log`
- Login de teste (Fundador, seedado): `matheus@sawhub.com.br` / `trocar-no-primeiro-login`
- Testes E2E (Playwright): `cd frontend && npm run test:e2e` (precisa do sistema já no ar)

## 2. Sanity-check pré-deploy — `scripts/full-up.sh`

Sobe a stack **inteira containerizada** (infra + backend + frontend), front e back
atrás da mesma origem via Nginx (path-based, igual à config real do Coolify em
produção). Serve pra validar que os Dockerfiles buildam certo e que sessão/cookie/CSRF
funcionam na topologia de produção antes de dar deploy — não é o fluxo de dev diário
(sem hot-reload, precisa rebuildar a imagem a cada mudança).

```bash
./scripts/full-up.sh       # builda as imagens e sobe tudo (front :8081, back :8080)
./scripts/full-down.sh     # derruba tudo
./scripts/full-down.sh -v  # derruba tudo e apaga o volume do Postgres
```

- Frontend: `http://localhost:8081` (Nginx serve o build estático e faz proxy de `/api` pro backend)
- Backend: `http://localhost:8080` (exposto direto também, útil pra debug)
- Logs: `docker compose -f docker-compose.yml -f docker-compose.full.yml logs -f [serviço]`

## Variáveis de ambiente obrigatórias (backend)

Desde a revisão de segurança (achado H1), `BOOTSTRAP_FUNDADOR_SENHA` **não tem default** —
o backend falha no boot (`PlaceholderResolutionException`) se ela não existir, de propósito
(sem isso, um deploy que esqueça de configurar a env sobia em produção com senha conhecida).
`SEED_DEMO_DATA` também virou `false` por padrão. Os dois scripts (`dev-up.sh` e
`docker-compose.full.yml`) já exportam essas envs pra você — só precisa se preocupar com isso
se for rodar `./mvnw spring-boot:run` manualmente, fora dos scripts:

```bash
SEED_DEMO_DATA=true BOOTSTRAP_FUNDADOR_SENHA=trocar-no-primeiro-login ./mvnw spring-boot:run
```

## Arquivos relevantes

- `docker-compose.yml` — infra base (Postgres, Redis, pgAdmin), usada pelos dois fluxos.
- `docker-compose.full.yml` — estende o base com `backend` + `frontend` containerizados.
- `backend/Dockerfile`, `frontend/Dockerfile` + `frontend/nginx.conf` — build de produção,
  os mesmos usados pelo Coolify no deploy real.
