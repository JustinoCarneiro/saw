import { useEffect, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { Card } from '../../shared/components/Card';
import type { CategoriaValor, DreResponse } from '../../shared/lib/types';
import { formatBRL, formatPct } from '../../shared/lib/format';
import { PeriodoPicker } from '../../shared/components/PeriodoPicker';
import { Tooltip } from '../../shared/components/Tooltip';
import styles from './DrePage.module.css';

// Paleta fixa e cíclica — categoria é texto livre (nome de CategoriaFinanceira), não enum, então
// não dá pra mapear cor 1:1 como ORIGEM_COLOR faz em DashboardFaturamentoPage.
const PALETA = ['var(--gold)', 'var(--info)', 'var(--success)', 'var(--danger)', 'var(--text-faint)'];

export function DrePage() {
  const now = new Date();
  const [ano, setAno] = useState(now.getFullYear());
  const [mes, setMes] = useState(now.getMonth() + 1);
  const [dre, setDre] = useState<DreResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setDre(null);
    apiClient
      .get<DreResponse>('/admin/financeiro/dre', { params: { ano, mes } })
      .then((res) => setDre(res.data))
      .catch(() => setError('Não foi possível carregar o DRE.'));
  }, [ano, mes]);

  const margemPct = dre && dre.receitaLiquida !== 0 ? (dre.resultado / dre.receitaLiquida) * 100 : 0;
  const lucro = (dre?.resultado ?? 0) >= 0;

  return (
    <div>
      <div className={styles.toolbar}>
        <PeriodoPicker ano={ano} mes={mes} onChange={(a, m) => { setAno(a); setMes(m); }} />
      </div>

      {error && <div className={styles.error}>{error}</div>}

      {dre && (
        <>
          <div className={styles.kpis}>
            <Card style={{ padding: 18 }}>
              <div className={styles.kpiLabel}>
                <Tooltip text="Soma de todas as receitas Realizadas no período, antes de deduções.">Receita Bruta</Tooltip>
              </div>
              <div className={styles.kpiValue}>{formatBRL(dre.receitaBruta)}</div>
            </Card>
            <Card style={{ padding: 18 }}>
              <div className={styles.kpiLabel}>
                <Tooltip text="Lançamentos de despesa categorizados como custo direto (ex.: insumo, matéria-prima), não operacional.">Custos</Tooltip>
              </div>
              <div className={styles.kpiValue} style={{ color: 'var(--danger)' }}>{formatBRL(dre.custos)}</div>
            </Card>
            <Card style={{ padding: 18 }}>
              <div className={styles.kpiLabel}>
                <Tooltip text="Despesas do dia a dia da operação (folha, aluguel, ferramentas etc.), separadas de Custos.">Despesas Operacionais</Tooltip>
              </div>
              <div className={styles.kpiValue} style={{ color: 'var(--danger)' }}>{formatBRL(dre.despesasOperacionais)}</div>
            </Card>
            <Card
              style={{
                padding: 18,
                background: lucro ? 'rgba(63,178,127,.05)' : 'rgba(229,87,63,.05)',
                borderColor: lucro ? 'rgba(63,178,127,.3)' : 'rgba(229,87,63,.3)',
              }}
            >
              <div className={styles.kpiLabel} style={{ color: lucro ? 'var(--success)' : 'var(--danger)' }}>
                <Tooltip text="Receita Líquida menos Custos menos Despesas Operacionais — o resultado final do período.">
                  {lucro ? 'Lucro' : 'Prejuízo'}
                </Tooltip>
              </div>
              <div className={styles.kpiValue} style={{ color: lucro ? 'var(--success)' : 'var(--danger)' }}>
                {formatBRL(dre.resultado)}
              </div>
              <div className={styles.kpiHint} style={{ color: lucro ? 'var(--success)' : 'var(--danger)' }}>
                Margem {margemPct.toFixed(1)}%
              </div>
            </Card>
          </div>

          <Card style={{ overflow: 'hidden' }}>
            <div className={styles.header}>Demonstrativo Estruturado — {dre.periodo}</div>
            <DreLinha label="Receita Bruta" valor={dre.receitaBruta} />
            <DreLinha label="(–) Deduções" valor={-dre.deducoes} indent />
            <DreLinha label="= Receita Líquida" valor={dre.receitaLiquida} strong />
            <DreLinha label="(–) Custos" valor={-dre.custos} indent />
            <DreLinha label="(–) Despesas Operacionais" valor={-dre.despesasOperacionais} indent />
            <DreLinha label={lucro ? '= Resultado (Lucro)' : '= Resultado (Prejuízo)'} valor={dre.resultado} strong highlight={lucro} />
          </Card>

          {/* E14 — só aparece quando pelo menos uma categoria de despesa do período tem
              natureza (Fixa/Variável) preenchida; a maioria das categorias existentes ainda não
              tem esse campo populado, mostrar 0/0 sempre pareceria quebrado. */}
          {(dre.despesasFixas !== 0 || dre.despesasVariaveis !== 0) && (
            <Card style={{ marginTop: 16, padding: '20px 22px' }}>
              <div className={styles.sectionTitle}>
                <Tooltip text="Divisão das despesas por natureza (Fixa/Variável), cadastrada por categoria financeira. Só aparece quando alguma categoria do período já tem essa natureza preenchida.">Despesas fixas x variáveis</Tooltip>
              </div>
              <div className={styles.comparativo}>
                <div>
                  <div className={styles.kpiLabel}>
                    <Tooltip text="Despesas que não variam com o volume de vendas (ex.: aluguel).">Fixas</Tooltip>
                  </div>
                  <div className={styles.comparativoValue}>{formatBRL(dre.despesasFixas)}</div>
                </div>
                <div>
                  <div className={styles.kpiLabel}>
                    <Tooltip text="Despesas que variam conforme o volume de vendas (ex.: insumo).">Variáveis</Tooltip>
                  </div>
                  <div className={styles.comparativoValue}>{formatBRL(dre.despesasVariaveis)}</div>
                </div>
              </div>
            </Card>
          )}

          <div className={styles.kpis} style={{ gridTemplateColumns: '1fr 1fr', marginTop: 16 }}>
            <Card style={{ padding: '20px 22px' }}>
              <CategoriaBreakdown
                titulo="Receita por categoria"
                tooltip="Como a receita Realizada do período se divide entre as categorias financeiras cadastradas."
                itens={dre.receitaPorCategoria}
              />
            </Card>
            <Card style={{ padding: '20px 22px' }}>
              <CategoriaBreakdown
                titulo="Despesa por categoria"
                tooltip="Como Custos + Despesas Operacionais do período se dividem entre as categorias financeiras cadastradas."
                itens={dre.despesaPorCategoria}
              />
            </Card>
          </div>

          <Card style={{ marginTop: 16, padding: '20px 22px' }}>
            <div className={styles.sectionTitle}>
              <Tooltip text="Resultado (lucro/prejuízo) do mês anterior e a variação percentual em relação a ele.">Comparativo com o mês anterior</Tooltip>
            </div>
            <div className={styles.comparativo}>
              <div>
                <div className={styles.kpiLabel}>
                  <Tooltip text="Resultado (lucro/prejuízo) do mês imediatamente anterior ao selecionado.">Resultado anterior</Tooltip>
                </div>
                <div className={styles.comparativoValue}>{formatBRL(dre.comparativoMesAnterior.resultado)}</div>
              </div>
              <div
                className={styles.variacao}
                style={{ color: dre.comparativoMesAnterior.variacaoPct >= 0 ? 'var(--success)' : 'var(--danger)' }}
              >
                {formatPct(dre.comparativoMesAnterior.variacaoPct)}
              </div>
            </div>
          </Card>
        </>
      )}
    </div>
  );
}

