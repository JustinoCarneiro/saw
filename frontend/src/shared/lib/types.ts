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
  carteira: number;
}

// M20 · H15.7 — desempenho do time no período: metaFechamentos/fechamentosRealizados/
// pctAtingidoFechamentos só existem pra quem já tem meta comercial configurada no período (hoje,
// só área Comercial) — null pros demais, mentoriasRealizadas sempre computado.
export interface DesempenhoColaborador {
  id: string;
  nome: string;
  area: Area;
  mentoriasRealizadas: number;
  metaFechamentos: number | null;
  fechamentosRealizados: number | null;
  pctAtingidoFechamentos: number | null;
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

// M21 — import CSV é tudo-ou-nada: erros vazio = todas as linhas foram persistidas.
export interface ImportErro {
  linha: number;
  motivo: string;
}

export interface ImportResultResponse {
  totalLinhas: number;
  importados: number;
  erros: ImportErro[];
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
  vencimentoPlano: string | null;
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
  avisos: AvisoMentorado[];
}

// M17 — E16 Avisos & Notificações.
export type CategoriaAviso = 'GERAL' | 'MENTORIAS' | 'MATERIAIS' | 'EVENTOS';

export interface Aviso {
  id: string;
  titulo: string;
  descricao: string;
  categoria: CategoriaAviso;
  planoMinimo: Plano;
  criadoEm: string;
}

export interface AvisoMentorado {
  id: string;
  titulo: string;
  descricao: string;
  categoria: CategoriaAviso;
  lido: boolean;
  quando: string;
}

export interface ResumoAvisos {
  naoLidos: number;
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

// M14 · E8 (Loja SAW)
export type CategoriaProduto = 'CURSO' | 'PLANILHA' | 'TEMPLATE' | 'EBOOK' | 'FERRAMENTA' | 'KIT' | 'CONSULTORIA';
export type StatusPedido = 'CARRINHO' | 'AGUARDANDO_PAGAMENTO' | 'PAGO' | 'LIBERADO' | 'CANCELADO' | 'REEMBOLSADO';

// Admin — inclui arquivoUrl/publicado, que a visão do mentorado (ProdutoCatalogo) nunca expõe.
export interface Produto {
  id: string;
  titulo: string;
  descricao: string;
  categoria: CategoriaProduto;
  preco: number;
  precoOriginal: number | null;
  avaliacaoMedia: number | null;
  destaque: boolean;
  vendas: number;
  arquivoUrl: string;
  imagemUrl: string | null;
  publicado: boolean;
  criadoEm: string;
}

// Mentee-facing — sem arquivoUrl (só liberado após pagamento, ver PedidoMentorado).
export interface ProdutoCatalogo {
  id: string;
  titulo: string;
  descricao: string;
  categoria: CategoriaProduto;
  preco: number;
  precoOriginal: number | null;
  avaliacaoMedia: number | null;
  destaque: boolean;
  vendas: number;
  imagemUrl: string | null;
}

export interface ItemCarrinho {
  id: string;
  produtoId: string;
  titulo: string;
  imagemUrl: string | null;
  quantidade: number;
  precoUnitario: number;
  subtotal: number;
}

export interface Carrinho {
  id: string | null;
  status: StatusPedido;
  valorTotal: number;
  itens: ItemCarrinho[];
}

export interface ItemPedidoMentorado {
  titulo: string;
  quantidade: number;
  precoUnitario: number;
  arquivoUrl: string | null;
}

export interface PedidoMentorado {
  id: string;
  status: StatusPedido;
  valorTotal: number;
  criadoEm: string;
  itens: ItemPedidoMentorado[];
}

export interface CheckoutResponse {
  checkoutUrl: string;
}

// Admin — visão de todos os pedidos, com nome do mentorado.
export interface ItemPedidoAdmin {
  titulo: string;
  quantidade: number;
  precoUnitario: number;
}

export interface PedidoAdmin {
  id: string;
  mentoradoId: string;
  mentoradoNome: string;
  status: StatusPedido;
  valorTotal: number;
  referenciaGateway: string | null;
  criadoEm: string;
  itens: ItemPedidoAdmin[];
}

// M15 — E9 Perfil & Gamificação.
export interface PerfilMentorado {
  nome: string;
  negocio: string | null;
  email: string;
  telefone: string | null;
  bio: string | null;
  areasInteresse: string[];
  fotoUrl: string | null;
  plano: Plano;
  vencimentoPlano: string | null;
  membroDesde: string;
}

export type NivelJornada = 'BRONZE' | 'PRATA' | 'OURO' | 'DIAMANTE';

export interface StatsJornada {
  materiaisAcessados: number;
  dicasAssistidas: number;
  eventosParticipados: number;
  mentoriasRealizadas: number;
}

export interface Conquista {
  codigo: string;
  titulo: string;
  descricao: string;
  desbloqueada: boolean;
}

export interface Jornada {
  nivelAtual: NivelJornada;
  xp: number;
  xpProximoNivel: number | null;
  progressoPct: number;
  stats: StatsJornada;
  conquistas: Conquista[];
}

export interface PlanoDisponivel {
  plano: Plano;
  acimaDoPlanoAtual: boolean;
}

export interface Assinatura {
  planoAtual: Plano;
  vencimentoPlano: string | null;
  planosDisponiveis: PlanoDisponivel[];
}

// M16 — E10 Painel Administrativo & Métricas.
export interface CrescimentoMesItem {
  mes: string;
  total: number;
}

export interface DistribuicaoPlanoItem {
  plano: Plano;
  quantidade: number;
  pct: number;
}

export interface AtividadeRecente {
  tipo: string;
  descricao: string;
  quando: string;
}

export interface MentoriaHojeItem {
  tipo: TipoMentoria;
  mentorNome: string | null;
  mentoradoNomes: string;
  hora: string;
  status: StatusMentoria;
}

// M23 — sparkline nos KPIs; mesma janela de 6 meses de CrescimentoMesItem, genérico o bastante
// pra contagem (mentorias/eventos) ou valor monetário (receita).
export interface SparklinePonto {
  mes: string;
  valor: number;
}

export interface DashboardAdminResponse {
  mentoradosAtivos: number;
  variacaoMentoradosAtivosPct: number;
  mentoriasRealizadas: number;
  variacaoMentoriasRealizadasPct: number;
  eventosRealizados: number;
  variacaoEventosRealizadosPct: number;
  receitaMes: number;
  variacaoReceitaMesPct: number;
  crescimentoMentorados: CrescimentoMesItem[];
  distribuicaoPlano: DistribuicaoPlanoItem[];
  atividadesRecentes: AtividadeRecente[];
  mentoriasHoje: MentoriaHojeItem[];
  historicoMentoriasRealizadas: SparklinePonto[];
  historicoEventosRealizados: SparklinePonto[];
  historicoReceitaMes: SparklinePonto[];
}
