import { type ChangeEvent, useEffect, useRef, useState } from 'react';
import { isAxiosError } from 'axios';
import { apiClient } from '../../shared/lib/apiClient';
import { Card } from '../../shared/components/Card';
import { PeriodoPicker } from '../../shared/components/PeriodoPicker';
import { Tooltip } from '../../shared/components/Tooltip';
import { LOJA_ADMIN_PAUSADA } from '../../shared/lib/featureFlags';
import type { DashboardComercialResponse, EventoVendaResumo, ImportResultResponse, StatusLead } from '../../shared/lib/types';
import { formatBRL } from '../../shared/lib/format';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import { PRODUTO_VENDA_LABEL } from '../../shared/lib/labels';
import styles from './DashboardComercialPage.module.css';

const STATUS_LABEL: Record<StatusLead, string> = {
  SOLICITACAO: 'Solicitação',
  EM_CONTATO: 'Em contato',
  DIAGNOSTICO: 'Diagnóstico',
  PROPOSTA: 'Proposta',
  FECHADO: 'Fechado',
  PERDIDO: 'Perdido',
};

const STATUS_COLOR: Record<StatusLead, string> = {
  SOLICITACAO: 'var(--text-faint)',
  EM_CONTATO: 'var(--info)',
  DIAGNOSTICO: 'var(--info)',
  PROPOSTA: 'var(--gold)',
  FECHADO: 'var(--success)',
  PERDIDO: 'var(--danger)',
};

