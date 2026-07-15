import { type FormEvent, useEffect, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { Card } from '../../shared/components/Card';
import { DataGrid, DataGridRow } from '../../shared/components/DataGrid';
import { Pill } from '../../shared/components/Pill';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import type { CategoriaProduto, Produto } from '../../shared/lib/types';
import styles from './ProdutosPage.module.css';

const COLUMNS = '2fr 1fr 1fr 1fr 1fr 1.6fr';

const CATEGORIA_LABEL: Record<CategoriaProduto, string> = {
  CURSO: 'Curso', PLANILHA: 'Planilha', TEMPLATE: 'Template', EBOOK: 'E-book',
  FERRAMENTA: 'Ferramenta', KIT: 'Kit', CONSULTORIA: 'Consultoria',
};

function formatarPreco(valor: number): string {
  return valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

export function ProdutosPage() {
  const [categoria, setCategoria] = useState<CategoriaProduto | ''>('');
  const [produtos, setProdutos] = useState<Produto[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [criando, setCriando] = useState(false);

  const carregar = () => {
    setProdutos(null);
    apiClient
      .get<Produto[]>('/admin/produtos', { params: { categoria: categoria || undefined } })
      .then((res) => setProdutos(res.data))
      .catch(() => setError('Não foi possível carregar os produtos.'));
  };

  useEffect(carregar, [categoria]);

  async function alternarPublicacao(p: Produto) {
    await apiClient.patch(`/admin/produtos/${p.id}/${p.publicado ? 'despublicar' : 'publicar'}`);
    carregar();
  }

  return (
    <div>
      <div className={styles.toolbar}>
        <select className={styles.select} value={categoria} onChange={(e) => setCategoria(e.target.value as CategoriaProduto | '')}>
          <option value="">Todas as categorias</option>
          {(Object.keys(CATEGORIA_LABEL) as CategoriaProduto[]).map((c) => (
            <option key={c} value={c}>{CATEGORIA_LABEL[c]}</option>
          ))}
        </select>
        <button className={styles.newButton} onClick={() => setCriando(true)}>
          <span style={{ fontSize: 16 }}>+</span>Novo produto
        </button>
      </div>

      {criando && <ProdutoForm onSalvo={() => { setCriando(false); carregar(); }} onCancelar={() => setCriando(false)} />}

      {error && <div className={styles.error}>{error}</div>}

      <DataGrid columns={COLUMNS} headers={['Título', 'Categoria', 'Preço', 'Vendas', 'Status', 'Ações']}>
        {produtos === null && !error && <div className={styles.loading}>Carregando…</div>}
        {produtos?.length === 0 && <div className={styles.loading}>Nenhum produto encontrado.</div>}
        {produtos?.map((p) => (
          <DataGridRow key={p.id} columns={COLUMNS} testId={`produto-row-${p.id}`}>
            <div className={styles.strong}>{p.titulo}{p.destaque && ' ★'}</div>
            <div className={styles.muted}>{CATEGORIA_LABEL[p.categoria]}</div>
            <div className={styles.muted}>{formatarPreco(p.preco)}</div>
            <div className={styles.muted}>{p.vendas}</div>
            <div>
              <Pill bg={p.publicado ? 'var(--success-bg)' : 'var(--line)'} color={p.publicado ? 'var(--success)' : 'var(--text-soft)'}>
                {p.publicado ? 'Publicado' : 'Rascunho'}
              </Pill>
            </div>
            <div className={styles.acoes}>
              <button className={styles.actionButton} onClick={() => alternarPublicacao(p)}>
                {p.publicado ? 'Despublicar' : 'Publicar'}
              </button>
            </div>
          </DataGridRow>
        ))}
      </DataGrid>
    </div>
  );
}

function ProdutoForm({ onSalvo, onCancelar }: { onSalvo: () => void; onCancelar: () => void }) {
  const [titulo, setTitulo] = useState('');
  const [descricao, setDescricao] = useState('');
  const [categoria, setCategoria] = useState<CategoriaProduto>('PLANILHA');
  const [preco, setPreco] = useState('');
  const [precoOriginal, setPrecoOriginal] = useState('');
  const [destaque, setDestaque] = useState(false);
  const [vendaEmAtacado, setVendaEmAtacado] = useState(false);
  const [arquivoUrl, setArquivoUrl] = useState('');
  const [imagemUrl, setImagemUrl] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await apiClient.post('/admin/produtos', {
        titulo, descricao, categoria,
        preco: Number(preco),
        precoOriginal: precoOriginal ? Number(precoOriginal) : null,
        avaliacaoMedia: null,
        destaque,
        vendaEmAtacado,
        arquivoUrl,
        imagemUrl: imagemUrl || null,
      });
      onSalvo();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível salvar o produto. Confira os dados.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <div className={styles.formTitle}>Novo produto</div>
      <form onSubmit={handleSubmit} className={styles.form}>
        <div className={styles.formRow}>
          <label className={styles.formField} style={{ flex: 2 }}>
            Título
            <input className={styles.textInput} value={titulo} onChange={(e) => setTitulo(e.target.value)} required />
          </label>
          <label className={styles.formField}>
            Categoria
            <select className={styles.select} value={categoria} onChange={(e) => setCategoria(e.target.value as CategoriaProduto)}>
              {(Object.keys(CATEGORIA_LABEL) as CategoriaProduto[]).map((c) => (
                <option key={c} value={c}>{CATEGORIA_LABEL[c]}</option>
              ))}
            </select>
          </label>
        </div>
        <label className={styles.formField}>
          Descrição
          <textarea className={styles.textarea} value={descricao} onChange={(e) => setDescricao(e.target.value)} required />
        </label>
        <div className={styles.formRow}>
          <label className={styles.formField}>
            Preço (R$)
            <input className={styles.textInput} type="number" step="0.01" min="0.01" value={preco} onChange={(e) => setPreco(e.target.value)} required />
          </label>
          <label className={styles.formField}>
            Preço original (opcional, pra mostrar desconto)
            <input className={styles.textInput} type="number" step="0.01" min="0" value={precoOriginal} onChange={(e) => setPrecoOriginal(e.target.value)} />
          </label>
          <label className={styles.checkboxField}>
            <input type="checkbox" checked={destaque} onChange={(e) => setDestaque(e.target.checked)} />
            Destaque
          </label>
        </div>
        <label className={styles.checkboxField}>
          <input type="checkbox" checked={vendaEmAtacado} onChange={(e) => setVendaEmAtacado(e.target.checked)} />
          Permite comprar mais de uma unidade (atacado)
        </label>
        <p className={styles.helpText}>
          Desmarcado (padrão): o mentorado só pode levar 1 unidade — recomendado pra acesso digital
          de licença única (curso, e-book, template). Marque só se o produto fizer sentido em lote.
        </p>
        <label className={styles.formField}>
          Link do arquivo (liberado ao mentorado após pagamento)
          <input className={styles.textInput} value={arquivoUrl} onChange={(e) => setArquivoUrl(e.target.value)} placeholder="https://..." required />
        </label>
        <label className={styles.formField}>
          Imagem (opcional)
          <input className={styles.textInput} value={imagemUrl} onChange={(e) => setImagemUrl(e.target.value)} placeholder="https://..." />
        </label>
        {error && <div className={styles.error}>{error}</div>}
        <div className={styles.formActions}>
          <button type="button" className={styles.cancelButton} onClick={onCancelar}>Cancelar</button>
          <button type="submit" className={styles.actionButton} disabled={submitting}>
            {submitting ? 'Salvando…' : 'Salvar'}
          </button>
        </div>
      </form>
    </Card>
  );
}
