import { useEffect, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { Card } from '../../shared/components/Card';
import { Pill } from '../../shared/components/Pill';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import type { Carrinho, CategoriaProduto, CheckoutResponse, PedidoMentorado, ProdutoCatalogo, StatusPedido } from '../../shared/lib/types';
import styles from './LojaPage.module.css';

const TABS = [
  { label: 'Catálogo', view: 'CATALOGO' as const },
  { label: 'Carrinho', view: 'CARRINHO' as const },
  { label: 'Meus Pedidos', view: 'PEDIDOS' as const },
];

const CATEGORIA_LABEL: Record<CategoriaProduto, string> = {
  CURSO: 'Curso',
  PLANILHA: 'Planilha',
  TEMPLATE: 'Template',
  EBOOK: 'E-book',
  FERRAMENTA: 'Ferramenta',
  KIT: 'Kit',
  CONSULTORIA: 'Consultoria',
};

function formatarPreco(valor: number): string {
  return valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function statusPedidoInfo(status: StatusPedido): { label: string; bg: string; color: string } {
  switch (status) {
    case 'AGUARDANDO_PAGAMENTO': return { label: 'Aguardando pagamento', bg: 'var(--warning-bg)', color: 'var(--warning)' };
    case 'PAGO': return { label: 'Pago', bg: 'var(--info-bg)', color: 'var(--info)' };
    case 'LIBERADO': return { label: 'Liberado', bg: 'var(--success-bg)', color: 'var(--success)' };
    case 'CANCELADO': return { label: 'Cancelado', bg: 'var(--danger-bg)', color: 'var(--danger)' };
    case 'REEMBOLSADO': return { label: 'Reembolsado', bg: 'var(--danger-bg)', color: 'var(--danger)' };
    default: return { label: status, bg: 'var(--line)', color: 'var(--text-soft)' };
  }
}

export function LojaPage() {
  const [view, setView] = useState<'CATALOGO' | 'CARRINHO' | 'PEDIDOS'>('CATALOGO');
  const [filtroCategoria, setFiltroCategoria] = useState<CategoriaProduto | ''>('');
  const [produtos, setProdutos] = useState<ProdutoCatalogo[] | null>(null);
  const [carrinho, setCarrinho] = useState<Carrinho | null>(null);
  const [pedidos, setPedidos] = useState<PedidoMentorado[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [processando, setProcessando] = useState<string | null>(null);

  const carregarCatalogo = () => {
    apiClient.get<ProdutoCatalogo[]>('/mentorado/loja/produtos', { params: { categoria: filtroCategoria || undefined } })
      .then((res) => setProdutos(res.data))
      .catch((err) => setError(getApiErrorMessage(err, 'Não foi possível carregar o catálogo.')));
  };

  const carregarCarrinho = () => {
    apiClient.get<Carrinho>('/mentorado/loja/carrinho')
      .then((res) => setCarrinho(res.data))
      .catch((err) => setError(getApiErrorMessage(err, 'Não foi possível carregar o carrinho.')));
  };

  const carregarPedidos = () => {
    apiClient.get<PedidoMentorado[]>('/mentorado/loja/pedidos')
      .then((res) => setPedidos(res.data))
      .catch((err) => setError(getApiErrorMessage(err, 'Não foi possível carregar seus pedidos.')));
  };

  useEffect(() => {
    setError(null);
    if (view === 'CATALOGO') carregarCatalogo();
    else if (view === 'CARRINHO') carregarCarrinho();
    else carregarPedidos();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [view, filtroCategoria]);

  async function adicionarAoCarrinho(produtoId: string) {
    setProcessando(produtoId);
    try {
      await apiClient.post('/mentorado/loja/carrinho/itens', { produtoId, quantidade: 1 });
      carregarCarrinho();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível adicionar ao carrinho.'));
    } finally {
      setProcessando(null);
    }
  }

  async function atualizarQuantidade(itemId: string, quantidade: number) {
    if (quantidade < 1) return;
    setProcessando(itemId);
    try {
      const res = await apiClient.patch<Carrinho>(`/mentorado/loja/carrinho/itens/${itemId}`, { quantidade });
      setCarrinho(res.data);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível atualizar a quantidade.'));
    } finally {
      setProcessando(null);
    }
  }

  async function removerItem(itemId: string) {
    setProcessando(itemId);
    try {
      const res = await apiClient.delete<Carrinho>(`/mentorado/loja/carrinho/itens/${itemId}`);
      setCarrinho(res.data);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível remover o item.'));
    } finally {
      setProcessando(null);
    }
  }

  async function finalizarCompra() {
    setProcessando('checkout');
    try {
      const res = await apiClient.post<CheckoutResponse>('/mentorado/loja/checkout');
      window.location.href = res.data.checkoutUrl;
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível iniciar o pagamento.'));
    } finally {
      setProcessando(null);
    }
  }

  return (
    <div className={styles.container}>
      <div>
        <h1 className={styles.title}>Loja SAW</h1>
        <p className={styles.subtitle}>Cursos, planilhas, templates e ferramentas pra acelerar seu restaurante.</p>
      </div>

      <div className={styles.tabs}>
        {TABS.map((t) => (
          <button
            key={t.label}
            className={`${styles.tab} ${view === t.view ? styles.tabActive : ''}`}
            onClick={() => setView(t.view)}
            data-testid={`loja-tab-${t.view}`}
          >
            {t.label}
            {t.view === 'CARRINHO' && carrinho && carrinho.itens.length > 0 && (
              <span className={styles.badge}>{carrinho.itens.length}</span>
            )}
          </button>
        ))}
      </div>

      {error && <div className={styles.error} data-testid="loja-erro">{error}</div>}

      {view === 'CATALOGO' && (
        <>
          <div className={styles.toolbar}>
            <select value={filtroCategoria} onChange={(e) => setFiltroCategoria(e.target.value as CategoriaProduto | '')} className={styles.select}>
              <option value="">Todas as categorias</option>
              {Object.entries(CATEGORIA_LABEL).map(([valor, label]) => (
                <option key={valor} value={valor}>{label}</option>
              ))}
            </select>
          </div>
          {produtos === null && !error && <div className={styles.emptyState}>Carregando catálogo…</div>}
          {produtos?.length === 0 && <div className={styles.emptyState}>Nenhum produto encontrado.</div>}
          <div className={styles.grid}>
            {produtos?.map((p) => (
              <Card key={p.id} testId={`produto-${p.id}`} style={{ padding: 20, display: 'flex', flexDirection: 'column', height: '100%' }}>
                {p.destaque && <div className={styles.destaqueTag}>DESTAQUE</div>}
                <div className={styles.categoriaLabel}>{CATEGORIA_LABEL[p.categoria]}</div>
                <div className={styles.produtoTitulo}>{p.titulo}</div>
                <div className={styles.produtoDesc}>{p.descricao}</div>
                {p.avaliacaoMedia && <div className={styles.avaliacao}>★ {p.avaliacaoMedia.toFixed(1)}</div>}
                <div className={styles.precoLine}>
                  {p.precoOriginal && <span className={styles.precoOriginal}>{formatarPreco(p.precoOriginal)}</span>}
                  <span className={styles.preco}>{formatarPreco(p.preco)}</span>
                </div>
                <button
                  className={styles.buyButton}
                  disabled={processando === p.id}
                  onClick={() => adicionarAoCarrinho(p.id)}
                  data-testid={`comprar-${p.id}`}
                >
                  Adicionar ao carrinho
                </button>
              </Card>
            ))}
          </div>
        </>
      )}

      {view === 'CARRINHO' && (
        <>
          {carrinho === null && !error && <div className={styles.emptyState}>Carregando carrinho…</div>}
          {carrinho?.itens.length === 0 && <div className={styles.emptyState}>Seu carrinho está vazio.</div>}
          {carrinho && carrinho.itens.length > 0 && (
            <div className={styles.cartList}>
              {carrinho.itens.map((item) => (
                <Card key={item.id} testId={`item-carrinho-${item.id}`} style={{ padding: 16 }}>
                  <div className={styles.cartRow}>
                    <div className={styles.cartInfo}>
                      <div className={styles.produtoTitulo}>{item.titulo}</div>
                      <div className={styles.produtoDesc}>{formatarPreco(item.precoUnitario)} cada</div>
                    </div>
                    <div className={styles.cartActions}>
                      <div className={styles.quantidadeControl}>
                        <button onClick={() => atualizarQuantidade(item.id, item.quantidade - 1)} disabled={processando === item.id}>−</button>
                        <span>{item.quantidade}</span>
                        <button
                          onClick={() => atualizarQuantidade(item.id, item.quantidade + 1)}
                          disabled={processando === item.id || !item.vendaEmAtacado}
                          title={item.vendaEmAtacado ? undefined : 'Este produto só pode ser comprado em unidade única.'}
                        >
                          +
                        </button>
                      </div>
                      <div className={styles.subtotal}>{formatarPreco(item.subtotal)}</div>
                      <button className={styles.removeButton} onClick={() => removerItem(item.id)} disabled={processando === item.id}>
                        Remover
                      </button>
                    </div>
                  </div>
                </Card>
              ))}
              <Card style={{ padding: 16 }}>
                <div className={styles.cartTotal}>
                  <span>Total</span>
                  <span className={styles.totalValor}>{formatarPreco(carrinho.valorTotal)}</span>
                </div>
                <button
                  className={styles.checkoutButton}
                  disabled={processando === 'checkout'}
                  onClick={finalizarCompra}
                  data-testid="finalizar-compra"
                >
                  {processando === 'checkout' ? 'Redirecionando…' : 'Finalizar compra'}
                </button>
              </Card>
            </div>
          )}
        </>
      )}

      {view === 'PEDIDOS' && (
        <>
          {pedidos === null && !error && <div className={styles.emptyState}>Carregando pedidos…</div>}
          {pedidos?.length === 0 && <div className={styles.emptyState}>Você ainda não fez nenhum pedido.</div>}
          <div className={styles.cartList}>
            {pedidos?.map((pedido) => {
              const info = statusPedidoInfo(pedido.status);
              return (
                <Card key={pedido.id} testId={`pedido-${pedido.id}`} style={{ padding: 16 }}>
                  <div className={styles.pedidoHeader}>
                    <span className={styles.pedidoData}>{new Date(pedido.criadoEm).toLocaleDateString('pt-BR')}</span>
                    <Pill bg={info.bg} color={info.color}>{info.label}</Pill>
                  </div>
                  {pedido.itens.map((item, i) => (
                    <div key={i} className={styles.pedidoItem}>
                      <span>{item.quantidade}x {item.titulo}</span>
                      {item.arquivoUrl && (
                        <a href={item.arquivoUrl} target="_blank" rel="noreferrer" className={styles.downloadLink}>
                          Baixar
                        </a>
                      )}
                    </div>
                  ))}
                  <div className={styles.pedidoTotal}>{formatarPreco(pedido.valorTotal)}</div>
                </Card>
              );
            })}
          </div>
        </>
      )}
    </div>
  );
}
