package com.sawhub.hub.mentorado.dto;

import com.sawhub.hub.mentorado.EstadoImplementacao;
import com.sawhub.hub.mentorado.RespostaSimNao;
import com.sawhub.hub.mentorado.TipoContrato;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** M23 item 4 (bulk-CREATE, 19/07/2026) — uma linha já validada do CSV de importação, pronta pra
 * virar Lead+Usuario+Mentorado(+Diagnóstico Inicial). Superset de {@link CriarMentoradoDiretoRequest}:
 * o formulário único do Admin não pede nomeFantasia/cnpj/sócios/diagnóstico, mas a migração real
 * de ~40 empresas do Notion carrega esses dados.
 *
 * <p>M28 (change request, 21/07/2026) — "import único": {@code mentoradoExistenteId} não-nulo
 * marca a linha como ATUALIZAÇÃO de um mentorado já cadastrado (resolvido por e-mail), em vez de
 * criação. Antes disso, e-mail já existente era sempre erro de validação — dois botões de import
 * confusos na tela (um só cria, outro só atualiza) viraram um só. */
public record ImportarMentoradoDiretoLinha(
        String email,
        String nome,
        String negocio,
        String telefone,
        TipoContrato tipoContrato,
        BigDecimal valorContrato,
        LocalDate dataFechamentoContrato,
        String nomeFantasia,
        String cnpj,
        String socios,
        BigDecimal faturamentoAnual,
        Integer quantidadeColaboradores,
        Boolean empresaRegularizada,
        Integer quantidadeLojas,
        RespostaSimNao cmvDefinido,
        String cmvDetalhe,
        String tempoMedioAtendimento,
        EstadoImplementacao culturaConstruida,
        EstadoImplementacao processosDesenhados,
        UUID mentoradoExistenteId
) {
    /** Só cria a linha 1:1 de {@code mentorado_diagnostico_inicial} quando pelo menos um campo do
     * bloco veio preenchido — a migração real não traz diagnóstico pronto pra todo mundo (é
     * levantado depois, incrementalmente, pela Leia antes da 1ª reunião). */
    public boolean temDadosDeDiagnostico() {
        return faturamentoAnual != null || quantidadeColaboradores != null || empresaRegularizada != null
                || quantidadeLojas != null || cmvDefinido != null || cmvDetalhe != null
                || tempoMedioAtendimento != null || culturaConstruida != null || processosDesenhados != null;
    }
}
