package com.sawhub.hub.mentorado;

import com.sawhub.hub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.ColumnTransformer;

/** M23 (change request pós-MVP, 17/07/2026) — feito pela Leia (papel "Sucesso do Gestor") antes
 * da 1ª reunião do mentorado com o Mateus. 1:1 com Mentorado, mesmo padrão de Ata/Mentoria (M06):
 * nasce só quando alguém preenche, não em toda linha de mentorado. */
@Entity
@Table(name = "mentorado_diagnostico_inicial")
public class MentoradoDiagnosticoInicial extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "mentorado_id", nullable = false, unique = true)
    private Mentorado mentorado;

    // faturamentoAnual é a leitura do Diagnóstico Inicial (uma vez, no onboarding) — distinto de
    // Mentorado.crescimentoFaturamentoPct (E17, evolução contínua pro ranking). Criptografado
    // pelo mesmo critério do V19: nunca aparece em WHERE/ORDER BY/SUM nesta leva.
    @Column(name = "faturamento_anual", columnDefinition = "bytea")
    @ColumnTransformer(
            read = "pgp_sym_decrypt(faturamento_anual, current_setting('app.encryption_key'))::numeric",
            write = "pgp_sym_encrypt(?::text, current_setting('app.encryption_key'))")
    private BigDecimal faturamentoAnual;

    @Column(name = "quantidade_colaboradores")
    private Integer quantidadeColaboradores;

    @Column(name = "empresa_regularizada")
    private Boolean empresaRegularizada;

    @Column(name = "quantidade_lojas")
    private Integer quantidadeLojas;

    @Enumerated(EnumType.STRING)
    @Column(name = "cmv_definido")
    private RespostaSimNao cmvDefinido;

    @Column(name = "cmv_detalhe", length = 255)
    private String cmvDetalhe;

    @Column(name = "tempo_medio_atendimento", length = 100)
    private String tempoMedioAtendimento;

    @Enumerated(EnumType.STRING)
    @Column(name = "cultura_construida", nullable = false)
    private EstadoImplementacao culturaConstruida = EstadoImplementacao.NAO;

    @Enumerated(EnumType.STRING)
    @Column(name = "processos_desenhados", nullable = false)
    private EstadoImplementacao processosDesenhados = EstadoImplementacao.NAO;

    protected MentoradoDiagnosticoInicial() {
    }

    public MentoradoDiagnosticoInicial(Mentorado mentorado) {
        this.mentorado = mentorado;
    }

    public void atualizar(BigDecimal faturamentoAnual, Integer quantidadeColaboradores,
                           Boolean empresaRegularizada, Integer quantidadeLojas,
                           RespostaSimNao cmvDefinido, String cmvDetalhe, String tempoMedioAtendimento,
                           EstadoImplementacao culturaConstruida, EstadoImplementacao processosDesenhados) {
        this.faturamentoAnual = faturamentoAnual;
        this.quantidadeColaboradores = quantidadeColaboradores;
        this.empresaRegularizada = empresaRegularizada;
        this.quantidadeLojas = quantidadeLojas;
        this.cmvDefinido = cmvDefinido;
        this.cmvDetalhe = cmvDetalhe;
        this.tempoMedioAtendimento = tempoMedioAtendimento;
        this.culturaConstruida = culturaConstruida != null ? culturaConstruida : EstadoImplementacao.NAO;
        this.processosDesenhados = processosDesenhados != null ? processosDesenhados : EstadoImplementacao.NAO;
    }

    public Mentorado getMentorado() {
        return mentorado;
    }

    public BigDecimal getFaturamentoAnual() {
        return faturamentoAnual;
    }

    public Integer getQuantidadeColaboradores() {
        return quantidadeColaboradores;
    }

    public Boolean getEmpresaRegularizada() {
        return empresaRegularizada;
    }

    public Integer getQuantidadeLojas() {
        return quantidadeLojas;
    }

    public RespostaSimNao getCmvDefinido() {
        return cmvDefinido;
    }

    public String getCmvDetalhe() {
        return cmvDetalhe;
    }

    public String getTempoMedioAtendimento() {
        return tempoMedioAtendimento;
    }

    public EstadoImplementacao getCulturaConstruida() {
        return culturaConstruida;
    }

    public EstadoImplementacao getProcessosDesenhados() {
        return processosDesenhados;
    }
}
