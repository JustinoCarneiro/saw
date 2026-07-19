package com.sawhub.hub.comercial;

/** M25 — categorias reais encontradas na planilha "Vendas Eventos", substituem o "Individual/
 * Duplo/VIP" citado de memória na reunião original.
 *
 * <p>Gap 8 (raio-x na planilha-fonte "Vendas Eventos", confirmado 19/07/2026): {@code CORTESIA}
 * saiu do eixo errado — é {@link OrigemVenda#CORTESIA} (de onde veio a venda), não tipo de
 * ingresso; o valor real que faltava aqui era {@link #BLACK}, tratado como categoria permanente
 * (não de campanha específica). Sem migração de dado: nenhuma linha existente usava CORTESIA. */
public enum CategoriaIngresso {
    ESSENCIAL,
    VIP,
    ESPECIAL,
    BLACK
}
