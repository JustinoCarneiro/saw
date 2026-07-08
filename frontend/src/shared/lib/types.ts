export type Area = 'COMERCIAL' | 'MARKETING' | 'GESTAO_PERFORMANCE' | 'FUNDADOR';

export type Modulo =
  | 'DASHBOARD'
  | 'COMERCIAL'
  | 'FINANCEIRO'
  | 'MENTORADOS'
  | 'CONTEUDOS'
  | 'TIME'
  | 'PAINEL_CONSOLIDADO';

export interface MeResponse {
  id: string;
  nome: string;
  email: string;
  perfil: 'ADMIN' | 'MENTORADO';
  area: Area | null;
  modulosPermitidos: Modulo[];
}

export interface Colaborador {
  id: string;
  nome: string;
  email: string;
  area: Area;
  carteira: number | null;
  conversaoPct: number | null;
}

export interface PermissionMatrixRow {
  area: Area;
  modulosPermitidos: Modulo[];
}

export interface MentoradoConsolidado {
  id: string;
  nome: string;
  negocio: string | null;
  progressoPct: number;
  encaminhamentosCumpridos: number;
  encaminhamentosTotal: number;
  ferramentasPct: number;
  crescimentoFaturamentoPct: number;
  status: 'EM_DIA' | 'ATENCAO' | 'ATRASADO';
}

export interface ConsolidatedSummary {
  total: number;
  emDia: number;
  atencao: number;
  atrasado: number;
  progressoMedioPct: number;
}

export interface RankingFaturamento {
  pos: number;
  nome: string;
  crescimentoFaturamentoPct: number;
}

// E14 · Financeiro & DRE
export type TipoLancamento = 'RECEITA' | 'DESPESA';
export type StatusLancamento = 'PREVISTO' | 'REALIZADO';
export type GrupoDre = 'RECEITA_BRUTA' | 'DEDUCOES' | 'CUSTOS' | 'DESPESA_OPERACIONAL';
export type OrigemReceita = 'ASSINATURA' | 'LOJA' | 'EVENTO' | 'OUTRA';
export type TipoConta = 'A_PAGAR' | 'A_RECEBER';
export type StatusConta = 'PENDENTE' | 'PAGO' | 'RECEBIDO' | 'VENCIDO';
export type Plano = 'GRATUITO' | 'BASICO' | 'ESSENCIAL' | 'PROFISSIONAL';

export interface CategoriaFinanceira {
  id: string;
  nome: string;
  tipo: TipoLancamento;
  grupoDre: GrupoDre;
  origemReceita: OrigemReceita | null;
}

export interface Lancamento {
  id: string;
  tipo: TipoLancamento;
  categoria: { id: string; nome: string; origemReceita: OrigemReceita | null };
  descricao: string;
  valor: number;
  dataCompetencia: string;
  status: StatusLancamento;
  planoReferencia: Plano | null;
}

export interface Conta {
  id: string;
  tipo: TipoConta;
  descricao: string;
  valor: number;
  dataVencimento: string;
  dataPagamento: string | null;
  status: StatusConta;
  lancamentoId: string | null;
}

export interface ComparativoMes {
  resultado: number;
  variacaoPct: number;
}

export interface DreResponse {
  periodo: string;
  receitaBruta: number;
  deducoes: number;
  receitaLiquida: number;
  custos: number;
  despesasOperacionais: number;
  resultado: number;
  comparativoMesAnterior: ComparativoMes;
}

export interface ComposicaoReceita {
  origem: OrigemReceita;
  valor: number;
}

export interface DashboardFaturamentoResponse {
  faturamentoMensal: number;
  mrr: number;
  churnPct: number;
  composicao: ComposicaoReceita[];
}

// E13 · Comercial & Vendas
export type StatusLead = 'SOLICITACAO' | 'EM_CONTATO' | 'PROPOSTA' | 'FECHADO' | 'PERDIDO';

export interface VendedorResumo {
  id: string;
  nome: string;
}

export interface Lead {
  id: string;
  nome: string;
  email: string;
  telefone: string | null;
  mensagem: string | null;
  planoInteresse: Plano | null;
  status: StatusLead;
  vendedor: VendedorResumo | null;
  planoFechado: Plano | null;
  motivoPerdido: string | null;
  dataFechamento: string | null;
  criadoEm: string;
}

export interface FunilItem {
  status: StatusLead;
  quantidade: number;
}

export interface DashboardComercialResponse {
  novosMentoradosNoMes: number;
  taxaConversaoPct: number;
  mrr: number;
  vendasLoja: number;
  variacaoMrrPct: number;
  funil: FunilItem[];
}

export interface RankingComercialItem {
  vendedor: VendedorResumo;
  metaFechamentos: number;
  realizado: number;
  pctAtingido: number;
}
