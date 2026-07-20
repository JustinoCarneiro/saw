package com.sawhub.hub.mentorado.dto;

import com.sawhub.hub.mentorado.EstadoImplementacao;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.NivelEngajamento;
import com.sawhub.hub.mentorado.Plano;
import com.sawhub.hub.mentorado.RiscoChurn;
import com.sawhub.hub.mentorado.StatusMentorado;
import com.sawhub.hub.mentorado.TipoContrato;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// H11.1 (Fase 5) — telefone/bio/fotoUrl passam a ser também editáveis pelo Admin (antes só
// autoedição do mentorado, H9.1) — o Admin precisa conseguir completar o cadastro sem depender do
// mentorado logar primeiro. Autoedição continua existindo em paralelo.
// M23 (change request pós-MVP) — campos de contrato aditivos (nomeFantasia..vencimentoContrato);
// vencimentoContrato é sempre derivado (Mentorado.getVencimentoContrato()), nunca lido de coluna.
// areasInteresse removido nesta mesma leva (confirmado pelo cliente como não aplicável — "é
// geral, não precisa dessa área").
// E17/M27 (change request pós-MVP) — as 4 ferramentas obrigatórias nomeadas (ferramenta*) e os
// dois eixos de acompanhamento (nivelEngajamento/riscoChurn/acompanhamentoAvaliadoEm) são
// aditivos: ferramentasConcluidas/ferramentasTotal (não expostos aqui, só no Painel Consolidado)
// continuam existindo e sendo recalculados a partir das 4 ferramentas, ver Mentorado.java.
public record MentoradoResponse(
        UUID id,
        String nome,
        String email,
        String negocio,
        Plano plano,
        LocalDate vencimentoPlano,
        StatusMentorado status,
        String telefone,
        String bio,
        String fotoUrl,
        Instant criadoEm,
        String nomeFantasia,
        String cnpj,
        String socios,
        TipoContrato tipoContrato,
        BigDecimal valorContrato,
        LocalDate dataFechamentoContrato,
        LocalDate vencimentoContrato,
        String documentoContratoUrl,
        EstadoImplementacao ferramentaDre,
        EstadoImplementacao ferramentaManualCultura,
        EstadoImplementacao ferramentaFichaTecnica,
        EstadoImplementacao ferramentaManualProcessos,
        NivelEngajamento nivelEngajamento,
        RiscoChurn riscoChurn,
        Instant acompanhamentoAvaliadoEm
) {
    public static MentoradoResponse from(Mentorado m) {
        return new MentoradoResponse(m.getId(), m.getNome(), m.getUsuario().getEmail(), m.getNegocio(),
                m.getPlano(), m.getVencimentoPlano(), m.getStatus(), m.getTelefone(), m.getBio(),
                m.getFotoUrl(), m.getCriadoEm(),
                m.getNomeFantasia(), m.getCnpj(), m.getSocios(), m.getTipoContrato(), m.getValorContrato(),
                m.getDataFechamentoContrato(), m.getVencimentoContrato(), m.getDocumentoContratoUrl(),
                m.getFerramentaDre(), m.getFerramentaManualCultura(), m.getFerramentaFichaTecnica(),
                m.getFerramentaManualProcessos(), m.getNivelEngajamento(), m.getRiscoChurn(),
                m.getAcompanhamentoAvaliadoEm());
    }
}
