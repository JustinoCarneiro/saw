import { useEffect, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { ConfirmDialog } from '../../shared/components/ConfirmDialog';
import { DataGrid, DataGridRow } from '../../shared/components/DataGrid';
import { Pill } from '../../shared/components/Pill';
import type { PedidoAdmin, StatusPedido } from '../../shared/lib/types';
import styles from './ProdutosPage.module.css';

const COLUMNS = '1.6fr 1.4fr 1fr 1fr 1.4fr';

const STATUS_INFO: Record<StatusPedido, { label: string; bg: string; color: string }> = {
  CARRINHO: { label: 'Carrinho', bg: 'var(--line)', color: 'var(--text-soft)' },
  AGUARDANDO_PAGAMENTO: { label: 'Aguardando pagamento', bg: 'var(--warning-bg)', color: 'var(--warning)' },
  PAGO: { label: 'Pago', bg: 'var(--info-bg)', color: 'var(--info)' },
  LIBERADO: { label: 'Liberado', bg: 'var(--success-bg)', color: 'var(--success)' },
  CANCELADO: { label: 'Cancelado', bg: 'var(--danger-bg)', color: 'var(--danger)' },
  REEMBOLSADO: { label: 'Reembolsado', bg: 'var(--danger-bg)', color: 'var(--danger)' },
};

function formatarPreco(valor: number): string {
  return valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

export function PedidosPage() {
  const [status, setStatus] = useState<StatusPedido | ''>('');
  const [pedidos, setPedidos] = useState<PedidoAdmin[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [processando, setProcessando] = useState<string | null>(null);
  const [confirmando, setConfirmando] = useState<{ pedido: PedidoAdmin; acao: 'reembolsar' | 'cancelar' } | null>(null);

  const carregar = () => {
    setPedidos(null);
    apiClient
      .get<PedidoAdmin[]>('/admin/pedidos', { params: { status: status || undefined } })
      .then((res) => setPedidos(res.data))
      .catch(() => setError('Não foi possível carregar os pedidos.'));
  };

  useEffect(carregar, [status]);

  async function agir(id: string, acao: 'reembolsar' | 'cancelar') {
    setProcessando(id);
    try {
      await apiClient.patch(`/admin/pedidos/${id}/${acao}`);
      carregar();
    } catch {
      setError('Não foi possível concluir a ação.');
    } finally {
      setProcessando(null);
    }
  }

  async function confirmarAcao() {
    if (!confirmando) return;
    await agir(confirmando.pedido.id, confirmando.acao);
    setConfirmando(null);
  }

  return (
    <div>
      <div className={styles.toolbar}>
        <select className={styles.select} value={status} onChange={(e) => setStatus(e.target.value as StatusPedido | '')}>
          <option value="">Todos os status</option>
          {(Object.keys(STATUS_INFO) as StatusPedido[]).map((s) => (
            <option key={s} value={s}>{STATUS_INFO[s].label}</option>
          ))}
        </select>
      </div>

      {error && <div className={styles.error}>{error}</div>}

      <DataGrid columns={COLUMNS} headers={['Mentorado', 'Data', 'Total', 'Status', 'Ações']}>
        {pedidos === null && !error && <div className={styles.loading}>Carregando…</div>}
        {pedidos?.length === 0 && <div className={styles.loading}>Nenhum pedido encontrado.</div>}
        {pedidos?.map((p) => {
          const info = STATUS_INFO[p.status];
          const podeReembolsar = p.status === 'PAGO' || p.status === 'LIBERADO';
          const podeCancelar = p.status !== 'LIBERADO' && p.status !== 'CANCELADO' && p.status !== 'REEMBOLSADO';
          return (
            <DataGridRow key={p.id} columns={COLUMNS} testId={`pedido-row-${p.id}`}>
              <div className={styles.strong}>{p.mentoradoNome}</div>
              <div className={styles.muted}>{new Date(p.criadoEm).toLocaleDateString('pt-BR')}</div>
              <div className={styles.muted}>{formatarPreco(p.valorTotal)}</div>
              <div><Pill bg={info.bg} color={info.color}>{info.label}</Pill></div>
              <div className={styles.acoes}>
                {podeReembolsar && (
                  <button className={styles.actionButton} disabled={processando === p.id} onClick={() => setConfirmando({ pedido: p, acao: 'reembolsar' })}>
                    Reembolsar
                  </button>
                )}
                {podeCancelar && (
                  <button className={styles.actionButton} disabled={processando === p.id} onClick={() => setConfirmando({ pedido: p, acao: 'cancelar' })}>
                    Cancelar
                  </button>
                )}
              </div>
            </DataGridRow>
          );
        })}
      </DataGrid>

      {confirmando && (
        <ConfirmDialog
          title={confirmando.acao === 'reembolsar' ? 'Reembolsar pedido?' : 'Cancelar pedido?'}
          message={
            confirmando.acao === 'reembolsar'
              ? `O pedido de ${confirmando.pedido.mentoradoNome} (${formatarPreco(confirmando.pedido.valorTotal)}) será marcado como reembolsado. Essa ação não pode ser desfeita.`
              : `O pedido de ${confirmando.pedido.mentoradoNome} (${formatarPreco(confirmando.pedido.valorTotal)}) será cancelado. Essa ação não pode ser desfeita.`
          }
          confirmLabel={confirmando.acao === 'reembolsar' ? 'Confirmar reembolso' : 'Cancelar pedido'}
          cancelLabel="Voltar"
          submitting={processando === confirmando.pedido.id}
          onConfirm={confirmarAcao}
          onCancel={() => setConfirmando(null)}
        />
      )}
    </div>
  );
}
