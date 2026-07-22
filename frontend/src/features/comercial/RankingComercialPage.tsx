import { type FormEvent, useEffect, useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import { apiClient } from '../../shared/lib/apiClient';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import { Card } from '../../shared/components/Card';
import { DataGrid, DataGridRow } from '../../shared/components/DataGrid';
import { PeriodoPicker } from '../../shared/components/PeriodoPicker';
import type { MetaComercial, RankingComercialItem, VendedorResumo } from '../../shared/lib/types';
import { VendasVendedorModal } from './VendasVendedorModal';
import styles from './RankingComercialPage.module.css';

const COLUMNS = '1.6fr 1fr 1fr 2fr 1fr';

export function RankingComercialPage() {
  const { user } = useAuth();
  const podeDefinirMeta = user?.area === 'ADMIN';
  const [ano, setAno] = useState(new Date().getFullYear());
  const [mes, setMes] = useState(new Date().getMonth() + 1);
  const [ranking, setRanking] = useState<RankingComercialItem[] | null>(null);
  const [vendedores, setVendedores] = useState<VendedorResumo[]>([]);
  const [metas, setMetas] = useState<MetaComercial[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [showMetaForm, setShowMetaForm] = useState(false);
  const [vendedorModal, setVendedorModal] = useState<MetaComercial | null>(null);

  const carregarRanking = () => {
    setRanking(null);
    apiClient
      .get<RankingComercialItem[]>('/admin/comercial/ranking', { params: { ano, mes } })
      .then((res) => setRanking(res.data))
      .catch(() => setError('Não foi possível carregar o ranking.'));
  };

  const carregarMetas = () => {
    apiClient.get<MetaComercial[]>('/admin/comercial/metas', { params: { ano, mes } })
      .then((res) => setMetas(res.data))
      .catch(() => setError('Não foi possível carregar as metas.'));
  };

  useEffect(carregarRanking, [ano, mes]);
  useEffect(carregarMetas, [ano, mes]);

  useEffect(() => {
    apiClient.get<VendedorResumo[]>('/admin/comercial/vendedores')
      .then((res) => setVendedores(res.data))
      .catch(() => setError('Não foi possível carregar a lista de vendedores.'));
  }, []);

  const vendedoresSemMeta = vendedores.filter((v) => !metas.some((m) => m.vendedorId === v.id));

  return (
    <div>
      <div className={styles.toolbar}>
        <PeriodoPicker ano={ano} mes={mes} onChange={(a, m) => { setAno(a); setMes(m); }} />
        {podeDefinirMeta && (
          <button className={styles.newButton} onClick={() => setShowMetaForm((v) => !v)}>
            <span style={{ fontSize: 16 }}>+</span>Definir meta
          </button>
        )}
      </div>

      {error && <div className={styles.error}>{error}</div>}

      <div className={styles.subtitle}>
        Meta e Realizado contam vendas fechadas (quantidade, não R$) no período, venda de ingresso de evento não entra aqui, ela conta na ocorrência do evento, não no mês da venda.
      </div>

      {podeDefinirMeta && showMetaForm && (
        <DefinirMetaForm
          vendedores={vendedores}
          metas={metas}
          ano={ano}
          mes={mes}
          onDefinida={() => { setShowMetaForm(false); carregarMetas(); carregarRanking(); }}
          onCancelar={() => setShowMetaForm(false)}
        />
      )}

      {vendedoresSemMeta.length > 0 && (
        <div className={styles.avisoSemMeta}>
          Sem meta definida para este período (não aparecem no ranking abaixo): {vendedoresSemMeta.map((v) => v.nome).join(', ')}.
        </div>
      )}

      <DataGrid columns={COLUMNS} headers={['Vendedor', 'Meta', 'Realizado', '% atingido', 'Ações']}>
        {ranking === null && !error && <div className={styles.loading}>Carregando…</div>}
        {ranking?.length === 0 && (
          <div className={styles.loading}>Nenhuma meta cadastrada para o período — clique em "+ Definir meta".</div>
        )}
        {ranking?.map((item, i) => {
          const pct = Math.min(100, item.pctAtingido);
          const cor = item.pctAtingido >= 100 ? 'var(--success)' : item.pctAtingido >= 60 ? 'var(--gold)' : 'var(--danger)';
          const metaInfo = metas.find(m => m.vendedorId === item.vendedor.id) || null;
          return (
            <DataGridRow key={item.vendedor.id} columns={COLUMNS} testId="ranking-row">
              <div className={styles.vendedor}>
                <span className={styles.pos}>#{i + 1}</span>
                {item.vendedor.nome}
              </div>
              <div className={styles.muted}>{item.metaFechamentos}</div>
              <div className={styles.strong}>{item.realizado}</div>
              <div className={styles.progressoCell}>
                <div className={styles.track}>
                  <div className={styles.fill} style={{ width: `${pct}%`, background: cor }} />
                </div>
                <span className={styles.pctValue} style={{ color: cor }}>{item.pctAtingido.toFixed(1)}%</span>
              </div>
              <div className={styles.actionsCell}>
                <button 
                  className={styles.viewButton} 
                  onClick={() => metaInfo && setVendedorModal(metaInfo)}
                  title="Ver vendas e comissões do vendedor"
                >
                  Ver Vendas
                </button>
              </div>
            </DataGridRow>
          );
        })}
      </DataGrid>

      {vendedorModal && (
        <VendasVendedorModal
          vendedorId={vendedorModal.vendedorId}
          vendedorNome={vendedorModal.vendedorNome}
          ano={ano}
          mes={mes}
          percentualComissao={vendedorModal.percentualComissao ?? null}
          onClose={() => setVendedorModal(null)}
        />
      )}
    </div>
  );
}

function DefinirMetaForm({ vendedores, metas, ano, mes, onDefinida, onCancelar }: {
  vendedores: VendedorResumo[];
  metas: MetaComercial[];
  ano: number;
  mes: number;
  onDefinida: () => void;
  onCancelar: () => void;
}) {
  const [vendedorId, setVendedorId] = useState('');
  const [metaFechamentos, setMetaFechamentos] = useState('');
  const [percentualComissao, setPercentualComissao] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function selecionarVendedor(id: string) {
    setVendedorId(id);
    const metaAtual = metas.find((m) => m.vendedorId === id);
    setMetaFechamentos(metaAtual ? String(metaAtual.metaFechamentos) : '');
    setPercentualComissao(metaAtual && metaAtual.percentualComissao != null ? String(metaAtual.percentualComissao) : '');
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!vendedorId) {
      setError('Selecione um vendedor.');
      return;
    }
    setSubmitting(true);
    try {
      await apiClient.put('/admin/comercial/metas', {
        vendedorId, ano, mes, metaFechamentos: Number(metaFechamentos),
        percentualComissao: percentualComissao ? Number(percentualComissao) : null,
      });
      onDefinida();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível salvar a meta.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <form onSubmit={handleSubmit} className={styles.form}>
        <div className={styles.formRow}>
          <label className={styles.formField} style={{ flex: 2 }}>
            Vendedor
            <select className={styles.select} value={vendedorId} onChange={(e) => selecionarVendedor(e.target.value)} required>
              <option value="">Selecione…</option>
              {vendedores.map((v) => (
                <option key={v.id} value={v.id}>{v.nome}</option>
              ))}
            </select>
          </label>
          <label className={styles.formField}>
            Meta de fechamentos (vendas)
            <input className={styles.textInput} type="number" min={0} step={1}
                   value={metaFechamentos} onChange={(e) => setMetaFechamentos(e.target.value)} required />
          </label>
          <label className={styles.formField}>
            % de comissão
            <input className={styles.textInput} type="number" min={0} max={100} step="0.01"
                   value={percentualComissao} onChange={(e) => setPercentualComissao(e.target.value)} />
          </label>
        </div>

        {error && <div className={styles.error}>{error}</div>}

        <div className={styles.formActions}>
          <button type="button" className={styles.cancelButton} onClick={onCancelar}>Cancelar</button>
          <button type="submit" className={styles.newButton} disabled={submitting}>
            {submitting ? 'Salvando…' : 'Salvar meta'}
          </button>
        </div>
      </form>
    </Card>
  );
}
