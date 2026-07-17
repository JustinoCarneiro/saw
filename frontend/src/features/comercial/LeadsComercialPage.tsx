import { type FormEvent, useEffect, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { Card } from '../../shared/components/Card';
import { CsvImportExport } from '../../shared/components/CsvImportExport';
import { DataGrid, DataGridRow } from '../../shared/components/DataGrid';
import { Pill } from '../../shared/components/Pill';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import type { Lead, Plano, StatusLead, VendedorResumo } from '../../shared/lib/types';
import styles from './LeadsComercialPage.module.css';

const COLUMNS = '1.4fr 1.6fr 1fr 1fr 1.3fr 1.6fr';

const STATUS_LABEL: Record<StatusLead, { label: string; bg: string; color: string }> = {
  SOLICITACAO: { label: 'Solicitação', bg: 'var(--line)', color: 'var(--text-soft)' },
  EM_CONTATO: { label: 'Em contato', bg: 'var(--info-bg)', color: 'var(--info)' },
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

type Acao =
  | { alvo: 'EM_CONTATO'; lead: Lead }
  | { alvo: 'PROPOSTA'; lead: Lead }
  | { alvo: 'FECHADO'; lead: Lead }
  | { alvo: 'PERDIDO'; lead: Lead };

export function LeadsComercialPage() {
  const [status, setStatus] = useState<StatusLead | ''>('');
  const [vendedorId, setVendedorId] = useState('');
  const [leads, setLeads] = useState<Lead[] | null>(null);
  const [vendedores, setVendedores] = useState<VendedorResumo[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [acao, setAcao] = useState<Acao | null>(null);
  const [showCriarForm, setShowCriarForm] = useState(false);

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
              </div>
              <div className={styles.muted}>{lead.vendedor?.nome ?? '—'}</div>
              <div className={styles.acoes}>
                {lead.status === 'SOLICITACAO' && (
                  <button className={styles.actionButton} onClick={() => setAcao({ alvo: 'EM_CONTATO', lead })}>
                    Mover p/ Em contato
                  </button>
                )}
                {lead.status === 'EM_CONTATO' && (
                  <button className={styles.actionButton} onClick={() => setAcao({ alvo: 'PROPOSTA', lead })}>
                    Avançar p/ Proposta
                  </button>
                )}
                {lead.status === 'PROPOSTA' && (
                  <button className={styles.actionButton} onClick={() => setAcao({ alvo: 'FECHADO', lead })}>
                    Fechar venda
                  </button>
                )}
                {(lead.status === 'SOLICITACAO' || lead.status === 'EM_CONTATO' || lead.status === 'PROPOSTA') && (
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
  const [planoFechado, setPlanoFechado] = useState<Plano | ''>('');
  const [motivoPerdido, setMotivoPerdido] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const titulo: Record<Acao['alvo'], string> = {
    EM_CONTATO: `Mover "${acao.lead.nome}" para Em contato`,
    PROPOSTA: `Avançar "${acao.lead.nome}" para Proposta`,
    FECHADO: `Fechar venda: ${acao.lead.nome}`,
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
        planoFechado: acao.alvo === 'FECHADO' ? planoFechado : undefined,
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

        {acao.alvo === 'FECHADO' && (
          <label className={styles.formField}>
            Plano fechado
            <select className={styles.select} value={planoFechado} onChange={(e) => setPlanoFechado(e.target.value as Plano | '')} required>
              <option value="">Selecione o plano</option>
              {(Object.keys(PLANO_LABEL) as Plano[]).map((p) => (
                <option key={p} value={p}>{PLANO_LABEL[p]}</option>
              ))}
            </select>
          </label>
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
