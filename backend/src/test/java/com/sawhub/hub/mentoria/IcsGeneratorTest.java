package com.sawhub.hub.mentoria;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** H5.3 — formato RFC 5545 mínimo. Se isto quebrar, o .ics baixado não abre no Google/Outlook/
 * Apple Calendar (linha malformada, timestamp fora do formato UTC esperado, etc.). */
class IcsGeneratorTest {

    private static final UUID MENTORIA_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant INICIO = Instant.parse("2026-07-15T14:00:00Z");
    private static final Instant FIM = Instant.parse("2026-07-15T15:00:00Z");
    private static final Instant AGORA = Instant.parse("2026-07-09T10:00:00Z");

    @Test
    void geraVeventComCamposObrigatorios() {
        String ics = new String(IcsGenerator.gerar(MENTORIA_ID, "Mentoria com Brayan", INICIO, FIM, "https://meet.google.com/x", AGORA),
                StandardCharsets.UTF_8);

        assertThat(ics).startsWith("BEGIN:VCALENDAR\r\n");
        assertThat(ics).endsWith("END:VCALENDAR\r\n");
        assertThat(ics).contains("UID:" + MENTORIA_ID + "@sawhub.com.br\r\n");
        assertThat(ics).contains("DTSTART:20260715T140000Z\r\n");
        assertThat(ics).contains("DTEND:20260715T150000Z\r\n");
        assertThat(ics).contains("DTSTAMP:20260709T100000Z\r\n");
        assertThat(ics).contains("SUMMARY:Mentoria com Brayan\r\n");
        assertThat(ics).contains("LOCATION:https://meet.google.com/x\r\n");
    }

    @Test
    void semLocalOmiteALinhaLocation() {
        String ics = new String(IcsGenerator.gerar(MENTORIA_ID, "Mentoria", INICIO, FIM, null, AGORA), StandardCharsets.UTF_8);
        assertThat(ics).doesNotContain("LOCATION:");
    }

    @Test
    void escapaVirgulaPontoEVirgulaEBarraInvertidaNoSummary() {
        String ics = new String(IcsGenerator.gerar(MENTORIA_ID, "Mentoria: pauta, ideias; extras \\ obs", INICIO, FIM, null, AGORA),
                StandardCharsets.UTF_8);
        assertThat(ics).contains("SUMMARY:Mentoria: pauta\\, ideias\\; extras \\\\ obs\r\n");
    }

    @Test
    void usaCrlfComoTerminadorDeLinhaRfc5545SemQuebrasSoltas() {
        String ics = new String(IcsGenerator.gerar(MENTORIA_ID, "Mentoria", INICIO, FIM, null, AGORA), StandardCharsets.UTF_8);
        // Remove todo \r\n válido; se sobrar \r ou \n solto, alguma linha não seguiu CRLF.
        String semCrlf = ics.replace("\r\n", "");
        assertThat(semCrlf).doesNotContain("\r").doesNotContain("\n");
        assertThat(ics.split("\r\n", -1)).hasSizeGreaterThan(5);
    }

    // Achado do revisor-seguranca no M12: \r solto (sem \n) num campo livre (ex.: local da
    // mentoria, nome do mentor) não era escapado antes desta correção — como o gerador usa \r\n
    // como terminador de linha, um \r cru vira um terminador de linha extra pra qualquer parser
    // tolerante, permitindo injetar propriedades/componentes forjados (ex.: um VALARM) dentro do
    // VEVENT. Testa os dois campos que passam por escapar(): summary e local.
    @Test
    void crSoltoNoSummaryNaoInjetaLinhaNovaNoIcs() {
        String ics = new String(
                IcsGenerator.gerar(MENTORIA_ID, "Sala 3\rBEGIN:VALARM\rTRIGGER:-PT15M\rEND:VALARM", INICIO, FIM, null, AGORA),
                StandardCharsets.UTF_8);
        // "BEGIN:VALARM" pode aparecer DENTRO do texto escapado do SUMMARY (correto, inofensivo)
        // — o que não pode existir é como linha própria, delimitada por \r\n de verdade.
        assertThat(ics).doesNotContain("\r\nBEGIN:VALARM\r\n");
        assertThat(ics).contains("SUMMARY:Sala 3\\nBEGIN:VALARM\\nTRIGGER:-PT15M\\nEND:VALARM\r\n");
    }

    @Test
    void crSoltoNoLocalNaoInjetaLinhaNovaNoIcs() {
        String ics = new String(
                IcsGenerator.gerar(MENTORIA_ID, "Mentoria", INICIO, FIM, "Sala 3\rX-FORJADO:valor", AGORA),
                StandardCharsets.UTF_8);
        assertThat(ics).doesNotContain("\r\nX-FORJADO:valor\r\n");
        assertThat(ics).contains("LOCATION:Sala 3\\nX-FORJADO:valor\r\n");
    }
}
