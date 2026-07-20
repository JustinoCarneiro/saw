import { useEffect, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { Card } from '../../shared/components/Card';
import type { DreResponse } from '../../shared/lib/types';
import { formatBRL, formatPct } from '../../shared/lib/format';
import { PeriodoPicker } from '../../shared/components/PeriodoPicker';
import styles from './DrePage.module.css';

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
              <div className={styles.kpiLabel}>Receita Bruta</div>
              <div className={styles.kpiValue}>{formatBRL(dre.receitaBruta)}</div>
            </Card>
            <Card style={{ padding: 18 }}>
              <div className={styles.kpiLabel}>Custos</div>
              <div className={styles.kpiValue} style={{ color: 'var(--danger)' }}>{formatBRL(dre.custos)}</div>
            </Card>
            <Card style={{ padding: 18 }}>
              <div className={styles.kpiLabel}>Despesas Operacionais</div>
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
                {lucro ? 'Lucro' : 'Prejuízo'}
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
              <div className={styles.sectionTitle}>Despesas fixas x variáveis</div>
              <div className={styles.comparativo}>
                <div>
                  <div className={styles.kpiLabel}>Fixas</div>
                  <div className={styles.comparativoValue}>{formatBRL(dre.despesasFixas)}</div>
                </div>
                <div>
                  <div className={styles.kpiLabel}>Variáveis</div>
                  <div className={styles.comparativoValue}>{formatBRL(dre.despesasVariaveis)}</div>
                </div>
              </div>
            </Card>
          )}

          <Card style={{ marginTop: 16, padding: '20px 22px' }}>
            <div className={styles.sectionTitle}>Comparativo com o mês anterior</div>
            <div className={styles.comparativo}>
              <div>
                <div className={styles.kpiLabel}>Resultado anterior</div>
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
