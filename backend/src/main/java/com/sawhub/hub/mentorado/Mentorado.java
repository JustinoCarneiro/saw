package com.sawhub.hub.mentorado;

import com.sawhub.hub.common.BaseEntity;
import com.sawhub.hub.security.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.ColumnTransformer;

@Entity
@Table(name = "mentorado")
public class Mentorado extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(nullable = false)
    private String nome;

    private String negocio;

    // Pass transversal de pgcrypto (Fase 5): é literalmente "financeiro do mentorado" (CLAUDE.md
    // § Criptografia), ao contrário do DRE interno da SAW (M04, deliberadamente fora de escopo).
    // Nunca aparece em SUM()/ORDER BY do Postgres — o ranking do E17 (ConsolidatedService) ordena
    // em memória Java depois do fetch, então criptografar não quebra a agregação. nome/telefone/
    // busca continuam de fora (ver justificativa em V19__pgcrypto_dados_sensiveis.sql): nome é
    // usado em LOWER(m.nome) LIKE (MentoradoRepository.buscarComFiltro), criptografar quebraria a
    // busca do Admin.
    @Column(name = "crescimento_faturamento_pct", nullable = false, columnDefinition = "bytea")
    @ColumnTransformer(
            read = "pgp_sym_decrypt(crescimento_faturamento_pct, current_setting('app.encryption_key'))::numeric",
            write = "pgp_sym_encrypt(?::text, current_setting('app.encryption_key'))")
    private BigDecimal crescimentoFaturamentoPct = BigDecimal.ZERO;

    @Column(name = "ferramentas_concluidas", nullable = false)
    private Integer ferramentasConcluidas = 0;

    @Column(name = "ferramentas_total", nullable = false)
    private Integer ferramentasTotal = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusMentorado status = StatusMentorado.ATIVO;

    @Column(columnDefinition = "bytea")
    @ColumnTransformer(
            read = "pgp_sym_decrypt(telefone, current_setting('app.encryption_key'))",
            write = "pgp_sym_encrypt(?, current_setting('app.encryption_key'))")
    private String telefone;

    @Column(columnDefinition = "bytea")
    @ColumnTransformer(
            read = "pgp_sym_decrypt(bio, current_setting('app.encryption_key'))",
            write = "pgp_sym_encrypt(?, current_setting('app.encryption_key'))")
    private String bio;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    // H9.2 — marca a primeira vez que a jornada (XP/conquistas) deste mentorado foi computada
    // depois da migração V18. Distingue "essa conquista já era verdadeira antes de rastrearmos"
    // (backfill, sem data fabricada) de "acabou de acontecer" (data real) — ver
    // PerfilJornadaService.sincronizarConquistas.
    @Column(name = "conquistas_observadas_em")
    private Instant conquistasObservadasEm;

    // M23 (change request pós-MVP, 17/07/2026) — dados de contrato levantados do Notion real da
    // operação ("CRM Saw"). nomeFantasia/dataFechamentoContrato/documentoContratoUrl não são
    // sensíveis o bastante pra pgcrypto (não são PII de indivíduo, e dataFechamentoContrato
    // precisa ficar filtrável); cnpj/socios/valorContrato entram criptografados, mesmo critério
    // do V19 (nunca aparecem em WHERE/ORDER BY/SUM nesta leva).
    @Column(name = "nome_fantasia", length = 255)
    private String nomeFantasia;

    @Column(columnDefinition = "bytea")
    @ColumnTransformer(
            read = "pgp_sym_decrypt(cnpj, current_setting('app.encryption_key'))",
            write = "pgp_sym_encrypt(?, current_setting('app.encryption_key'))")
    private String cnpj;

    // Texto livre "Nome 1; Nome 2" (Suposição 2 do Blueprint M23) — o Notion só usa isso como
    // referência de contato, nenhum fluxo do produto precisa consultar sócio individualmente
    // ainda; promover pra tabela própria é trivial depois se um caso de uso real pedir.
    @Column(columnDefinition = "bytea")
    @ColumnTransformer(
            read = "pgp_sym_decrypt(socios, current_setting('app.encryption_key'))",
            write = "pgp_sym_encrypt(?, current_setting('app.encryption_key'))")
    private String socios;

    @Column(name = "valor_contrato", columnDefinition = "bytea")
    @ColumnTransformer(
            read = "pgp_sym_decrypt(valor_contrato, current_setting('app.encryption_key'))::numeric",
            write = "pgp_sym_encrypt(?::text, current_setting('app.encryption_key'))")
    private BigDecimal valorContrato;

    @Column(name = "data_fechamento_contrato")
    private LocalDate dataFechamentoContrato;

    @Column(name = "documento_contrato_url", length = 500)
    private String documentoContratoUrl;

    // Nullable de propósito: vendas de INGRESSO_EVENTO/PRODUTO_DIGITAL/FORMULA_SAW/
    // FORMACAO_PROFISSIONAL/FICHA_TECNICA_LUCRATIVA (MentoradoAdminService.mapearTipoContrato)
    // legitimamente não têm tipo de contrato de mentoria.
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_contrato")
    private TipoContrato tipoContrato;

    // E17/M27 (change request pós-MVP, 19/07/2026) — as 4 ferramentas obrigatórias nomeadas do
    // ranking, mesmo enum de 3 estados já usado em MentoradoDiagnosticoInicial (SIM/NAO/
    // EM_CONSTRUCAO). ferramentasConcluidas/ferramentasTotal (acima) continuam existindo e sendo
    // lidos exatamente como antes por ConsolidatedRepository — só passam a ser recalculados a
    // partir destes 4 campos em vez de editáveis livremente, ver atualizarFerramentasObrigatorias.
    @Enumerated(EnumType.STRING)
    @Column(name = "ferramenta_dre", nullable = false)
    private EstadoImplementacao ferramentaDre = EstadoImplementacao.NAO;

    @Enumerated(EnumType.STRING)
    @Column(name = "ferramenta_manual_cultura", nullable = false)
    private EstadoImplementacao ferramentaManualCultura = EstadoImplementacao.NAO;

    @Enumerated(EnumType.STRING)
    @Column(name = "ferramenta_ficha_tecnica", nullable = false)
    private EstadoImplementacao ferramentaFichaTecnica = EstadoImplementacao.NAO;

    @Enumerated(EnumType.STRING)
    @Column(name = "ferramenta_manual_processos", nullable = false)
    private EstadoImplementacao ferramentaManualProcessos = EstadoImplementacao.NAO;

    // E17/M27 — "dois eixos de acompanhamento", preenchidos manualmente pelo mentor/time de
    // sucesso (não calculados). Nullable: nem todo mentorado já passou por uma "análise pós-
    // check-in". Snapshot único, sem histórico nesta leva (Suposição 3 do Blueprint M27) — o
    // status EM_DIA/ATENCAO/ATRASADO calculado em ConsolidatedService continua existindo do
    // mesmo jeito de sempre, estes dois eixos são informação adicional, não substituição.
    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_engajamento")
    private NivelEngajamento nivelEngajamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "risco_churn")
    private RiscoChurn riscoChurn;

    @Column(name = "acompanhamento_avaliado_em")
    private Instant acompanhamentoAvaliadoEm;

    protected Mentorado() {
    }

    public Mentorado(Usuario usuario, String nome, String negocio,
                      BigDecimal crescimentoFaturamentoPct, Integer ferramentasConcluidas, Integer ferramentasTotal) {
        this.usuario = usuario;
        this.nome = nome;
        this.negocio = negocio;
        this.crescimentoFaturamentoPct = crescimentoFaturamentoPct;
        this.ferramentasConcluidas = ferramentasConcluidas;
        this.ferramentasTotal = ferramentasTotal;
        this.status = StatusMentorado.ATIVO;
    }

    /** H11.1 — edição administrativa (nome, negócio); status muda por {@link #ativar()}/{@link #desativar()}. */
    public void atualizar(String nome, String negocio) {
        this.nome = nome;
        this.negocio = negocio;
    }

    public void ativar() {
        this.status = StatusMentorado.ATIVO;
    }

    public void desativar() {
        this.status = StatusMentorado.INATIVO;
    }

    /** H9.1 — autoedição do mentorado: só contato/preferências, nunca identidade (esses são admin-only via {@link #atualizar}). */
    public void atualizarPerfil(String telefone, String bio, String fotoUrl) {
        this.telefone = telefone;
        this.bio = bio;
        this.fotoUrl = fotoUrl;
    }

    /** M23 — edição administrativa dos dados de contrato (H11.1 estendida). vencimentoContrato
     * é sempre derivado de tipoContrato+dataFechamentoContrato ({@link TipoContrato#calcularVencimento}),
     * nunca um valor de entrada — evita as duas fontes divergirem. */
    public void atualizarDadosContrato(String nomeFantasia, String cnpj, String socios,
                                        TipoContrato tipoContrato, BigDecimal valorContrato,
                                        LocalDate dataFechamentoContrato) {
        this.nomeFantasia = nomeFantasia;
        this.cnpj = cnpj;
        this.socios = socios;
        this.tipoContrato = tipoContrato;
        this.valorContrato = valorContrato;
        this.dataFechamentoContrato = dataFechamentoContrato;
    }

    public void atualizarDocumentoContrato(String documentoContratoUrl) {
        this.documentoContratoUrl = documentoContratoUrl;
    }

    /** Deriva o vencimento a partir do tipo de contrato — null se tipoContrato ou
     * dataFechamentoContrato ainda não foram preenchidos, ou se o tipo não tem prazo fixo
     * (Consultoria, "esporádica"). */
    public LocalDate getVencimentoContrato() {
        if (tipoContrato == null || dataFechamentoContrato == null) {
            return null;
        }
        return tipoContrato.calcularVencimento(dataFechamentoContrato);
    }

    public void marcarConquistasObservadas() {
        if (this.conquistasObservadasEm == null) {
            this.conquistasObservadasEm = Instant.now();
        }
    }

    /** E17/M27 — seta as 4 ferramentas nomeadas e recalcula ferramentasConcluidas/ferramentasTotal
     * na mesma chamada (ferramentasTotal fixo em 4; ferramentasConcluidas = quantas das 4 estão
     * SIM — EM_CONSTRUCAO conta como não concluída, Suposição 2 do Blueprint M27). É isso que
     * mantém ConsolidatedRepository/MentoradoConsolidadoRow/Response sem nenhuma mudança: o dado
     * que eles já leem continua no mesmo formato de sempre. */
    public void atualizarFerramentasObrigatorias(EstadoImplementacao ferramentaDre, EstadoImplementacao ferramentaManualCultura,
                                                  EstadoImplementacao ferramentaFichaTecnica, EstadoImplementacao ferramentaManualProcessos) {
        this.ferramentaDre = ferramentaDre != null ? ferramentaDre : EstadoImplementacao.NAO;
        this.ferramentaManualCultura = ferramentaManualCultura != null ? ferramentaManualCultura : EstadoImplementacao.NAO;
        this.ferramentaFichaTecnica = ferramentaFichaTecnica != null ? ferramentaFichaTecnica : EstadoImplementacao.NAO;
        this.ferramentaManualProcessos = ferramentaManualProcessos != null ? ferramentaManualProcessos : EstadoImplementacao.NAO;
        this.ferramentasTotal = 4;
        this.ferramentasConcluidas = (int) java.util.stream.Stream.of(
                        this.ferramentaDre, this.ferramentaManualCultura, this.ferramentaFichaTecnica, this.ferramentaManualProcessos)
                .filter(e -> e == EstadoImplementacao.SIM)
                .count();
    }

    /** E17/M27 — "dois eixos de acompanhamento", preenchimento manual (ver Javadoc dos campos).
     * Semântica de PATCH: campo nulo na chamada não apaga o valor já registrado, só o campo
     * explicitamente informado muda — dá pra registrar/atualizar um eixo por vez. */
    public void atualizarAcompanhamento(NivelEngajamento nivelEngajamento, RiscoChurn riscoChurn) {
        if (nivelEngajamento != null) {
            this.nivelEngajamento = nivelEngajamento;
        }
        if (riscoChurn != null) {
            this.riscoChurn = riscoChurn;
        }
        this.acompanhamentoAvaliadoEm = Instant.now();
    }

    public StatusMentorado getStatus() {
        return status;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getNome() {
        return nome;
    }

    public String getNegocio() {
        return negocio;
    }

    public BigDecimal getCrescimentoFaturamentoPct() {
        return crescimentoFaturamentoPct;
    }

    public Integer getFerramentasConcluidas() {
        return ferramentasConcluidas;
    }

    public Integer getFerramentasTotal() {
        return ferramentasTotal;
    }

    public String getTelefone() {
        return telefone;
    }

    public String getBio() {
        return bio;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public Instant getConquistasObservadasEm() {
        return conquistasObservadasEm;
    }

    public String getNomeFantasia() {
        return nomeFantasia;
    }

    public String getCnpj() {
        return cnpj;
    }

    public String getSocios() {
        return socios;
    }

    public BigDecimal getValorContrato() {
        return valorContrato;
    }

    public LocalDate getDataFechamentoContrato() {
        return dataFechamentoContrato;
    }

    public String getDocumentoContratoUrl() {
        return documentoContratoUrl;
    }

    public TipoContrato getTipoContrato() {
        return tipoContrato;
    }

    public EstadoImplementacao getFerramentaDre() {
        return ferramentaDre;
    }

    public EstadoImplementacao getFerramentaManualCultura() {
        return ferramentaManualCultura;
    }

    public EstadoImplementacao getFerramentaFichaTecnica() {
        return ferramentaFichaTecnica;
    }

    public EstadoImplementacao getFerramentaManualProcessos() {
        return ferramentaManualProcessos;
    }

    public NivelEngajamento getNivelEngajamento() {
        return nivelEngajamento;
    }

    public RiscoChurn getRiscoChurn() {
        return riscoChurn;
    }

    public Instant getAcompanhamentoAvaliadoEm() {
        return acompanhamentoAvaliadoEm;
    }
}
