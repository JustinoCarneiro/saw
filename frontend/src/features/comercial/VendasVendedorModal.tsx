import { useEffect, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import { PRODUTO_VENDA_LABEL } from '../../shared/lib/labels';
import type { FormaPagamento, Lead } from '../../shared/lib/types';
import styles from './RankingComercialPage.module.css';

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
    <div className={styles.modalOverlay}>
      <div className={styles.modalContent} style={{ maxWidth: '900px' }}>
        <button className={styles.closeButton} onClick={onClose} aria-label="Fechar" title="Fechar modal">✕</button>
        <h2 className={styles.modalTitle}>
          Vendas de {vendedorNome} ({mes.toString().padStart(2, '0')}/{ano})
        </h2>
        
        {percentualComissao != null ? (
          <p style={{ marginBottom: 16 }}>
            Comissão definida: <strong>{percentualComissao}%</strong> 
            {' | '} 
            Total a pagar: <strong>{formatCurrency(comissaoTotal)}</strong>
          </p>
        ) : (
          <p style={{ marginBottom: 16, color: 'var(--text-muted)' }}>
            Nenhum % de comissão definido para este período.
          </p>
        )}

        {error && <div className={styles.error}>{error}</div>}

        {loading ? (
          <div className={styles.loading}>Carregando vendas…</div>
        ) : vendas.length === 0 ? (
          <p>Nenhuma venda registrada neste período (excluindo ingressos).</p>
        ) : (
          <div className={styles.tableWrapper}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>Data</th>
                  <th>Cliente</th>
                  <th>Produto</th>
                  <th>Pgto</th>
                  <th style={{ textAlign: 'right' }}>Total Venda</th>
                  <th style={{ textAlign: 'right' }}>Pago no Ato</th>
                  <th>Status</th>
                  {percentualComissao != null && <th style={{ textAlign: 'right' }}>Comissão</th>}
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
                      <td>{formatDate(v.dataFechamento)}</td>
                      <td>{v.nome}</td>
                      <td>{v.produtoVenda ? PRODUTO_VENDA_LABEL[v.produtoVenda] : '-'}</td>
                      <td>{v.formaPagamento ? FORMA_PAGAMENTO_LABEL[v.formaPagamento] : '-'}</td>
                      <td style={{ textAlign: 'right' }}>{formatCurrency(v.valorTotalVenda)}</td>
                      <td style={{ textAlign: 'right' }}>{formatCurrency(v.valorPagoNoAto)}</td>
                      <td>
                        <span className={styles.badge} style={{ background: isPago ? 'var(--success)' : 'var(--gold)', color: '#fff' }}>
                          {statusLabel}
                        </span>
                      </td>
                      {valorComissao != null && (
                        <td style={{ textAlign: 'right', fontWeight: 'bold' }}>
                          {formatCurrency(valorComissao)}
                        </td>
                      )}
                    </tr>
                  );
                })}
              </tbody>
              <tfoot>
                <tr>
                  <td colSpan={4} style={{ textAlign: 'right', fontWeight: 'bold' }}>Totais:</td>
                  <td style={{ textAlign: 'right', fontWeight: 'bold' }}>{formatCurrency(totalVendido)}</td>
                  <td colSpan={2}></td>
                  {percentualComissao != null && (
                    <td style={{ textAlign: 'right', fontWeight: 'bold', color: 'var(--success)' }}>
                      {formatCurrency(comissaoTotal)}
                    </td>
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
