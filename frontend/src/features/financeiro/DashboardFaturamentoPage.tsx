import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiClient } from '../../shared/lib/apiClient';
import { Card } from '../../shared/components/Card';
import type { ConciliacaoVenda, DashboardFaturamentoResponse, TransferenciaBancaria } from '../../shared/lib/types';
import { formatBRL } from '../../shared/lib/format';
import { PeriodoPicker } from '../../shared/components/PeriodoPicker';
import { Tooltip } from '../../shared/components/Tooltip';
import styles from './DashboardFaturamentoPage.module.css';

// Pedido do Marcos (22/07/2026) — composição agora é por CategoriaFinanceira (nome, já
// legível — "Mentoria Contínua", "Produtos Digitais" etc.), não por OrigemReceita (só 4 valores
// fixos, "categórico" no CSS token). Paleta cíclica por posição (maior valor primeiro): a
// categoria com mais receita sempre cai no dourado, sem precisar mapear nome -> cor à mão pra
// cada categoria nova que a planilha real possa trazer.
const PALETA_COMPOSICAO = ['var(--gold)', 'var(--success)', 'var(--info)', 'var(--violet)', 'var(--danger)'];

// Pedido do Marcos (22/07/2026) — "o Dashboard precisa refletir tudo que está sendo mostrado nas
// outras abas; acessar uma aba é só ver em mais detalhe aquilo que já está no Dashboard". Os 4
// cards de "Resumo do Financeiro" abaixo espelham DRE/Lançamentos/Caixa/Conciliação e navegam pra
// aba correspondente ao clicar.
export function DashboardFaturamentoPage() {
  const now = new Date();
  const navigate = useNavigate();
  const [ano, setAno] = useState(now.getFullYear());
  const [mes, setMes] = useState(now.getMonth() + 1);
  const [dashboard, setDashboard] = useState<DashboardFaturamentoResponse | null>(null);
  const [vendasEmAtraso, setVendasEmAtraso] = useState<number | null>(null);
  const [transferenciasNoMes, setTransferenciasNoMes] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setDashboard(null);
    apiClient
      .get<DashboardFaturamentoResponse>('/admin/financeiro/dashboard-faturamento', { params: { ano, mes } })
      .then((res) => setDashboard(res.data))
      .catch(() => setError('Não foi possível carregar o dashboard de faturamento.'));
  }, [ano, mes]);

  // Conciliação (ver ConciliacaoVenda.emAtraso) não tem escopo de período — mesmo raciocínio de
  // lancamentosPendentes/Vencidos, contado à parte pra não criar financeiro->comercial no backend
  // (ConciliacaoService vive em `comercial`, que já depende de `financeiro`; o inverso criaria
  // ciclo de pacote).
  useEffect(() => {
    apiClient.get<ConciliacaoVenda[]>('/admin/financeiro/conciliacao')
      .then((res) => setVendasEmAtraso(res.data.filter((v) => v.emAtraso).length))
      .catch(() => setVendasEmAtraso(null));
  }, []);

  // Transferências entre contas são a outra metade da aba Caixa (ver CaixaPage) — sem isso, o
  // card "Caixa" do resumo não reflete tudo que está naquela aba (achado do Marcos, 22/07/2026).
  useEffect(() => {
    const de = `${ano}-${String(mes).padStart(2, '0')}-01`;
    const ate = new Date(ano, mes, 0).toISOString().slice(0, 10);
    apiClient.get<TransferenciaBancaria[]>('/admin/financeiro/transferencias', { params: { de, ate } })
      .then((res) => setTransferenciasNoMes(res.data.length))
      .catch(() => setTransferenciasNoMes(null));
  }, [ano, mes]);

  const total = dashboard?.composicao.reduce((acc, c) => acc + c.valor, 0) ?? 0;

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
                <Tooltip text="Soma de todos os lançamentos de receita Realizados no mês/ano selecionado.">Faturamento do mês</Tooltip>
              </div>
              <div className={styles.kpiValue}>{formatBRL(dashboard.faturamentoMensal)}</div>
            </Card>
            <Card style={{ padding: 18 }}>
              <div className={styles.kpiLabel}>
                <Tooltip text="Receita mensal recorrente. Contratos de mentoria em andamento, sem contar receitas pontuais (Loja, eventos).">Receita recorrente (MRR)</Tooltip>
              </div>
              <div className={styles.kpiValue}>{formatBRL(dashboard.mrr)}</div>
            </Card>
            <Card style={{ padding: 18 }}>
              <div className={styles.kpiLabel}>
                <Tooltip text="% de queda do MRR em relação ao mês anterior.">Churn de receita</Tooltip>
              </div>
              <div className={styles.kpiValue} style={{ color: dashboard.churnPct > 0 ? 'var(--danger)' : 'var(--success)' }}>
                {dashboard.churnPct.toFixed(1)}%
              </div>
              <div className={styles.kpiHint}>{dashboard.churnPct > 0 ? 'queda de MRR vs. mês anterior' : 'sem queda de MRR'}</div>
            </Card>
          </div>

          <div className={styles.sectionTitle} style={{ marginBottom: 12 }}>
            <Tooltip text="Atalho pra DRE, Lançamentos, Caixa e Conciliação. Clique num card pra ver o detalhe na aba correspondente.">Resumo do Financeiro</Tooltip>
          </div>
          <div className={styles.resumoGrid}>
            <ResumoCard
              titulo="DRE"
              destaque={formatBRL(dashboard.resultadoDre)}
              corDestaque={dashboard.resultadoDre >= 0 ? 'var(--success)' : 'var(--danger)'}
              legenda={dashboard.resultadoDre >= 0 ? 'Resultado do mês (lucro)' : 'Resultado do mês (prejuízo)'}
              onClick={() => navigate('/admin/financeiro/dre')}
            />
            <ResumoCard
              titulo="Lançamentos"
              destaque={`${dashboard.lancamentosPendentes + dashboard.lancamentosVencidos}`}
              corDestaque={dashboard.lancamentosVencidos > 0 ? 'var(--danger)' : 'var(--text)'}
              legenda={`${dashboard.lancamentosPendentes} previsto(s)${dashboard.lancamentosVencidos > 0 ? ` · ${dashboard.lancamentosVencidos} vencido(s)` : ''}`}
              onClick={() => navigate('/admin/financeiro/lancamentos')}
            />
            <ResumoCard
              titulo="Caixa"
              destaque={formatBRL(dashboard.saldoCaixaAtual)}
              legenda={`Saldo final do mês${transferenciasNoMes != null && transferenciasNoMes > 0 ? ` · ${transferenciasNoMes} transferência(s)` : ''}`}
              onClick={() => navigate('/admin/financeiro/caixa')}
            />
            <ResumoCard
              titulo="Conciliação"
              destaque={vendasEmAtraso === null ? '—' : `${vendasEmAtraso}`}
              corDestaque={vendasEmAtraso != null && vendasEmAtraso > 0 ? 'var(--danger)' : 'var(--success)'}
              legenda={vendasEmAtraso != null && vendasEmAtraso > 0 ? 'venda(s) com parcela em atraso' : 'nenhuma venda em atraso'}
              onClick={() => navigate('/admin/financeiro/conciliacao')}
            />
          </div>

          <Card style={{ padding: '20px 22px' }}>
            <div className={styles.sectionTitle}>
              <Tooltip text="Quanto cada categoria de receita (Mentoria Contínua, Mentoria Individual, Eventos, Patrocínio, Produtos Digitais etc.) contribuiu no período. Mesma granularidade da planilha real 'DRE Financeira Saw'.">Composição da receita</Tooltip>
            </div>
            <div className={styles.composicaoList}>
              {dashboard.composicao.length === 0 && (
                <div className={styles.empty}>Sem receitas realizadas neste período.</div>
              )}
              {dashboard.composicao.map((c, i) => {
                const pct = total === 0 ? 0 : (c.valor / total) * 100;
                const cor = PALETA_COMPOSICAO[i % PALETA_COMPOSICAO.length];
                return (
                  <div key={c.categoria} className={styles.composicaoRow}>
                    <div className={styles.composicaoHeader}>
                      <span className={styles.composicaoLabel}>
                        <span className={styles.dot} style={{ background: cor }} />
                        {c.categoria}
                      </span>
                      <span className={styles.composicaoValue}>
                        {formatBRL(c.valor)} <span className={styles.composicaoPct}>({pct.toFixed(0)}%)</span>
                      </span>
                    </div>
                    <div className={styles.track}>
                      <div className={styles.fill} style={{ width: `${pct}%`, background: cor }} />
                    </div>
                  </div>
                );
              })}
            </div>
          </Card>

          {dashboard.resultadoPorEvento.length > 0 && (
            <Card style={{ padding: '20px 22px', marginTop: 16 }}>
              <div className={styles.sectionTitle}>
                <Tooltip text="Receita menos despesa de cada evento Realizado no período. Mesma riqueza da planilha 'Eventos - Despesas e Receitas' do cliente. Comercial mostra quantidade/valor de ingresso vendido; aqui é o resultado financeiro completo do evento (inclui despesas como estrutura, alimentação etc.).">
                  Resultado por evento
                </Tooltip>
              </div>
              <div className={styles.composicaoList}>
                {dashboard.resultadoPorEvento.map((ev) => (
                  <div key={ev.eventoId} className={styles.composicaoRow} data-testid="evento-resultado-row">
                    <div className={styles.composicaoHeader}>
                      <span className={styles.composicaoLabel}>{ev.eventoTitulo}</span>
                      <span
                        className={styles.composicaoValue}
                        style={{ color: ev.resultado >= 0 ? 'var(--success)' : 'var(--danger)' }}
                      >
                        {formatBRL(ev.resultado)}
                      </span>
                    </div>
                    <div className={styles.muted}>
                      Receita {formatBRL(ev.receitaTotal)} · Despesa {formatBRL(ev.despesaTotal)}
                    </div>
                  </div>
                ))}
              </div>
            </Card>
          )}
        </>
      )}
    </div>
  );
}

// Card clicável — leva pra aba correspondente ("a função de acessar uma aba é apenas de ver em
// mais detalhes aquilo que está no dashboard", pedido do Marcos 22/07/2026).
function ResumoCard({ titulo, destaque, corDestaque, legenda, onClick }: {
  titulo: string;
  destaque: string;
  corDestaque?: string;
  legenda: string;
  onClick: () => void;
}) {
  return (
    <div
      className={styles.resumoCard}
      role="button"
      tabIndex={0}
      onClick={onClick}
      onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') onClick(); }}
    >
      <Card style={{ padding: 16 }}>
        <div className={styles.resumoTitulo}>{titulo}</div>
        <div className={styles.resumoDestaque} style={corDestaque ? { color: corDestaque } : undefined}>{destaque}</div>
        <div className={styles.resumoLegenda}>{legenda}</div>
      </Card>
    </div>
  );
}