export function DashboardComercialPage() {
  const now = new Date();
  const [ano, setAno] = useState(now.getFullYear());
  const [mes, setMes] = useState(now.getMonth() + 1);
  const [dashboard, setDashboard] = useState<DashboardComercialResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setDashboard(null);
    apiClient
      .get<DashboardComercialResponse>('/admin/comercial/dashboard', { params: { ano, mes } })
      .then((res) => setDashboard(res.data))
      .catch(() => setError('Não foi possível carregar o dashboard comercial.'));
  }, [ano, mes]);

  const maiorQuantidade = Math.max(1, ...(dashboard?.funil.map((f) => f.quantidade) ?? [1]));

  return (
    <div>
      <div className={styles.toolbar}>
        <PeriodoPicker ano={ano} mes={mes} onChange={(a, m) => { setAno(a); setMes(m); }} />
      </div>

      {error && <div className={styles.error}>{error}</div>}

      {dashboard && (
        <>
          <div className={styles.kpis}>
            <Card style={{ padding: 18 }}>
              <div className={styles.kpiLabel}>
                <Tooltip text="Leads que fecharam venda de contrato de mentoria (Lead FECHADO) dentro do mês/ano selecionado.">Novos mentorados no mês</Tooltip>
              </div>
              <div className={styles.kpiValue}>{dashboard.novosMentoradosNoMes}</div>
            </Card>
            <Card style={{ padding: 18 }}>
              <div className={styles.kpiLabel}>
                <Tooltip text="% de leads que chegaram a FECHADO em relação ao total de leads do funil no período.">Taxa de conversão</Tooltip>
              </div>
              <div className={styles.kpiValue}>{dashboard.taxaConversaoPct.toFixed(1)}%</div>
            </Card>
            <Card style={{ padding: 18 }}>
              <div className={styles.kpiLabel}>
                <Tooltip text="Receita mensal recorrente. Contratos de mentoria em andamento, sem contar receitas pontuais (Loja, eventos).">Receita recorrente (MRR)</Tooltip>
              </div>
              <div className={styles.kpiValue}>{formatBRL(dashboard.mrr)}</div>
              <div className={styles.kpiHint} style={{ color: dashboard.variacaoMrrPct >= 0 ? 'var(--success)' : 'var(--danger)' }}>
                {dashboard.variacaoMrrPct >= 0 ? '+' : ''}{dashboard.variacaoMrrPct.toFixed(1)}% vs. mês anterior
              </div>
            </Card>
            <Card style={{ padding: 18 }}>
              <div className={styles.kpiLabel}>
                <Tooltip text={LOJA_ADMIN_PAUSADA
                  ? 'Total de pedidos Pagos da Loja SAW no período. A Loja está temporariamente pausada (decisão do cliente). Este número fica zerado até ela reabrir.'
                  : 'Total de pedidos Pagos da Loja SAW no período.'}>
                  Vendas da loja
                </Tooltip>
              </div>
              <div className={styles.kpiValue}>{formatBRL(dashboard.vendasLoja)}</div>
              {LOJA_ADMIN_PAUSADA && <div className={styles.kpiHint}>Loja pausada</div>}
            </Card>
          </div>

          <Card style={{ padding: '20px 22px' }}>
            <div className={styles.sectionTitle}>
              <Tooltip text="Quantos leads estão em cada etapa (Solicitação → Em contato → Diagnóstico → Proposta → Fechado), no período selecionado.">Funil de vendas</Tooltip>
            </div>
            <div className={styles.funilList}>
              {dashboard.funil.map((f) => {
                const pct = (f.quantidade / maiorQuantidade) * 100;
                return (
                  <div key={f.status} className={styles.funilRow}>
                    <div className={styles.funilHeader}>
                      <span className={styles.funilLabel}>
                        <span className={styles.dot} style={{ background: STATUS_COLOR[f.status] }} />
                        {STATUS_LABEL[f.status]}
                      </span>
                      <span className={styles.funilValue}>{f.quantidade}</span>
                    </div>
                    <div className={styles.track}>
                      <div className={styles.fill} style={{ width: `${pct}%`, background: STATUS_COLOR[f.status] }} />
                    </div>
                  </div>
                );
              })}
            </div>
          </Card>

          {dashboard.vendaIngressos.length > 0 && (
            <Card style={{ padding: '20px 22px', marginTop: 16 }}>
              <div className={styles.sectionTitle}>
                <Tooltip text="Quantidade e valor líquido vendido de ingressos, por evento, no período selecionado.">Venda de ingressos por evento</Tooltip>
              </div>
              <div className={styles.funilList}>
                {dashboard.vendaIngressos.map((v) => (
                  <div key={v.eventoId} className={styles.funilRow}>
                    <div className={styles.funilHeader}>
                      <span className={styles.funilLabel}>{v.eventoTitulo}</span>
                      <span className={styles.funilValue}>
                        {v.quantidadeVendida}{v.quantidadeTotal != null ? ` / ${v.quantidadeTotal}` : ''} — {formatBRL(v.valorLiquido)}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </Card>
          )}

          {dashboard.vendaPorFora.length > 0 && (
            <Card style={{ padding: '20px 22px', marginTop: 16 }}>
              <div className={styles.sectionTitle}>
                <Tooltip text="Vendas fechadas no período, por produto. Exclui ingresso de evento, que já tem a seção própria acima.">Venda por fora</Tooltip>
              </div>
              <div className={styles.funilList}>
                {dashboard.vendaPorFora.map((v) => (
                  <div key={v.produto} className={styles.funilRow}>
                    <div className={styles.funilHeader}>
                      <span className={styles.funilLabel}>{PRODUTO_VENDA_LABEL[v.produto]}</span>
                      <span className={styles.funilValue}>
                        {v.quantidade} venda(s) — {formatBRL(v.valorTotal)}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </Card>
          )}

          <ImportarHistoricoIngressos />
        </>
      )}
    </div>
  );
}

// Change request pós-MVP ("importação de planilhas de eventos passados pra popular histórico",
// reunião 17/07/2026) — uma aba por evento na planilha real, por isso o evento é escolhido aqui
// (não é coluna do CSV, ver VendaIngressoCsvService no backend). Diferente de CsvImportExport
// (que sempre exige exportUrl): este import não tem export equivalente, é só backfill histórico.
function ImportarHistoricoIngressos() {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [eventos, setEventos] = useState<EventoVendaResumo[]>([]);
  const [eventoId, setEventoId] = useState('');
  const [importando, setImportando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [resultado, setResultado] = useState<ImportResultResponse | null>(null);

  useEffect(() => {
    apiClient.get<EventoVendaResumo[]>('/admin/comercial/eventos/historico')
      .then((res) => setEventos(res.data))
      .catch(() => setErro('Não foi possível carregar a lista de eventos já realizados.'));
  }, []);

  async function importar(e: ChangeEvent<HTMLInputElement>) {
    const arquivo = e.target.files?.[0];
    if (!arquivo || !eventoId) return;
    setImportando(true);
    setErro(null);
    setResultado(null);
    const form = new FormData();
    form.append('arquivo', arquivo);
    try {
      const res = await apiClient.post<ImportResultResponse>(`/admin/comercial/eventos/${eventoId}/ingressos/import`, form);
      setResultado(res.data);
    } catch (err) {
      if (isAxiosError(err) && err.response?.status === 422 && Array.isArray(err.response.data?.erros)) {
        setResultado(err.response.data as ImportResultResponse);
      } else {
        setErro(getApiErrorMessage(err, 'Não foi possível importar o arquivo.'));
      }
    } finally {
      setImportando(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  }

  return (
    <Card style={{ padding: '20px 22px', marginTop: 16 }}>
      <div className={styles.sectionHeader}>
        <div className={styles.sectionTitle} style={{ marginBottom: 0 }}>
          <Tooltip text="Backfill de eventos já realizados antes do sistema existir. Não afeta vendas de eventos futuros, que são registradas pelo fluxo normal de inscrição.">Importar histórico de vendas de ingresso</Tooltip>
        </div>
        <div className={styles.importRow}>
          <select
            className={styles.select}
            aria-label="Selecione o evento"
            value={eventoId}
            onChange={(e) => setEventoId(e.target.value)}
          >
            <option value="">{eventos.length === 0 ? 'Nenhum evento realizado ainda' : 'Selecione o evento…'}</option>
            {eventos.map((ev) => (
              <option key={ev.id} value={ev.id}>{ev.titulo}</option>
            ))}
          </select>
          <button
            type="button"
            className={styles.outlineButton}
            onClick={() => fileInputRef.current?.click()}
            disabled={!eventoId || importando}
          >
            {importando ? 'Importando…' : 'Importar CSV'}
          </button>
          <input
            ref={fileInputRef}
            type="file"
            accept=".csv"
            hidden
            onChange={importar}
            data-testid="importar-ingressos-input"
          />
        </div>
      </div>
      <div className={styles.muted}>
        Renomeie as colunas do CSV exportado da planilha "Vendas Eventos" pra estes nomes antes de
        importar: nomeAluno, quantidadeIngressos, valorLiquidoIngresso, tipoIngresso, email
        (origemVenda, nomeEmpresa, telefone são opcionais).
      </div>
      <div className={styles.muted} style={{ marginTop: 4 }}>
        Este import só registra os ingressos (pra relatório de Eventos/Credenciamento) — <strong>não
        lança receita no Financeiro</strong>. Se ainda não importou o financeiro desses meses, faça
        isso separadamente em Lançamentos.
      </div>

      {erro && <div className={styles.erros}>{erro}</div>}
      {resultado && resultado.erros.length === 0 && (
        <div className={styles.sucesso}>{resultado.importados} ingresso(s) importado(s) com sucesso.</div>
      )}
      {resultado && resultado.erros.length > 0 && (
        <div className={styles.erros}>
          <div>Nenhuma linha foi importada — corrija o arquivo e reenvie:</div>
          <ul>
            {resultado.erros.map((e) => (
              <li key={e.linha}>Linha {e.linha}: {e.motivo}</li>
            ))}
          </ul>
        </div>
      )}
    </Card>
  );
}
