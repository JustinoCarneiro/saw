package com.sawhub.hub.comercial;

import com.sawhub.hub.common.BaseEntity;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.Plano;
import com.sawhub.hub.mentorado.TipoContrato;
import com.sawhub.hub.team.Colaborador;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.ColumnTransformer;

/** H1.3 + H13.2 — nasce da solicitação pública de acesso (status inicial {@link StatusLead#SOLICITACAO})
 * e progride pelo funil comercial. Máquina de estado (CLAUDE.md): Solicitação -&gt; Em contato -&gt;
 * Proposta -&gt; Fechado, com desvio -&gt; Perdido a partir de qualquer estado não-terminal — cada
 * transição vive num método próprio, impossível pular etapa (ex.: Solicitação direto pra Fechado)
 * de fora desta classe. */
@Entity
@Table(name = "lead")
public class Lead extends BaseEntity {

    // Pass transversal de pgcrypto (Fase 5, achado L3 do revisor-seguranca): nome/email/telefone/
    // mensagem/motivoPerdido são PII de LGPD nunca usados em WHERE/LIKE/ORDER BY (ver
    // LeadRepository/LeadController — filtro é só por status/vendedorId), então criptografar não
    // quebra busca nenhuma. A chave vem de uma GUC de sessão (SET app.encryption_key, ver
    // application.yml), nunca de um literal aqui — ver V19__pgcrypto_dados_sensiveis.sql.
    @Column(nullable = false, columnDefinition = "bytea")
    @ColumnTransformer(
            read = "pgp_sym_decrypt(nome, current_setting('app.encryption_key'))",
            write = "pgp_sym_encrypt(?, current_setting('app.encryption_key'))")
    private String nome;

    @Column(nullable = false, columnDefinition = "bytea")
    @ColumnTransformer(
            read = "pgp_sym_decrypt(email, current_setting('app.encryption_key'))",
            write = "pgp_sym_encrypt(?, current_setting('app.encryption_key'))")
    private String email;

    @Column(columnDefinition = "bytea")
    @ColumnTransformer(
            read = "pgp_sym_decrypt(telefone, current_setting('app.encryption_key'))",
            write = "pgp_sym_encrypt(?, current_setting('app.encryption_key'))")
    private String telefone;

    @Column(columnDefinition = "bytea")
    @ColumnTransformer(
            read = "pgp_sym_decrypt(mensagem, current_setting('app.encryption_key'))",
            write = "pgp_sym_encrypt(?, current_setting('app.encryption_key'))")
    private String mensagem;

    @Enumerated(EnumType.STRING)
    @Column(name = "plano_interesse")
    private Plano planoInteresse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusLead status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_id")
    private Colaborador vendedor;

    @Enumerated(EnumType.STRING)
    @Column(name = "plano_fechado")
    private Plano planoFechado;

    @Column(name = "motivo_perdido", columnDefinition = "bytea")
    @ColumnTransformer(
            read = "pgp_sym_decrypt(motivo_perdido, current_setting('app.encryption_key'))",
            write = "pgp_sym_encrypt(?, current_setting('app.encryption_key'))")
    private String motivoPerdido;

    @Column(name = "data_fechamento")
    private Instant dataFechamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentorado_id")
    private Mentorado mentorado;

    // M23 (change request pós-MVP, 17/07/2026) — em paralelo a planoFechado, não o substitui (ver
    // Suposição 1 do Blueprint M23 em ROADMAP.md: TipoContrato é aditivo). Setado só por
    // criarJaFechado() nesta leva.
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_contrato_fechado")
    private TipoContrato tipoContratoFechado;

    // M25 (change request pós-MVP, 17/07/2026) — "formulário único de venda", aditivo em
    // paralelo aos campos do M23 (planoFechado/tipoContratoFechado). Setado só por
    // fecharVenda(), nunca pelo fechar(Plano) legado.
    @Enumerated(EnumType.STRING)
    @Column(name = "produto_venda")
    private ProdutoVenda produtoVenda;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_venda")
    private OrigemVenda origemVenda;

    @Column(name = "valor_total_venda", columnDefinition = "bytea")
    @ColumnTransformer(
            read = "pgp_sym_decrypt(valor_total_venda, current_setting('app.encryption_key'))::numeric",
            write = "pgp_sym_encrypt(?::text, current_setting('app.encryption_key'))")
    private BigDecimal valorTotalVenda;

