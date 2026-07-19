import { type FormEvent, useEffect, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { Card } from '../../shared/components/Card';
import { CsvImportExport } from '../../shared/components/CsvImportExport';
import { DataGrid, DataGridRow } from '../../shared/components/DataGrid';
import { Pill } from '../../shared/components/Pill';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import { formatBRL } from '../../shared/lib/format';
import type {
  CategoriaIngresso,
  EventoVendaResumo,
  FecharVendaRequest,
  FormaPagamento,
  Lead,
  OrigemVenda,
  ParcelaVendaRequest,
  Plano,
  ProdutoVenda,
  StatusLead,
  VendaIngressoRequest,
  VendedorResumo,
} from '../../shared/lib/types';
import styles from './LeadsComercialPage.module.css';

const COLUMNS = '1.4fr 1.6fr 1fr 1fr 1.3fr 1.6fr';

const STATUS_LABEL: Record<StatusLead, { label: string; bg: string; color: string }> = {
  SOLICITACAO: { label: 'Solicitação', bg: 'var(--line)', color: 'var(--text-soft)' },
  EM_CONTATO: { label: 'Em contato', bg: 'var(--info-bg)', color: 'var(--info)' },
  DIAGNOSTICO: { label: 'Diagnóstico', bg: 'var(--info-bg)', color: 'var(--info)' },
  PROPOSTA: { label: 'Proposta', bg: 'var(--warning-bg)', color: 'var(--warning)' },
  FECHADO: { label: 'Fechado', bg: 'var(--success-bg)', color: 'var(--success)' },
  PERDIDO: { label: 'Perdido', bg: 'var(--danger-bg)', color: 'var(--danger)' },
};

const PLANO_LABEL: Record<Plano, string> = {
  GRATUITO: 'Gratuito',
  BASICO: 'Básico',
  ESSENCIAL: 'Essencial',
  PROFISSIONAL: 'Profissional',
};

// M25 — catálogo confirmado via raio-x nas planilhas reais (docs/reuniao-2026-07-17-atualizacoes.md).
const PRODUTO_VENDA_LABEL: Record<ProdutoVenda, string> = {
  MENTORIA_CONTINUA: 'Mentoria contínua',
  MENTORIA_INDIVIDUAL: 'Mentoria individual',
  CONSULTORIA: 'Consultoria',
  FORMULA_SAW: 'Fórmula SAW',
  FORMACAO_PROFISSIONAL: 'Formação Profissional',
  FICHA_TECNICA_LUCRATIVA: 'Ficha técnica Lucrativa',
  INGRESSO_EVENTO: 'Ingresso de evento',
  PRODUTO_DIGITAL: 'Produto digital (planilha, aula avulsa etc.)',
};

const ORIGEM_VENDA_LABEL: Record<OrigemVenda, string> = {
  DIRETA: 'Direta',
  HOTMART: 'Hotmart',
  CORTESIA: 'Cortesia',
  PATROCINIO: 'Patrocínio',
  PALESTRANTE: 'Palestrante',
  PARCEIRO: 'Parceiro',
};

// Gap 8 (raio-x, 19/07/2026) — CORTESIA saiu do eixo errado (é OrigemVenda, não tipo de
// ingresso); BLACK é a categoria real que faltava.
const CATEGORIA_INGRESSO_LABEL: Record<CategoriaIngresso, string> = {
  ESSENCIAL: 'Essencial',
  VIP: 'VIP',
  ESPECIAL: 'Especial',
  BLACK: 'Black',
};

const FORMA_PAGAMENTO_LABEL: Record<FormaPagamento, string> = {
  PIX: 'Pix',
  PIX_RECORRENTE: 'Pix recorrente (assinatura)',
  CARTAO: 'Cartão',
  BOLETO: 'Boleto',
  HOTMART: 'Hotmart',
};

type Acao =
  | { alvo: 'EM_CONTATO'; lead: Lead }
  | { alvo: 'DIAGNOSTICO'; lead: Lead }
  | { alvo: 'PROPOSTA'; lead: Lead }
  | { alvo: 'PERDIDO'; lead: Lead };

export function LeadsComercialPage() {
  const [status, setStatus] = useState<StatusLead | ''>('');
  const [vendedorId, setVendedorId] = useState('');
  const [leads, setLeads] = useState<Lead[] | null>(null);
  const [vendedores, setVendedores] = useState<VendedorResumo[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [acao, setAcao] = useState<Acao | null>(null);
  const [showCriarForm, setShowCriarForm] = useState(false);
  const [fecharVendaLead, setFecharVendaLead] = useState<Lead | null>(null);

  const carregar = () => {
    setLeads(null);
    apiClient
      .get<Lead[]>('/admin/comercial/leads', { params: { status: status || undefined, vendedorId: vendedorId || undefined } })
      .then((res) => setLeads(res.data))
      .catch(() => setError('Não foi possível carregar os leads.'));
  };

  useEffect(carregar, [status, vendedorId]);

  useEffect(() => {
    apiClient.get<VendedorResumo[]>('/admin/comercial/vendedores')
      .then((res) => setVendedores(res.data))
      .catch(() => setError('Não foi possível carregar a lista de vendedores.'));
  }, []);

  return (
    <div>
      <div className={styles.toolbar}>
        <div className={styles.filters}>
          <select className={styles.select} value={status} onChange={(e) => setStatus(e.target.value as StatusLead | '')}>
            <option value="">Todos os status</option>
            {(Object.keys(STATUS_LABEL) as StatusLead[]).map((s) => (
              <option key={s} value={s}>{STATUS_LABEL[s].label}</option>
            ))}
          </select>
          <select className={styles.select} value={vendedorId} onChange={(e) => setVendedorId(e.target.value)}>
            <option value="">Todos os vendedores</option>
            {vendedores.map((v) => (
              <option key={v.id} value={v.id}>{v.nome}</option>
            ))}
          </select>
        </div>
        <div style={{ display: 'flex', gap: 12, alignItems: 'flex-start' }}>
          <CsvImportExport
            exportUrl="/admin/comercial/leads/export"
            exportParams={{ status: status || undefined, vendedorId: vendedorId || undefined }}
            exportFilename="leads.csv"
            importUrl="/admin/comercial/leads/import"
            onImportado={carregar}
          />
          <button className={styles.newButton} onClick={() => setShowCriarForm((v) => !v)}>
            <span style={{ fontSize: 16 }}>+</span>Criar Lead
          </button>
        </div>
      </div>

      {showCriarForm && (
        <CriarLeadForm
          onCriado={() => { setShowCriarForm(false); carregar(); }}
          onCancelar={() => setShowCriarForm(false)}
        />
      )}

      {acao && (
        <AvancarLeadForm
          acao={acao}
          vendedores={vendedores}
          onAvancado={() => { setAcao(null); carregar(); }}
          onCancelar={() => setAcao(null)}
        />
      )}

      {fecharVendaLead && (
        <FecharVendaForm
          lead={fecharVendaLead}
          onFechado={() => { setFecharVendaLead(null); carregar(); }}
          onCancelar={() => setFecharVendaLead(null)}
        />
      )}

      {error && <div className={styles.error}>{error}</div>}

      <DataGrid columns={COLUMNS} headers={['Lead', 'Contato', 'Plano de interesse', 'Status', 'Vendedor', 'Ações']}>
        {leads === null && !error && <div className={styles.loading}>Carregando…</div>}
        {leads?.length === 0 && <div className={styles.loading}>Nenhum lead encontrado.</div>}
        {leads?.map((lead) => {
          const st = STATUS_LABEL[lead.status];
          return (
            <DataGridRow key={lead.id} columns={COLUMNS}>
              <div>
                {/* Texto de lead público (nome/mensagem) renderizado como filho JSX puro —
                    React escapa por padrão, nunca usar dangerouslySetInnerHTML aqui. */}
                <div className={styles.strong}>{lead.nome}</div>
                {lead.mensagem && <div className={styles.muted}>{lead.mensagem}</div>}
              </div>
              <div className={`${styles.muted} ${styles.contato}`}>
                <div>{lead.email}</div>
                {lead.telefone && <div>{lead.telefone}</div>}
              </div>
              <div className={styles.muted}>{lead.planoInteresse ? PLANO_LABEL[lead.planoInteresse] : '—'}</div>
              <div>
                <Pill bg={st.bg} color={st.color}>{st.label}</Pill>
                {lead.status === 'PERDIDO' && lead.motivoPerdido && (
                  <div className={styles.motivo}>{lead.motivoPerdido}</div>
                )}
                {lead.status === 'FECHADO' && lead.produtoVenda && (
                  <div className={styles.motivo}>
                    {PRODUTO_VENDA_LABEL[lead.produtoVenda]}
                    {lead.valorTotalVenda != null && ` · ${formatBRL(lead.valorTotalVenda)}`}
                  </div>
                )}
              </div>
              <div className={styles.muted}>{lead.vendedor?.nome ?? '—'}</div>
              <div className={styles.acoes}>
                {lead.status === 'SOLICITACAO' && (
                  <button className={styles.actionButton} onClick={() => setAcao({ alvo: 'EM_CONTATO', lead })}>
                    Mover p/ Em contato
                  </button>
                )}
                {lead.status === 'EM_CONTATO' && (
                  <>
                    <button className={styles.actionButton} onClick={() => setAcao({ alvo: 'DIAGNOSTICO', lead })}>
                      Mover p/ Diagnóstico
                    </button>
                    <button className={styles.actionButton} onClick={() => setAcao({ alvo: 'PROPOSTA', lead })}>
                      Avançar p/ Proposta
                    </button>
                  </>
                )}
                {lead.status === 'DIAGNOSTICO' && (
                  <button className={styles.actionButton} onClick={() => setAcao({ alvo: 'PROPOSTA', lead })}>
                    Avançar p/ Proposta
                  </button>
                )}
                {lead.status === 'PROPOSTA' && (
                  <button className={styles.actionButton} onClick={() => setFecharVendaLead(lead)}>
                    Fechar venda
                  </button>
                )}
                {(lead.status === 'SOLICITACAO' || lead.status === 'EM_CONTATO' || lead.status === 'DIAGNOSTICO' || lead.status === 'PROPOSTA') && (
                  <button className={styles.actionButtonDanger} onClick={() => setAcao({ alvo: 'PERDIDO', lead })}>
                    Perder
                  </button>
                )}
                {(lead.status === 'FECHADO' || lead.status === 'PERDIDO') && <span className={styles.muted}>—</span>}
              </div>
            </DataGridRow>
          );
        })}
      </DataGrid>
    </div>
  );
}

function CriarLeadForm({ onCriado, onCancelar }: {
  onCriado: () => void;
  onCancelar: () => void;
}) {
  const [nome, setNome] = useState('');
  const [email, setEmail] = useState('');
  const [telefone, setTelefone] = useState('');
  const [planoInteresse, setPlanoInteresse] = useState<Plano | ''>('');
  const [mensagem, setMensagem] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await apiClient.post('/admin/comercial/leads', {
        nome, email, telefone: telefone || undefined, mensagem: mensagem || undefined,
        planoInteresse: planoInteresse || undefined,
      });
      onCriado();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível criar o lead. Confira os dados.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <div className={styles.formTitle}>Criar Lead</div>
      <form onSubmit={handleSubmit} className={styles.form}>
        <div className={styles.formRow}>
          <label className={styles.formField}>
            Nome
            <input className={styles.textInput} value={nome} onChange={(e) => setNome(e.target.value)} required maxLength={120} />
          </label>
          <label className={styles.formField}>
            E-mail
            <input className={styles.textInput} type="email" value={email} onChange={(e) => setEmail(e.target.value)} required maxLength={255} />
          </label>
          <label className={styles.formField}>
            Telefone
            <input className={styles.textInput} value={telefone} onChange={(e) => setTelefone(e.target.value)} maxLength={20} />
          </label>
        </div>
        <div className={styles.formRow}>
          <label className={styles.formField}>
            Plano de interesse
            <select className={styles.select} value={planoInteresse} onChange={(e) => setPlanoInteresse(e.target.value as Plano | '')}>
              <option value="">Não informado</option>
              {(Object.keys(PLANO_LABEL) as Plano[]).map((p) => (
                <option key={p} value={p}>{PLANO_LABEL[p]}</option>
              ))}
            </select>
          </label>
        </div>
        <label className={styles.formField}>
          Mensagem
          <textarea
            className={styles.textarea}
            value={mensagem}
            onChange={(e) => setMensagem(e.target.value)}
            rows={3}
            maxLength={500}
          />
        </label>

        {error && <div className={styles.error}>{error}</div>}

        <div className={styles.formActions}>
          <button type="button" className={styles.cancelButton} onClick={onCancelar}>Cancelar</button>
          <button type="submit" className={styles.newButton} disabled={submitting}>
            {submitting ? 'Salvando…' : 'Salvar lead'}
          </button>
        </div>
      </form>
    </Card>
  );
}

function AvancarLeadForm({ acao, vendedores, onAvancado, onCancelar }: {
  acao: Acao;
  vendedores: VendedorResumo[];
  onAvancado: () => void;
  onCancelar: () => void;
}) {
  const [vendedorId, setVendedorId] = useState(vendedores[0]?.id ?? '');
  const [motivoPerdido, setMotivoPerdido] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const titulo: Record<Acao['alvo'], string> = {
    EM_CONTATO: `Mover "${acao.lead.nome}" para Em contato`,
    DIAGNOSTICO: `Mover "${acao.lead.nome}" para Diagnóstico`,
    PROPOSTA: `Avançar "${acao.lead.nome}" para Proposta`,
    PERDIDO: `Marcar "${acao.lead.nome}" como Perdido`,
  };

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await apiClient.patch(`/admin/comercial/leads/${acao.lead.id}/avancar`, {
        novoStatus: acao.alvo,
        vendedorId: acao.alvo === 'EM_CONTATO' ? vendedorId : undefined,
        motivoPerdido: acao.alvo === 'PERDIDO' ? motivoPerdido : undefined,
      });
      onAvancado();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível concluir a ação. Tente novamente.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <div className={styles.formTitle}>{titulo[acao.alvo]}</div>
      <form onSubmit={handleSubmit} className={styles.form}>
        {acao.alvo === 'EM_CONTATO' && (
          <label className={styles.formField}>
            Vendedor responsável
            <select className={styles.select} value={vendedorId} onChange={(e) => setVendedorId(e.target.value)} required>
              {vendedores.length === 0 && <option value="">Nenhum vendedor cadastrado</option>}
              {vendedores.map((v) => (
                <option key={v.id} value={v.id}>{v.nome}</option>
              ))}
            </select>
          </label>
        )}

        {acao.alvo === 'DIAGNOSTICO' && (
          <div className={styles.muted}>Confirma o avanço deste lead para a etapa de Diagnóstico?</div>
        )}

        {acao.alvo === 'PERDIDO' && (
          <label className={styles.formField}>
            Motivo
            <textarea
              className={styles.textarea}
              value={motivoPerdido}
              onChange={(e) => setMotivoPerdido(e.target.value)}
              rows={3}
              maxLength={255}
              required
            />
          </label>
        )}

        {acao.alvo === 'PROPOSTA' && (
          <div className={styles.muted}>Confirma o avanço deste lead para a etapa de Proposta?</div>
        )}

        {error && <div className={styles.error}>{error}</div>}

        <div className={styles.formActions}>
          <button type="button" className={styles.cancelButton} onClick={onCancelar}>Cancelar</button>
          <button type="submit" className={styles.actionButton} disabled={submitting}>
            {submitting ? 'Salvando…' : 'Confirmar'}
          </button>
        </div>
      </form>
    </Card>
  );
}

