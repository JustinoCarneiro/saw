package com.sawhub.hub.mentoria;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** H5.3 (M12) — gera um evento .ics (RFC 5545) mínimo pra download direto. Função pura (sem I/O,
 * sem estado), fácil de testar com instantes fixos. O link "Adicionar ao Google Calendar" cobre a
 * outra metade do "(.ics/Google)" da BDD e é montado 100% no frontend — ver ROADMAP.md M12. */
public final class IcsGenerator {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    private IcsGenerator() {
    }

    public static byte[] gerar(UUID mentoriaId, String summary, Instant inicio, Instant fim, String local, Instant agora) {
        StringBuilder ics = new StringBuilder()
                .append("BEGIN:VCALENDAR\r\n")
                .append("VERSION:2.0\r\n")
                .append("PRODID:-//SAW HUB//Mentorias//PT-BR\r\n")
                .append("BEGIN:VEVENT\r\n")
                .append("UID:").append(mentoriaId).append("@sawhub.com.br\r\n")
                .append("DTSTAMP:").append(TIMESTAMP.format(agora)).append("\r\n")
                .append("DTSTART:").append(TIMESTAMP.format(inicio)).append("\r\n")
                .append("DTEND:").append(TIMESTAMP.format(fim)).append("\r\n")
                .append("SUMMARY:").append(escapar(summary)).append("\r\n");
        if (local != null && !local.isBlank()) {
            ics.append("LOCATION:").append(escapar(local)).append("\r\n");
        }
        ics.append("END:VEVENT\r\n").append("END:VCALENDAR\r\n");
        return ics.toString().getBytes(StandardCharsets.UTF_8);
    }

    // RFC 5545 §3.3.11 — TEXT precisa escapar barra invertida, vírgula, ponto-e-vírgula e quebra
    // de linha (nessa ordem: a barra primeiro, senão escapa duas vezes o que os outros já escaparam).
    // Achado do revisor-seguranca no M12: \r sozinho (sem \n) não era escapado — como o próprio
    // gerador usa \r\n como terminador de linha, um \r cru dentro de um campo livre (ex.: local da
    // mentoria) vira um terminador de linha pra qualquer parser de calendário tolerante a CR solto,
    // permitindo injetar propriedades/componentes extras (ex.: um VALARM forjado) dentro do VEVENT.
    // \r\n e \r isolado colapsam pro mesmo \\n do LF — não precisam de dois tratamentos distintos.
    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace(",", "\\,").replace(";", "\\;")
                .replace("\r\n", "\\n").replace("\n", "\\n").replace("\r", "\\n");
    }
}
