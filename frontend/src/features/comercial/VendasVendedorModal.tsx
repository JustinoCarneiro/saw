import { useEffect, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import { Pill } from '../../shared/components/Pill';
import { PRODUTO_VENDA_LABEL } from '../../shared/lib/labels';
import type { FormaPagamento, Lead } from '../../shared/lib/types';
import styles from './VendasVendedorModal.module.css';

interface VendasVendedorModalProps {
  vendedorId: string;
  vendedorNome: string;
  ano: number;
  mes: number;
  percentualComissao: number | null;
  onClose: () => void;
}

// Mesmos rótulos de LeadsComercialPage (FORMA_PAGAMENTO_LABEL) — não exportado de lá, duplicado
// aqui de propósito pra não criar acoplamento entre as duas telas por um detalhe de rótulo.
const FORMA_PAGAMENTO_LABEL: Record<FormaPagamento, string> = {
  PIX: 'Pix',
  PIX_RECORRENTE: 'Pix recorrente (assinatura)',
  CARTAO: 'Cartão',
  BOLETO: 'Boleto',
  HOTMART: 'Hotmart',
};

function formatCurrency(value: number | null | undefined): string {
  if (value == null) return 'R$ 0,00';
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
}

function formatDate(isoDate: string | null | undefined): string {
  if (!isoDate) return '-';
  return new Date(isoDate).toLocaleDateString('pt-BR');
}

export function VendasVendedorModal({
  vendedorId,
  vendedorNome,
  ano,
  mes,
  percentualComissao,
  onClose,
}: VendasVendedorModalProps) {
  const [vendas, setVendas] = useState<Lead[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function carregarVendas() {
      try {
        setLoading(true);
        const res = await apiClient.get<Lead[]>(`/admin/comercial/ranking/vendedores/${vendedorId}/vendas`, {
          params: { ano, mes },
        });
        setVendas(res.data);
      } catch (err) {
        setError(getApiErrorMessage(err, 'Erro ao carregar as vendas do vendedor.'));
      } finally {
        setLoading(false);
      }
    }
    carregarVendas();
  }, [vendedorId, ano, mes]);

  const totalVendido = vendas.reduce((acc, v) => acc + (v.valorTotalVenda || 0), 0);
  const comissaoTotal = percentualComissao != null ? (totalVendido * percentualComissao) / 100 : 0;

  return (
    <div className={styles.overlay} onClick={onClose}>
      <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
        <button className={styles.closeButton} onClick={onClose} aria-label="Fechar" title="Fechar modal">✕</button>
        <h2 className={styles.title}>Vendas de {vendedorNome}</h2>
        <div className={styles.subtitle}>{mes.toString().padStart(2, '0')}/{ano}</div>

        <div className={styles.summary}>
          {percentualComissao != null ? (
            <>
              <div className={styles.summaryItem}>
                <span className={styles.summaryLabel}>Comissão definida</span>
                <span className={styles.summaryValue}>{percentualComissao}%</span>
              </div>
              <div className={styles.summaryDivider} />
              <div className={styles.summaryItem}>
                <span className={styles.summaryLabel}>Total vendido</span>
                <span className={styles.summaryValue}>{formatCurrency(totalVendido)}</span>
              </div>
              <div className={styles.summaryDivider} />
              <div className={styles.summaryItem}>
                <span className={styles.summaryLabel}>Total a pagar</span>
                <span className={styles.summaryValueHighlight}>{formatCurrency(comissaoTotal)}</span>
              </div>
            </>
          ) : (
            <span className={styles.summaryEmpty}>Nenhum % de comissão definido para este período.</span>
          )}
        </div>

        {error && <div className={styles.error}>{error}</div>}

        {loading ? (
          <div className={styles.loading}>Carregando vendas…</div>
        ) : vendas.length === 0 ? (
          <div className={styles.empty}>Nenhuma venda registrada neste período (excluindo ingressos).</div>
        ) : (
          <div className={styles.tableWrapper}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>Data</th>
                  <th>Cliente</th>
                  <th>Produto</th>
                  <th>Pgto</th>
                  <th className={styles.right}>Total venda</th>
                  <th className={styles.right}>Pago no ato</th>
                  <th>Status</th>
                  {percentualComissao != null && <th className={styles.right}>Comissão</th>}
                </tr>
              </thead>
              <tbody>
                {vendas.map((v) => {
                  const valorRecebido = (v.valorPagoNoAto || 0) + (v.taxaPlataformaRetida || 0);
                  const isPago = valorRecebido >= (v.valorTotalVenda || 0);
                  const statusLabel = isPago ? 'Pago' : 'Parcial / Pendente';
                  const valorComissao = percentualComissao != null ? ((v.valorTotalVenda || 0) * percentualComissao) / 100 : null;

                  return (
                    <tr key={v.id}>
                      <td className={styles.muted}>{formatDate(v.dataFechamento)}</td>
                      <td>{v.nome}</td>
                      <td className={styles.muted}>{v.produtoVenda ? PRODUTO_VENDA_LABEL[v.produtoVenda] : '-'}</td>
                      <td className={styles.muted}>{v.formaPagamento ? FORMA_PAGAMENTO_LABEL[v.formaPagamento] : '-'}</td>
                      <td className={styles.right}>{formatCurrency(v.valorTotalVenda)}</td>
                      <td className={`${styles.right} ${styles.muted}`}>{formatCurrency(v.valorPagoNoAto)}</td>
                      <td>
                        <Pill bg={isPago ? 'var(--success-bg)' : 'var(--warning-bg)'} color={isPago ? 'var(--success)' : 'var(--warning)'}>
                          {statusLabel}
                        </Pill>
                      </td>
                      {valorComissao != null && (
                        <td className={styles.right}>{formatCurrency(valorComissao)}</td>
                      )}
                    </tr>
                  );
                })}
              </tbody>
              <tfoot>
                <tr className={styles.totalsRow}>
                  <td colSpan={4} className={styles.right}>Totais</td>
                  <td className={styles.right}>{formatCurrency(totalVendido)}</td>
                  <td colSpan={2}></td>
                  {percentualComissao != null && (
                    <td className={`${styles.right} ${styles.totalsHighlight}`}>{formatCurrency(comissaoTotal)}</td>
                  )}
                </tr>
              </tfoot>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
