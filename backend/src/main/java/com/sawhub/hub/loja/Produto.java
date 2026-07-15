package com.sawhub.hub.loja;

import com.sawhub.hub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** H8.1 — catálogo curado pelo Admin. `avaliacaoMedia` é um campo estático setado pelo Admin, não
 * um sistema de reviews (não existe em nenhum outro módulo do projeto — ver Suposições do
 * Blueprint M14 no ROADMAP.md). `vendas` incrementa quando um Pedido contendo este produto vira
 * PAGO (ver PedidoService). */
@Entity
@Table(name = "produto")
public class Produto extends BaseEntity {

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaProduto categoria;

    @Column(nullable = false)
    private BigDecimal preco;

    @Column(name = "preco_original")
    private BigDecimal precoOriginal;

    @Column(name = "avaliacao_media")
    private BigDecimal avaliacaoMedia;

    @Column(nullable = false)
    private boolean destaque = false;

    @Column(nullable = false)
    private int vendas = 0;

    @Column(name = "arquivo_url", nullable = false)
    private String arquivoUrl;

    @Column(name = "imagem_url")
    private String imagemUrl;

    @Column(nullable = false)
    private boolean publicado = false;

    // Achado de UX (Fase 5): a maioria dos produtos daqui é acesso digital de licença única (curso,
    // e-book, template) — permitir quantidade > 1 no carrinho é um erro induzido do usuário mais
    // vezes do que uma compra legítima. Default FALSE (só unidade) de propósito: o Admin precisa
    // marcar explicitamente os itens que fazem sentido em lote (ex.: um kit físico, licença de
    // equipe), em vez de todo produto novo nascer permitindo quantidade livre.
    @Column(name = "venda_em_atacado", nullable = false)
    private boolean vendaEmAtacado = false;

    protected Produto() {
    }

    public Produto(String titulo, String descricao, CategoriaProduto categoria, BigDecimal preco,
                    BigDecimal precoOriginal, BigDecimal avaliacaoMedia, boolean destaque,
                    String arquivoUrl, String imagemUrl, boolean vendaEmAtacado) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.categoria = categoria;
        this.preco = preco;
        this.precoOriginal = precoOriginal;
        this.avaliacaoMedia = avaliacaoMedia;
        this.destaque = destaque;
        this.arquivoUrl = arquivoUrl;
        this.imagemUrl = imagemUrl;
        this.vendaEmAtacado = vendaEmAtacado;
    }

    public void atualizar(String titulo, String descricao, CategoriaProduto categoria, BigDecimal preco,
                           BigDecimal precoOriginal, BigDecimal avaliacaoMedia, boolean destaque,
                           String arquivoUrl, String imagemUrl, boolean vendaEmAtacado) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.categoria = categoria;
        this.preco = preco;
        this.precoOriginal = precoOriginal;
        this.avaliacaoMedia = avaliacaoMedia;
        this.destaque = destaque;
        this.arquivoUrl = arquivoUrl;
        this.imagemUrl = imagemUrl;
        this.vendaEmAtacado = vendaEmAtacado;
    }

    public void publicar() {
        this.publicado = true;
    }

    public void despublicar() {
        this.publicado = false;
    }

    public void incrementarVendas(int quantidade) {
        this.vendas += quantidade;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public CategoriaProduto getCategoria() {
        return categoria;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public BigDecimal getPrecoOriginal() {
        return precoOriginal;
    }

    public BigDecimal getAvaliacaoMedia() {
        return avaliacaoMedia;
    }

    public boolean isDestaque() {
        return destaque;
    }

    public int getVendas() {
        return vendas;
    }

    public String getArquivoUrl() {
        return arquivoUrl;
    }

    public String getImagemUrl() {
        return imagemUrl;
    }

    public boolean isPublicado() {
        return publicado;
    }

    public boolean isVendaEmAtacado() {
        return vendaEmAtacado;
    }
}