    @Column(name = "valor_pago_no_ato", columnDefinition = "bytea")
    @ColumnTransformer(
            read = "pgp_sym_decrypt(valor_pago_no_ato, current_setting('app.encryption_key'))::numeric",
            write = "pgp_sym_encrypt(?::text, current_setting('app.encryption_key'))")
    private BigDecimal valorPagoNoAto;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento")
    private FormaPagamento formaPagamento;

    // Gap 7 (raio-x em "Vendas Aline Melo" + pesquisa da taxa real da Hotmart, confirmado
    // 19/07/2026): gateways de pagamento (Hotmart: ~9,9%+R$1, mais taxa de antecipação opcional)
    // retêm uma fatia antes de repassar pra SAW. Sem esse conceito, valorPagoNoAto < valorTotalVenda
    // parecia dívida do cliente quando na verdade ele pagou 100% — a diferença é taxa de
    // plataforma, não parcela em aberto. Mesmo critério pgcrypto de valorTotalVenda/valorPagoNoAto.
    @Column(name = "taxa_plataforma_retida", columnDefinition = "bytea")
    @ColumnTransformer(
            read = "pgp_sym_decrypt(taxa_plataforma_retida, current_setting('app.encryption_key'))::numeric",
            write = "pgp_sym_encrypt(?::text, current_setting('app.encryption_key'))")
    private BigDecimal taxaPlataformaRetida;

    protected Lead() {
    }

    public Lead(String nome, String email, String telefone, String mensagem, Plano planoInteresse) {
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.mensagem = mensagem;
        this.planoInteresse = planoInteresse;
        this.status = StatusLead.SOLICITACAO;
    }

    /** M23 — "criar mentorado direto" (pedido explícito do cliente: "É IMPORTANTE PODER CRIAR
     * DIRETAMENTE O MENTORADO, SENDO AUTOMÁTICAMENTE UM LEAD FECHADO"). Construtor alternativo,
     * não uma transição — nasce direto em FECHADO sem passar pelas etapas do funil
     * (Solicitação->Em contato->Proposta), mas continua sendo um Lead FECHADO de verdade: o
     * mesmo {@link #vincularMentorado} de sempre funciona a partir daqui. */
    public static Lead criarJaFechado(String nome, String email, String telefone, TipoContrato tipoContrato) {
        Lead lead = new Lead(nome, email, telefone, null, null);
        lead.status = StatusLead.FECHADO;
        lead.tipoContratoFechado = tipoContrato;
        lead.dataFechamento = Instant.now();
        return lead;
    }

    /** Só a partir de SOLICITACAO — primeiro contato do time comercial com o lead. */
    public void moverParaEmContato(Colaborador vendedor) {
        exigirStatus(StatusLead.SOLICITACAO);
        this.status = StatusLead.EM_CONTATO;
        this.vendedor = vendedor;
    }

    /** M25 — só a partir de EM_CONTATO. Opcional: quem não passa por aqui continua indo direto
     * pra PROPOSTA (ver {@link #moverParaProposta}), bate com o funil real mas não é um gate
     * obrigatório imposto sobre o comportamento já existente. */
    public void moverParaDiagnostico() {
        exigirStatus(StatusLead.EM_CONTATO);
        this.status = StatusLead.DIAGNOSTICO;
    }

    /** A partir de EM_CONTATO (caminho direto já existente) ou DIAGNOSTICO (M25, etapa nova). */
    public void moverParaProposta() {
        if (status != StatusLead.EM_CONTATO && status != StatusLead.DIAGNOSTICO) {
            throw new IllegalStateException(
                    "Lead precisa estar em EM_CONTATO ou DIAGNOSTICO para essa transição (está em " + status + ").");
        }
        this.status = StatusLead.PROPOSTA;
    }

    /** Só a partir de PROPOSTA — venda fechada. Não cria conta de mentorado (isso é do E11,
     * ver ROADMAP.md M05), só registra o resultado comercial. */
    public void fechar(Plano planoFechado) {
        exigirStatus(StatusLead.PROPOSTA);
        this.status = StatusLead.FECHADO;
        this.planoFechado = planoFechado;
        this.dataFechamento = Instant.now();
    }

