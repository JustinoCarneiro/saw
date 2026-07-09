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

// M07 · Google OAuth
export interface OAuth2ConfigResponse {
  googleEnabled: boolean;
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

// M06 · E11 (Gestão Admin) + E5 (Mentorias & Atas) + diferencial de IA
export type StatusMentorado = 'ATIVO' | 'INATIVO';

export interface MentoradoAdmin {
  id: string;
  nome: string;
  email: string;
  negocio: string | null;
  plano: Plano;
  status: StatusMentorado;
  criadoEm: string;
}

export interface MentoradoCriado {
  id: string;
  nome: string;
  email: string;
  senhaTemporaria: string;
}

export type TipoMentoria = 'INDIVIDUAL' | 'GRUPO';
export type StatusMentoria = 'AGENDADA' | 'CONFIRMADA' | 'REALIZADA' | 'CANCELADA';

export interface MentorResumo {
  id: string;
  nome: string;
}

// M12 — mesma forma usada tanto na visão Admin (MentoriaResponse, sem filtro) quanto na
// mentee-facing (MentoriaMentorado, filtrada por publicado/plano — ver ROADMAP.md M12).
export interface MaterialResumo {
  id: string;
  titulo: string;
  tipo: TipoConteudo;
  url: string;
}

export interface Mentoria {
  id: string;
  tipo: TipoMentoria;
  mentor: MentorResumo;
  mentorados: { id: string; nome: string }[];
  dataHora: string;
  duracaoMin: number;
  linkOnline: string | null;
  local: string | null;
  status: StatusMentoria;
  materiaisRecomendados: MaterialResumo[];
}

export type StatusAta = 'RASCUNHO' | 'PUBLICADA';
export type StatusProcessamentoAta = 'SEM_AUDIO' | 'PROCESSANDO' | 'CONCLUIDO' | 'FALHA';

export interface SugestaoEncaminhamento {
  id: string;
  titulo: string;
  pesoSugerido: number;
  aceito: boolean;
}

export interface Ata {
  id: string;
  mentoriaId: string;
  transcricao: string | null;
  resumo: string | null;
  statusProcessamento: StatusProcessamentoAta;
  status: StatusAta;
  erroProcessamento: string | null;
  publicadaEm: string | null;
  sugestoes: SugestaoEncaminhamento[];
}

// M12 · E5 (lado mentorado) — deliberadamente mais enxuto que Ata (Admin) acima: nunca
// transcricao/erroProcessamento/sugestoes (dado interno do pipeline de IA/pré-revisão humana),
// só aparece quando a ata já está PUBLICADA (ver ROADMAP.md M12).
export interface AtaResumoMentorado {
  resumo: string | null;
  publicadaEm: string;
}

export interface MentoriaMentorado {
  id: string;
  tipo: TipoMentoria;
  mentorNome: string;
  dataHora: string;
  duracaoMin: number;
  linkOnline: string | null;
  local: string | null;
  status: StatusMentoria;
  podeEntrarAgora: boolean;
  ata: AtaResumoMentorado | null;
  materiaisRecomendados: MaterialResumo[];
}

export type TipoConteudo = 'DOCUMENTO' | 'VIDEO' | 'PLANILHA' | 'APRESENTACAO' | 'OUTRO';

export interface Conteudo {
  id: string;
  titulo: string;
  tipo: TipoConteudo;
  url: string;
  planoMinimo: Plano;
  publicado: boolean;
  criadoEm: string;
  favorito?: boolean;
  assistido?: boolean;
}

// M08 · E2 (Dashboard do Mentorado)
export interface CompromissoMentorado {
  id: string;
  tipo: TipoMentoria;
  dataHora: string;
  linkOnline: string | null;
  local: string | null;
}

export interface DicaDestaque {
  id: string;
  titulo: string;
  url: string;
}

export interface DashboardMentoradoResponse {
  nome: string;
  evolucaoGeralPct: number;
  tarefasAbertas: number;
  metaSemanalPct: number | null;
  proximaReuniao: CompromissoMentorado | null;
  compromissos: CompromissoMentorado[];
  dicaDestaque: DicaDestaque | null;
  avisos: string[];
}

// M09 · E3 (Metas Estratégicas)
export type StatusMeta = 'ATIVA' | 'CONCLUIDA' | 'PAUSADA';
export type SubStatusMeta = 'NO_PRAZO' | 'ATENCAO' | 'ATRASADA';

export interface Meta {
  id: string;
  titulo: string;
  descricao: string | null;
  prazo: string;
  diasRestantes: number;
  progressoPct: number;
  status: StatusMeta;
  subStatus: SubStatusMeta | null;
}

export interface ResumoMetas {
  conclusaoMediaPct: number;
  concluidas: number;
  noPrazo: number;
  atrasadas: number;
}

// M10 · E4 (Tarefas & Agenda)
export type StatusTarefa = 'PENDENTE' | 'EM_ANDAMENTO' | 'CONCLUIDA';
export type Prioridade = 'ALTA' | 'MEDIA' | 'BAIXA';

export interface MetaRelacionada {
  id: string;
  titulo: string;
}

export interface Tarefa {
  id: string;
  titulo: string;
  metaRelacionada: MetaRelacionada | null;
  prazo: string | null;
  diasRestantes: number | null;
  prioridade: Prioridade;
  status: StatusTarefa;
  atrasada: boolean;
  peso: number;
}

export interface ResumoTarefas {
  total: number;
  concluidas: number;
  emAndamento: number;
  pendentes: number;
}

export type TipoEvento = 'AO_VIVO' | 'PRESENCIAL';
export type StatusEvento = 'PROGRAMADO' | 'AO_VIVO' | 'REALIZADO' | 'CANCELADO';

export interface Evento {
  id: string;
  titulo: string;
  tipo: TipoEvento;
  tema: string | null;
  dataHora: string;
  local: string | null;
  linkOnline: string | null;
  vagas: number | null;
  status: StatusEvento;
}

// M13 · E7 (lado mentorado) — mentee-facing, com vagasDisponiveis (derivado) e inscrito (estado
// do mentorado atual), nenhum dos dois existe na visão Admin acima.
export interface EventoMentorado {
  id: string;
  titulo: string;
  tipo: TipoEvento;
  tema: string | null;
  dataHora: string;
  local: string | null;
  linkOnline: string | null;
  vagas: number | null;
  vagasDisponiveis: number | null;
  status: StatusEvento;
  inscrito: boolean;
}
