# SAW HUB · Sistema de Design

> Identidade **do cliente (SAW)**, extraída dos mockups oficiais (15 telas). **Não** usa a marca da
> Onda. Tema base **dark**: vinho/bordô + dourado/champagne sobre near-black. Tokens: `./tokens.css`.

## 1. Essência
Premium, sóbrio e focado — um "clube" de alta performance para donos de restaurante. Dark mode
elegante, dourado como brilho de status/conquista, vinho como assinatura da marca.

## 2. Cores (ver `tokens.css`)
| Papel | Token | Hex |
|---|---|---|
| Ação primária / marca | `--gold` | `#F0B050` |
| Hover dourado | `--gold-bright` | `#F8C85E` |
| Dourado suave (texto detalhe) | `--gold-soft` | `#D9BE93` |
| Vinho (marca, login, faixas) | `--wine` | `#57191C` |
| Fundo do app | `--bg` | `#0C0C0C` |
| Cartão / painel | `--surface` | `#141414` |
| Superfície 2 | `--surface-2` | `#1B1917` |
| Borda | `--line` | `#2A2724` |
| Texto | `--text` | `#F4EEE4` |
| Texto secundário | `--text-soft` | `#A9A29A` |

**Status:** verde `--success` (no prazo/concluído/pago) · âmbar `--warning` (atenção/pendente) ·
vermelho `--danger` (atrasado/ao vivo/cancelado) · azul `--info` (visita/informativo).

**Regras:** dourado é pontual (1 ação principal por tela, badges, valores de destaque); nunca como
grande área de fundo. Vinho para faixas de marca e o hero de login. Fundo sempre near-black.

## 3. Tipografia
- **UI (tudo no app):** `Inter` — 400/500/600/700. Títulos de seção 600–700, corpo 400/500.
- **Display/hero (wordmark SAW, login):** `Anton` (condensado pesado, MAIÚSCULAS), como nos mockups.
- Números de métrica: Inter 700–800, tabulares.
- Escala: h1 seção ~22px · card title ~15px · corpo 14px · legenda 12px · métrica 24–30px.

## 4. Componentes-chave
- **Sidebar** (248px): logo SAW no topo, itens com ícone linear; item ativo com faixa/realce dourado
  e leve fundo `--elevated`. Rodapé com Perfil/Suporte.
- **Topbar:** saudação + busca opcional, sino de notificações, avatar do mentorado + plano.
- **Card:** fundo `--surface`, raio `--r-card` (16px), borda `--line`, sombra `--shadow`.
- **KPI/stat:** rótulo (12px, `--text-soft`) + valor grande (Inter 800) + delta colorido.
- **Botão primário:** fundo `--gold`, texto `--on-gold`, pílula (`--r-pill`), peso 600.
  - Secundário: contorno `--line`, texto `--text`. Ghost: só texto/ícone.
- **Chip/tag de status:** pílula pequena, fundo `--*-bg`, texto na cor do status + ícone/rótulo
  (nunca só cor — acessibilidade).
- **Tabela:** cabeçalho discreto (`--text-faint`, uppercase 11px), linhas com divisor `--line-soft`,
  hover `--surface-2`.
- **Barra de progresso:** trilho `--line`, preenchimento `--gold` (ou status).
- **Player de vídeo (dicas):** thumb com play dourado sobre overlay escuro.

## 5. Forma & movimento
- Cantos arredondados (16px cartões, pílula em botões/chips). Sem cantos retos em superfícies.
- Sombra difusa escura para baixo. Foco visível com `--ring` (dourado translúcido).
- Transições suaves (150–250ms, `cubic-bezier(.2,.7,.2,1)`); respeitar `prefers-reduced-motion`.

## 6. Responsividade
> **Fora do MVP** (cliente confirmou em reunião 07/07/2026, ver CLAUDE.md § Princípios) — o
> comportamento abaixo é a intenção de design pra quando mobile virar requisito pós-MVP, mas
> **não foi implementado**: hoje não existe nenhum `@media query` no CSS do frontend. O que se
> pede por enquanto é só não fechar portas (evitar larguras fixas absurdas), não construir isto.
- **Desktop:** sidebar fixa + conteúdo em grid (cards bento). É o único breakpoint que existe hoje.
- **≤900px (planejado, não implementado):** sidebar vira drawer (hambúrguer); grids colapsam para 1–2 colunas.
- **Mobile (planejado, não implementado):** cards empilhados, tabelas viram listas/cards roláveis. Áreas de toque ≥ 44px.

## 7. Acessibilidade (AA no dark)
- Texto `--text` sobre `--bg`/`--surface`: contraste AA. Dourado sobre near-black para destaque, mas
  **texto longo nunca em dourado** (usar creme). Sobre botão dourado, texto `--on-gold` (quase-preto).
- Status sempre com ícone + rótulo, não só cor.

## 8. Iconografia
Linear, traço ~1.6px, cantos arredondados, `currentColor` (herda a cor do contexto). 20px inline /
44px marca de card. Coerente com o conjunto visto nos mockups (dashboard, metas, tarefas…).
