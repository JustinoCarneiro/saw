import type { ProdutoVenda } from './types';

// M25 — catálogo confirmado via raio-x nas planilhas reais (docs/reuniao-2026-07-17-atualizacoes.md).
// Centralizado aqui (22/07/2026) na 2ª tela que precisou do mesmo rótulo (LeadsComercialPage +
// DashboardComercialPage) — mesmo critério já usado no resto do projeto pra formula/label
// duplicada (ver ROADMAP.md, lições do M16).
export const PRODUTO_VENDA_LABEL: Record<ProdutoVenda, string> = {
  MENTORIA_CONTINUA: 'Mentoria contínua',
  MENTORIA_INDIVIDUAL: 'Mentoria individual',
  CONSULTORIA: 'Consultoria',
  FORMULA_SAW: 'Fórmula SAW',
  FORMACAO_PROFISSIONAL: 'Formação Profissional',
  FICHA_TECNICA_LUCRATIVA: 'Ficha técnica Lucrativa',
  INGRESSO_EVENTO: 'Ingresso de evento',
  PRODUTO_DIGITAL: 'Produto digital (planilha, aula avulsa etc.)',
};