    /** M25 — "formulário único de venda". Só a partir de PROPOSTA, mesma guarda de
     * {@link #fechar(Plano)} — os dois convivem, nenhum lead-fechamento existente precisa migrar
     * pra este caminho. Overload sem taxaPlataformaRetida (gap 7) — todo chamador que não conhece
     * taxa de plataforma continua funcionando sem mudar nada. */
    public void fecharVenda(ProdutoVenda produtoVenda, OrigemVenda origemVenda, BigDecimal valorTotalVenda,
                             BigDecimal valorPagoNoAto, FormaPagamento formaPagamento) {
        fecharVenda(produtoVenda, origemVenda, valorTotalVenda, valorPagoNoAto, formaPagamento, null);
    }

    /** Gap 7 (raio-x + pesquisa da taxa real da Hotmart, confirmado 19/07/2026) —
     * taxaPlataformaRetida é o valor retido pelo gateway antes de repassar pra SAW. A soma
     * valorPagoNoAto + taxaPlataformaRetida representa o total efetivamente contabilizado da
     * venda (o restante, se houver, vira parcela de verdade); a garantia de que essa soma não
     * ultrapassa valorTotalVenda vive em {@code LeadService.fecharVenda} (mesmo critério de
     * validação B3 já usado pra valorPagoNoAto sozinho, não duplicado aqui na entidade). */
    public void fecharVenda(ProdutoVenda produtoVenda, OrigemVenda origemVenda, BigDecimal valorTotalVenda,
                             BigDecimal valorPagoNoAto, FormaPagamento formaPagamento, BigDecimal taxaPlataformaRetida) {
        exigirStatus(StatusLead.PROPOSTA);
        this.status = StatusLead.FECHADO;
        this.produtoVenda = produtoVenda;
        this.origemVenda = origemVenda;
        this.valorTotalVenda = valorTotalVenda;
        this.valorPagoNoAto = valorPagoNoAto;
        this.formaPagamento = formaPagamento;
        this.taxaPlataformaRetida = taxaPlataformaRetida;
        this.dataFechamento = Instant.now();
    }

    /** Desvio a partir de qualquer estado não-terminal (mesmo lead pode ser perdido logo na
     * solicitação, sem nunca ter sido contatado — ex.: spam, fora do público-alvo). */
    public void perder(String motivo) {
        if (status == StatusLead.FECHADO || status == StatusLead.PERDIDO) {
            throw new IllegalStateException("Lead já está em um estado final (" + status + ").");
        }
        this.status = StatusLead.PERDIDO;
        this.motivoPerdido = motivo;
        this.dataFechamento = Instant.now();
    }

    /** H11.1 (M06) — fecha a pendência deixada pelo M05: liga o lead à conta de mentorado
     * criada a partir dele, sem o qual não haveria como rastrear a origem depois que o funil
     * já rodou. Só faz sentido em FECHADO e só uma vez (não pode religar). */
    public void vincularMentorado(Mentorado mentorado) {
        exigirStatus(StatusLead.FECHADO);
        if (this.mentorado != null) {
            throw new IllegalStateException("Lead já está vinculado a um mentorado.");
        }
        this.mentorado = mentorado;
    }

    public Mentorado getMentorado() {
        return mentorado;
    }

    private void exigirStatus(StatusLead esperado) {
        if (status != esperado) {
            throw new IllegalStateException(
                    "Lead precisa estar em " + esperado + " para essa transição (está em " + status + ").");
        }
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefone() {
        return telefone;
    }

    public String getMensagem() {
        return mensagem;
    }

    public Plano getPlanoInteresse() {
        return planoInteresse;
    }

    public StatusLead getStatus() {
        return status;
    }

    public Colaborador getVendedor() {
        return vendedor;
    }

    public Plano getPlanoFechado() {
        return planoFechado;
    }

    public String getMotivoPerdido() {
        return motivoPerdido;
    }

    public Instant getDataFechamento() {
        return dataFechamento;
    }

    public TipoContrato getTipoContratoFechado() {
        return tipoContratoFechado;
    }

    public ProdutoVenda getProdutoVenda() {
        return produtoVenda;
    }

    public OrigemVenda getOrigemVenda() {
        return origemVenda;
    }

    public BigDecimal getValorTotalVenda() {
        return valorTotalVenda;
    }

    public BigDecimal getValorPagoNoAto() {
        return valorPagoNoAto;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public BigDecimal getTaxaPlataformaRetida() {
        return taxaPlataformaRetida;
    }
}
