package com.sawhub.hub.mentoria.dto;

import com.sawhub.hub.conteudo.Conteudo;
import com.sawhub.hub.mentoria.Ata;
import com.sawhub.hub.mentoria.Mentoria;
import com.sawhub.hub.mentoria.StatusMentoria;
import com.sawhub.hub.mentoria.TipoMentoria;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/** H5.1-H5.3 (M12) — resposta mentee-facing, deliberadamente mais enxuta que {@link MentoriaResponse}
 * (Admin): sem lista de participantes (o mentorado já sabe quem é), ata reduzida a resumo+data
 * (nunca transcrição/erro de processamento/sugestões — dado interno do pipeline de IA), materiais
 * já filtrados por publicado+plano. Ver Suposições do Blueprint M12 no ROADMAP.md. */
public record MentoriaMentoradoResponse(
        UUID id,
        TipoMentoria tipo,
        String mentorNome,
        Instant dataHora,
        Integer duracaoMin,
        String linkOnline,
        String local,
        StatusMentoria status,
        boolean podeEntrarAgora,
        AtaResumo ata,
        List<MaterialResumoResponse> materiaisRecomendados
) {
    // spec.md não define a janela — 10min antes do início até o fim previsto é uma escolha
    // documentada, não um valor "mágico" (ver Suposições do Blueprint M12 no ROADMAP.md).
    private static final long JANELA_ENTRADA_ANTES_MIN = 10;

    public record AtaResumo(String resumo, Instant publicadaEm) {
        public static AtaResumo from(Ata ata) {
            return new AtaResumo(ata.getResumo(), ata.getPublicadaEm());
        }
    }

    public static MentoriaMentoradoResponse from(Mentoria m, Ata ataPublicada, List<Conteudo> materiaisVisiveis, Instant agora) {
        boolean podeEntrarAgora = podeEntrarAgora(m, agora);
        AtaResumo ata = ataPublicada == null ? null : AtaResumo.from(ataPublicada);
        List<MaterialResumoResponse> materiais = materiaisVisiveis.stream().map(MaterialResumoResponse::from).toList();
        return new MentoriaMentoradoResponse(m.getId(), m.getTipo(), m.getMentor().getNome(), m.getDataHora(),
                m.getDuracaoMin(), m.getLinkOnline(), m.getLocal(), m.getStatus(), podeEntrarAgora, ata, materiais);
    }

    // H5.1: "Dado uma mentoria agendada online, quando chega o horário, então o botão 'Entrar na
    // reunião' fica disponível." Só mentoria com link (online), ainda não finalizada/cancelada, e
    // dentro da janela [dataHora - 10min, dataHora + duracaoMin].
    private static boolean podeEntrarAgora(Mentoria m, Instant agora) {
        if (m.getLinkOnline() == null) {
            return false;
        }
        if (m.getStatus() != StatusMentoria.AGENDADA && m.getStatus() != StatusMentoria.CONFIRMADA) {
            return false;
        }
        Instant inicioJanela = m.getDataHora().minus(JANELA_ENTRADA_ANTES_MIN, ChronoUnit.MINUTES);
        Instant fimJanela = m.getDataHora().plus(m.getDuracaoMin(), ChronoUnit.MINUTES);
        return !agora.isBefore(inicioJanela) && !agora.isAfter(fimJanela);
    }
}