// "mais gráficos e detalhe que estão nas planilhas do financeiro" (reunião 17/07/2026) — quebra
// por categoria real (CategoriaFinanceira.nome), mesma granularidade da planilha "DRE Financeira
// Saw". Mesmo estilo de barra de DashboardFaturamentoPage.composicaoList.
function CategoriaBreakdown({ titulo, tooltip, itens }: { titulo: string; tooltip: string; itens: CategoriaValor[] }) {
  const total = itens.reduce((acc, c) => acc + c.valor, 0);
  return (
    <>
      <div className={styles.sectionTitle}>
        <Tooltip text={tooltip}>{titulo}</Tooltip>
      </div>
      {itens.length === 0 && <div className={styles.empty}>Sem lançamentos categorizados neste período.</div>}
      <div className={styles.composicaoList}>
        {itens.map((c, i) => {
          const pct = total === 0 ? 0 : (c.valor / total) * 100;
          const cor = PALETA[i % PALETA.length];
          return (
            <div key={c.categoria}>
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
    </>
  );
}

function DreLinha({ label, valor, indent = false, strong = false, highlight = false }: {
  label: string;
  valor: number;
  indent?: boolean;
  strong?: boolean;
  highlight?: boolean;
}) {
  return (
    <div className={styles.linha} style={{ background: highlight ? 'rgba(63,178,127,.06)' : undefined }}>
      <span className={styles.linhaLabel} style={{ paddingLeft: indent ? 16 : 0, fontWeight: strong ? 700 : 400 }}>
        {label}
      </span>
      <span
        className={styles.linhaValor}
        style={{ fontWeight: strong ? 700 : 400, color: valor < 0 ? 'var(--danger)' : undefined }}
      >
        {formatBRL(valor)}
      </span>
    </div>
  );
}
