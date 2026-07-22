import { useEffect, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { DataGrid, DataGridRow } from '../../shared/components/DataGrid';
import type { ConciliacaoVenda } from '../../shared/lib/types';
import { formatBRL } from '../../shared/lib/format';
import styles from './ConciliacaoPage.module.css';

const COLUMNS = '1.6fr 1fr 1fr 1fr 1.4fr';

// Pedido do Marcos (22/07/2026) — alerta de atraso. Componente local (não o Tooltip
// compartilhado) porque este é um ÍCONE DE ALERTA (triângulo, cor de perigo), não o ⓘ de
// "explicar termo" que o Tooltip compartilhado sempre renderiza.
function AlertaAtraso({ diasAtraso, parcelasEmAtraso }: { diasAtraso: number; parcelasEmAtraso: number }) {
  const [visivel, setVisivel] = useState(false);
  const texto = `${parcelasEmAtraso} parcela${parcelasEmAtraso > 1 ? 's' : ''} em atraso — a mais antiga venceu há ${diasAtraso} dia${diasAtraso > 1 ? 's' : ''}.`;

  return (
    <span
      className={styles.alertaTrigger}
      tabIndex={0}
      onMouseEnter={() => setVisivel(true)}
      onMouseLeave={() => setVisivel(false)}
      onFocus={() => setVisivel(true)}
      onBlur={() => setVisivel(false)}
    >
      <span className={styles.alertaIcone} aria-hidden="true">⚠</span>
      {visivel && (
        <span className={styles.alertaBubble} role="tooltip">{texto}</span>
      )}
    </span>
  );
}

function corDaBarra(pct: number): string {
  if (pct >= 100) return 'var(--success)';
  if (pct >= 50) return 'var(--warning)';
  return 'var(--danger)';
}

// Change request 17/07/2026 ("conciliação entre valor total do contrato e valor efetivamente
// recebido, parcela a parcela — importante pra declaração de imposto").
export function ConciliacaoPage() {
  const [vendas, setVendas] = useState<ConciliacaoVenda[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiClient.get<ConciliacaoVenda[]>('/admin/financeiro/conciliacao')
      .then((res) => setVendas(res.data))
      .catch(() => setError('Não foi possível carregar a conciliação.'));
  }, []);

  return (
    <div>
      <div className={styles.toolbar}>
        <div className={styles.subtitle}>
          Valor total de cada venda fechada (Comercial) comparado com o que já foi efetivamente
          recebido — pago no ato, taxa de plataforma retida e parcelas liquidadas.
        </div>
      </div>

      {error && <div className={styles.error}>{error}</div>}

      <DataGrid columns={COLUMNS} headers={['Venda', 'Valor total', 'Recebido', 'Pendente', '% recebido']}>
        {vendas === null && !error && <div className={styles.loading}>Carregando…</div>}
        {vendas?.length === 0 && <div className={styles.loading}>Nenhuma venda fechada encontrada.</div>}
        {vendas?.map((v) => (
          <DataGridRow key={v.leadId} columns={COLUMNS}>
            <div className={styles.strong}>
              {v.nome}
              {v.emAtraso && v.diasAtraso != null && v.parcelasEmAtraso != null && (
                <AlertaAtraso diasAtraso={v.diasAtraso} parcelasEmAtraso={v.parcelasEmAtraso} />
              )}
            </div>
            <div className={styles.valor}>{formatBRL(v.valorTotalVenda)}</div>
            <div className={styles.valor}>{formatBRL(v.valorRecebido)}</div>
            <div className={v.valorPendente > 0 ? styles.valor : styles.muted}>{formatBRL(v.valorPendente)}</div>
            <div>
              <div className={styles.muted}>{v.percentualRecebido.toFixed(0)}%</div>
              <div className={styles.barTrack}>
                <div
                  className={styles.barFill}
                  style={{ width: `${Math.min(v.percentualRecebido, 100)}%`, background: corDaBarra(v.percentualRecebido) }}
                />
              </div>
            </div>
          </DataGridRow>
        ))}
      </DataGrid>
    </div>
  );
}
