package com.sawhub.hub.mentorado.dto;

import com.sawhub.hub.conteudo.Conteudo;
import com.sawhub.hub.mentoria.Mentoria;
import com.sawhub.hub.mentoria.TipoMentoria;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** H2.1–H2.3 (M08) — agregado de leitura pro Dashboard do Mentorado, sem entidade própria: junta
 * {@code Mentorado}, {@code Encaminhamento}, {@code Mentoria} e {@code Conteudo} já existentes. */
public record DashboardMentoradoResponse(
        String nome,
        int evolucaoGeralPct,
        long tarefasAbertas,
        Integer metaSemanalPct,
        Compromisso proximaReuniao,
        List<Compromisso> compromissos,
        DicaDestaque dicaDestaque,
        List<String> avisos
) {
    public record Compromisso(UUID id, TipoMentoria tipo, Instant dataHora, String linkOnline, String local) {
        public static Compromisso from(Mentoria m) {
            return new Compromisso(m.getId(), m.getTipo(), m.getDataHora(), m.getLinkOnline(), m.getLocal());
        }
    }

    public record DicaDestaque(UUID id, String titulo, String url) {
        public static DicaDestaque from(Conteudo c) {
            return new DicaDestaque(c.getId(), c.getTitulo(), c.getUrl());
        }
    }
}
