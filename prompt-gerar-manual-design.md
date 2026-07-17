Gere um documento visual (artifact HTML) do Manual do Sistema do SAW HUB, uma plataforma
SaaS de mentoria para restaurantes, a partir do conteúdo em `MANUAL_SISTEMA.md`.

Contexto: é um manual **voltado pro cliente final** (o Fundador da SAW e a equipe dele) —
não é documentação técnica de engenharia. Tom acolhedor, direto, sem jargão de programação.
O documento vai ser consultado por pessoas não-técnicas aprendendo a usar o sistema.

**Identidade visual:** siga a identidade do próprio SAW HUB, não a da Onda — use
`design/tokens.css` e `design/DESIGN.md` como fonte da verdade (tema escuro, vinho/bordô +
dourado/champagne, tipografia display condensada + sans para UI). Não inventar paleta nova.

**Estrutura esperada:**
- Capa/cabeçalho com o nome do sistema e uma frase de abertura convidativa.
- Sumário navegável (âncoras) pras 11 seções do manual: Acesso, Benefícios por Perfil,
  Dashboard Consolidado, Comercial, Financeiro, Mentorados, Mentorias & IA, Conteúdos &
  Loja, Eventos & Avisos, Gestão de Time, Importação em Massa.
- Cada seção com hierarquia visual clara — títulos, sub-blocos por funcionalidade, e destaque
  visual pros "passo a passo" (numerados, fáceis de seguir enquanto a pessoa usa o sistema
  em outra aba).
- A seção "6. Mentorados" reflete uma tela real com 5 abas internas (Painel Consolidado,
  Mentorados, Mentorias, Metas, Tarefas) — represente essa sub-navegação visualmente (ex.:
  mini-abas ou blocos rotulados dentro da seção), não só um bloco de texto corrido, já que é
  assim que a pessoa vai encontrar essas funcionalidades na tela de verdade.
- A seção "2. Benefícios e Fluxos por Perfil" já usa emojis por perfil (👑💼🚀🎓) — mantenha
  esse recurso visual, mas com tratamento gráfico (cards ou blocos), não só texto corrido.
- Callouts visuais distintos pra "Dica" (ex.: o acesso rápido `?demo=1`) vs. passo-a-passo
  operacional — não misturar os dois estilos.
- Tema claro E escuro (o artifact deve responder ao tema do visualizador), mas a identidade
  de marca (vinho/dourado) permanece a mesma âncora em ambos.

**O que evitar:** não adicionar informação que não está no `MANUAL_SISTEMA.md` — é uma
peça de design sobre o conteúdo existente, não uma reescrita ou expansão de funcionalidades.
Não usar a paleta/tipografia de marketing da Onda (Manrope, oceano/turquesa) — isso é
documentação institucional do produto do cliente, identidade errada.
