package com.sawhub.hub.mentoria.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.sawhub.hub.conteudo.Conteudo;
import com.sawhub.hub.conteudo.TipoConteudo;
import com.sawhub.hub.mentorado.Plano;
import com.sawhub.hub.mentoria.Ata;
import com.sawhub.hub.mentoria.Mentoria;
import com.sawhub.hub.mentoria.StatusMentoria;
import com.sawhub.hub.mentoria.TipoMentoria;
import com.sawhub.hub.team.Area;
import com.sawhub.hub.team.Colaborador;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** H5.1 — regra central: janela de "posso entrar agora" (10min antes do início até o fim
 * previsto). Se isto quebrar, o mentorado vê o botão "Entrar na reunião" cedo/tarde demais, ou
 * nunca. Ver Suposições do Blueprint M12 no ROADMAP.md pra por que 10min foi a escolha. */
class MentoriaMentoradoResponseTest {

    private static final Instant DATA_HORA = Instant.parse("2026-07-15T14:00:00Z");
    private static final int DURACAO_MIN = 60;

    private static Colaborador mentor() {
        return new Colaborador(null, "Brayan Silva", Area.GESTAO_PERFORMANCE, null, null);
    }

    private static Mentoria mentoriaOnline(StatusMentoria status) {
        Mentoria m = new Mentoria(TipoMentoria.INDIVIDUAL, mentor(), Set.of(), DATA_HORA, DURACAO_MIN,
                "https://meet.google.com/abc-defg-hij", null);
        aplicarStatus(m, status);
        return m;
    }

    private static Mentoria mentoriaPresencial() {
        Mentoria m = new Mentoria(TipoMentoria.GRUPO, mentor(), Set.of(), DATA_HORA, DURACAO_MIN, null, "SAW HUB — Sala 1");
        return m;
    }

    private static void aplicarStatus(Mentoria m, StatusMentoria alvo) {
        switch (alvo) {
            case AGENDADA -> { }
            case CONFIRMADA -> m.confirmar();
            case REALIZADA -> { m.confirmar(); m.realizar(); }
            case CANCELADA -> m.cancelar();
        }
    }

    @Test
    void exatamenteNoInicioDaJanelaPodeEntrar() {
        var r = MentoriaMentoradoResponse.from(mentoriaOnline(StatusMentoria.CONFIRMADA), null, List.of(),
                DATA_HORA.minusSeconds(600)); // -10min exato
        assertThat(r.podeEntrarAgora()).isTrue();
    }

    @Test
    void umSegundoAntesDaJanelaNaoPodeEntrar() {
        var r = MentoriaMentoradoResponse.from(mentoriaOnline(StatusMentoria.CONFIRMADA), null, List.of(),
                DATA_HORA.minusSeconds(601));
        assertThat(r.podeEntrarAgora()).isFalse();
    }

    @Test
    void exatamenteNoFimPrevistoAindaPodeEntrar() {
        var r = MentoriaMentoradoResponse.from(mentoriaOnline(StatusMentoria.CONFIRMADA), null, List.of(),
                DATA_HORA.plusSeconds(DURACAO_MIN * 60L)); // dataHora + duracaoMin exato
        assertThat(r.podeEntrarAgora()).isTrue();
    }

    @Test
    void umSegundoDepoisDoFimPrevistoNaoPodeMaisEntrar() {
        var r = MentoriaMentoradoResponse.from(mentoriaOnline(StatusMentoria.CONFIRMADA), null, List.of(),
                DATA_HORA.plusSeconds(DURACAO_MIN * 60L + 1));
        assertThat(r.podeEntrarAgora()).isFalse();
    }

    @Test
    void dentroDaJanelaComStatusAgendadaTambemPodeEntrar() {
        var r = MentoriaMentoradoResponse.from(mentoriaOnline(StatusMentoria.AGENDADA), null, List.of(), DATA_HORA);
        assertThat(r.podeEntrarAgora()).isTrue();
    }

    @Test
    void canceladaNuncaPodeEntrarMesmoDentroDaJanela() {
        var r = MentoriaMentoradoResponse.from(mentoriaOnline(StatusMentoria.CANCELADA), null, List.of(), DATA_HORA);
        assertThat(r.podeEntrarAgora()).isFalse();
    }

    @Test
    void realizadaNuncaPodeEntrarMesmoDentroDaJanela() {
        var r = MentoriaMentoradoResponse.from(mentoriaOnline(StatusMentoria.REALIZADA), null, List.of(), DATA_HORA);
        assertThat(r.podeEntrarAgora()).isFalse();
    }

    @Test
    void semLinkOnlineNuncaPodeEntrarMesmoDentroDaJanela() {
        var r = MentoriaMentoradoResponse.from(mentoriaPresencial(), null, List.of(), DATA_HORA);
        assertThat(r.podeEntrarAgora()).isFalse();
    }

    @Test
    void ataNulaViraNullNaResposta() {
        var r = MentoriaMentoradoResponse.from(mentoriaOnline(StatusMentoria.AGENDADA), null, List.of(), DATA_HORA);
        assertThat(r.ata()).isNull();
    }

    @Test
    void ataPublicadaViraResumoESemDadoInternoDeIa() {
        Mentoria m = mentoriaOnline(StatusMentoria.REALIZADA);
        Ata ata = new Ata(m);
        ata.concluirProcessamento("transcrição bruta — nunca deve aparecer aqui", "Resumo da mentoria.");
        ata.publicar();

        var r = MentoriaMentoradoResponse.from(m, ata, List.of(), DATA_HORA);

        assertThat(r.ata().resumo()).isEqualTo("Resumo da mentoria.");
        assertThat(r.ata().publicadaEm()).isNotNull();
    }

    @Test
    void materiaisVisiveisSaoMapeadosNaResposta() {
        Conteudo c = new Conteudo("Ficha técnica", TipoConteudo.PLANILHA, "https://cdn.sawhub.com.br/x", Plano.GRATUITO);
        ReflectionTestUtils.setField(c, "id", UUID.randomUUID());

        var r = MentoriaMentoradoResponse.from(mentoriaOnline(StatusMentoria.AGENDADA), null, List.of(c), DATA_HORA);

        assertThat(r.materiaisRecomendados()).hasSize(1);
        assertThat(r.materiaisRecomendados().get(0).titulo()).isEqualTo("Ficha técnica");
    }
}