// M25 — "formulário único de venda" (pedido explícito do cliente: substitui as 2-3 planilhas
// separadas — venda por fora, venda de ingresso, credenciamento — por um único formulário que já
// distribui o dado pro financeiro/credenciamento).
const PARCELA_VAZIA = { valor: '', dataPrevista: '' };
const INGRESSO_VAZIO: {
  categoriaIngresso: CategoriaIngresso | '';
  nomeCredenciado: string;
  setor: string;
  almoco: boolean;
  nomeEmpresa: string;
  telefone: string;
  email: string;
} = {
  categoriaIngresso: '', nomeCredenciado: '', setor: '', almoco: false, nomeEmpresa: '', telefone: '', email: '',
};

function FecharVendaForm({ lead, onFechado, onCancelar }: {
  lead: Lead;
  onFechado: () => void;
  onCancelar: () => void;
}) {
  const [produtoVenda, setProdutoVenda] = useState<ProdutoVenda | ''>('');
  const [origemVenda, setOrigemVenda] = useState<OrigemVenda | ''>('');
  const [valorTotalVenda, setValorTotalVenda] = useState('');
  const [valorPagoNoAto, setValorPagoNoAto] = useState('');
  const [formaPagamento, setFormaPagamento] = useState<FormaPagamento | ''>('');
  const [parcelas, setParcelas] = useState<{ valor: string; dataPrevista: string }[]>([]);
  const [eventoId, setEventoId] = useState('');
  const [ingressos, setIngressos] = useState<typeof INGRESSO_VAZIO[]>([]);
  const [eventos, setEventos] = useState<EventoVendaResumo[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const isIngresso = produtoVenda === 'INGRESSO_EVENTO';

  useEffect(() => {
    if (!isIngresso || eventos.length > 0) return;
    apiClient.get<EventoVendaResumo[]>('/admin/comercial/eventos')
      .then((res) => setEventos(res.data))
      .catch(() => setError('Não foi possível carregar a lista de eventos.'));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isIngresso]);

  function adicionarParcela() {
    setParcelas((atual) => [...atual, { ...PARCELA_VAZIA }]);
  }

  function removerParcela(index: number) {
    setParcelas((atual) => atual.filter((_, i) => i !== index));
  }

  function atualizarParcela(index: number, campo: 'valor' | 'dataPrevista', valor: string) {
    setParcelas((atual) => atual.map((p, i) => (i === index ? { ...p, [campo]: valor } : p)));
  }

  function adicionarIngresso() {
    setIngressos((atual) => [...atual, { ...INGRESSO_VAZIO }]);
  }

  function removerIngresso(index: number) {
    setIngressos((atual) => atual.filter((_, i) => i !== index));
  }

  function atualizarIngresso<K extends keyof typeof INGRESSO_VAZIO>(index: number, campo: K, valor: typeof INGRESSO_VAZIO[K]) {
    setIngressos((atual) => atual.map((ing, i) => (i === index ? { ...ing, [campo]: valor } : ing)));
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);

    if (isIngresso && (!eventoId || ingressos.length === 0)) {
      setError('Venda de ingresso exige um evento e pelo menos um ingresso.');
      return;
    }
    if (isIngresso && ingressos.some((ing) => !ing.categoriaIngresso || !ing.nomeCredenciado)) {
      setError('Cada ingresso precisa de categoria e nome do credenciado.');
      return;
    }

    setSubmitting(true);
    try {
      const request: FecharVendaRequest = {
        produtoVenda: produtoVenda as ProdutoVenda,
        origemVenda: origemVenda as OrigemVenda,
        valorTotalVenda: Number(valorTotalVenda),
        valorPagoNoAto: valorPagoNoAto ? Number(valorPagoNoAto) : null,
        formaPagamento: formaPagamento as FormaPagamento,
        parcelas: parcelas.length > 0
          ? parcelas.map((p, i): ParcelaVendaRequest => ({ numero: i + 1, valor: Number(p.valor), dataPrevista: p.dataPrevista }))
          : null,
        eventoId: isIngresso ? eventoId : null,
        ingressos: isIngresso
          ? ingressos.map((ing): VendaIngressoRequest => ({
              categoriaIngresso: ing.categoriaIngresso as CategoriaIngresso,
              nomeCredenciado: ing.nomeCredenciado,
              setor: ing.setor || null,
              almoco: ing.almoco,
              nomeEmpresa: ing.nomeEmpresa || null,
              telefone: ing.telefone || null,
              email: ing.email || null,
            }))
          : null,
      };
      await apiClient.post(`/admin/comercial/leads/${lead.id}/fechar-venda`, request);
      onFechado();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível fechar a venda. Confira os dados.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <div className={styles.formTitle}>Fechar venda: {lead.nome}</div>
      <form onSubmit={handleSubmit} className={styles.form}>
        <div className={styles.formRow}>
          <label className={styles.formField}>
            Produto vendido
            <select className={styles.select} value={produtoVenda}
                    onChange={(e) => setProdutoVenda(e.target.value as ProdutoVenda | '')} required>
              <option value="">Selecione o produto</option>
              {(Object.keys(PRODUTO_VENDA_LABEL) as ProdutoVenda[]).map((p) => (
                <option key={p} value={p}>{PRODUTO_VENDA_LABEL[p]}</option>
              ))}
            </select>
          </label>
          <label className={styles.formField}>
            Origem da venda
            <select className={styles.select} value={origemVenda}
                    onChange={(e) => setOrigemVenda(e.target.value as OrigemVenda | '')} required>
              <option value="">Selecione a origem</option>
              {(Object.keys(ORIGEM_VENDA_LABEL) as OrigemVenda[]).map((o) => (
                <option key={o} value={o}>{ORIGEM_VENDA_LABEL[o]}</option>
              ))}
            </select>
          </label>
        </div>

        <div className={styles.formRow}>
          <label className={styles.formField}>
            Valor total da venda
            <input className={styles.textInput} type="number" min="0" step="0.01"
                   value={valorTotalVenda} onChange={(e) => setValorTotalVenda(e.target.value)} required />
          </label>
          <label className={styles.formField}>
            Valor pago no ato
            <input className={styles.textInput} type="number" min="0" step="0.01"
                   value={valorPagoNoAto} onChange={(e) => setValorPagoNoAto(e.target.value)} />
          </label>
          <label className={styles.formField}>
            Forma de pagamento
            <select className={styles.select} value={formaPagamento}
                    onChange={(e) => setFormaPagamento(e.target.value as FormaPagamento | '')} required>
              <option value="">Selecione</option>
              {(Object.keys(FORMA_PAGAMENTO_LABEL) as FormaPagamento[]).map((f) => (
                <option key={f} value={f}>{FORMA_PAGAMENTO_LABEL[f]}</option>
              ))}
            </select>
          </label>
        </div>

        {isIngresso && (
          <>
            <label className={styles.formField}>
              Evento do ingresso
              <select className={styles.select} value={eventoId} onChange={(e) => setEventoId(e.target.value)} required>
                <option value="">Selecione o evento</option>
                {eventos.map((ev) => (
                  <option key={ev.id} value={ev.id}>
                    {ev.titulo}{ev.vagasDisponiveis != null ? ` (${ev.vagasDisponiveis} vagas)` : ''}
                  </option>
                ))}
              </select>
            </label>

            {ingressos.map((ing, i) => (
              <div key={i} className={styles.formRow} style={{ flexDirection: 'column', gap: 8 }}>
                <div className={styles.formRow}>
                  <label className={styles.formField}>
                    Categoria
                    <select className={styles.select} value={ing.categoriaIngresso}
                            onChange={(e) => atualizarIngresso(i, 'categoriaIngresso', e.target.value as CategoriaIngresso)} required>
                      <option value="">Selecione</option>
                      {(Object.keys(CATEGORIA_INGRESSO_LABEL) as CategoriaIngresso[]).map((c) => (
                        <option key={c} value={c}>{CATEGORIA_INGRESSO_LABEL[c]}</option>
                      ))}
                    </select>
                  </label>
                  <label className={styles.formField}>
                    Nome do credenciado
                    <input className={styles.textInput} value={ing.nomeCredenciado}
                           onChange={(e) => atualizarIngresso(i, 'nomeCredenciado', e.target.value)} required maxLength={120} />
                  </label>
                  <label className={styles.formField}>
                    Setor
                    <input className={styles.textInput} value={ing.setor}
                           onChange={(e) => atualizarIngresso(i, 'setor', e.target.value)} maxLength={100} />
                  </label>
                  <label className={styles.formField} style={{ flex: '0 0 auto', flexDirection: 'row', alignItems: 'center', gap: 8 }}>
                    <input type="checkbox" checked={ing.almoco} onChange={(e) => atualizarIngresso(i, 'almoco', e.target.checked)} />
                    Almoço
                  </label>
                  <button type="button" className={styles.actionButtonDanger} onClick={() => removerIngresso(i)}>Remover</button>
                </div>
                <div className={styles.formRow}>
                  <label className={styles.formField}>
                    Empresa (opcional)
                    <input className={styles.textInput} value={ing.nomeEmpresa}
                           onChange={(e) => atualizarIngresso(i, 'nomeEmpresa', e.target.value)} maxLength={255} />
                  </label>
                  <label className={styles.formField}>
                    Telefone (opcional)
                    <input className={styles.textInput} value={ing.telefone}
                           onChange={(e) => atualizarIngresso(i, 'telefone', e.target.value)} maxLength={20} />
                  </label>
                  <label className={styles.formField}>
                    E-mail (opcional)
                    <input className={styles.textInput} type="email" value={ing.email}
                           onChange={(e) => atualizarIngresso(i, 'email', e.target.value)} maxLength={255} />
                  </label>
                </div>
              </div>
            ))}
            <button type="button" className={styles.cancelButton} onClick={adicionarIngresso} style={{ alignSelf: 'flex-start' }}>
              + Adicionar ingresso
            </button>
          </>
        )}

        {parcelas.map((p, i) => (
          <div key={i} className={styles.formRow}>
            <label className={styles.formField}>
              Parcela {i + 1} — valor
              <input className={styles.textInput} type="number" min="0" step="0.01"
                     value={p.valor} onChange={(e) => atualizarParcela(i, 'valor', e.target.value)} required />
            </label>
            <label className={styles.formField}>
              Data prevista
              <input className={styles.textInput} type="date"
                     value={p.dataPrevista} onChange={(e) => atualizarParcela(i, 'dataPrevista', e.target.value)} required />
            </label>
            <button type="button" className={styles.actionButtonDanger} onClick={() => removerParcela(i)}>Remover</button>
          </div>
        ))}
        <button type="button" className={styles.cancelButton} onClick={adicionarParcela} style={{ alignSelf: 'flex-start' }}>
          + Adicionar parcela
        </button>

        {error && <div className={styles.error}>{error}</div>}

        <div className={styles.formActions}>
          <button type="button" className={styles.cancelButton} onClick={onCancelar}>Cancelar</button>
          <button type="submit" className={styles.actionButton} disabled={submitting}>
            {submitting ? 'Salvando…' : 'Confirmar venda'}
          </button>
        </div>
      </form>
    </Card>
  );
}
