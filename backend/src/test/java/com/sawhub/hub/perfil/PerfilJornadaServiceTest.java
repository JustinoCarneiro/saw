package com.sawhub.hub.perfil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.sawhub.hub.conteudo.ConteudoMentoradoService;
import com.sawhub.hub.conteudo.TipoConteudo;
import com.sawhub.hub.conteudo.dto.ConteudoMentoradoResponse;
import com.sawhub.hub.evento.InscricaoEvento;
import com.sawhub.hub.evento.InscricaoEventoRepository;
import com.sawhub.hub.evento.StatusInscricao;
import com.sawhub.hub.mentorado.EncaminhamentoRepository;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.mentorado.Plano;
import com.sawhub.hub.mentoria.MentoriaRepository;
import com.sawhub.hub.meta.MetaRepository;
import com.sawhub.hub.perfil.dto.JornadaResponse;
import com.sawhub.hub.security.Perfil;
import com.sawhub.hub.security.Usuario;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** H9.2 — RED primeiro: PerfilJornadaService ainda não existia neste ponto do ciclo. Stats,
 * XP, nível e conquistas são 100% derivados (ver Suposições 2/3 do Blueprint M15), então este
 * teste cobre a fórmula/limiares contra combinações de contadores mockados, não persistência. */
@ExtendWith(MockitoExtension.class)
class PerfilJornadaServiceTest {

    @Mock
    private MentoradoRepository mentoradoRepository;
    @Mock
    private ConteudoMentoradoService conteudoMentoradoService;
    @Mock
    private InscricaoEventoRepository inscricaoEventoRepository;
    @Mock
    private MentoriaRepository mentoriaRepository;
    @Mock
    private MetaRepository metaRepository;
    @Mock
    private EncaminhamentoRepository encaminhamentoRepository;

    private PerfilJornadaService service() {
        return new PerfilJornadaService(mentoradoRepository, conteudoMentoradoService, inscricaoEventoRepository,
                mentoriaRepository, metaRepository, encaminhamentoRepository);
    }

    private static Mentorado mentorado(UUID id, BigDecimal crescimento, int ferramentasConcluidas, int ferramentasTotal) {
        Usuario usuario = new Usuario("ana@anacosta.com.br", "hash", Perfil.MENTORADO);
        Mentorado m = new Mentorado(usuario, "Ana Costa", "Cantina Ana Costa", Plano.ESSENCIAL,
                crescimento, ferramentasConcluidas, ferramentasTotal);
        ReflectionTestUtils.setField(m, "id", id);
        return m;
    }

    private void mockarZerado(UUID usuarioId, Mentorado mentorado) {
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(conteudoMentoradoService.buscarCatalogo(eq(usuarioId), isNull(), isNull())).thenReturn(List.of());
        when(inscricaoEventoRepository.findByMentoradoId(mentorado.getId())).thenReturn(List.of());
        when(mentoriaRepository.buscarPorMentorado(mentorado)).thenReturn(List.of());
        when(metaRepository.buscarPorMentorado(eq(mentorado.getId()), isNull())).thenReturn(List.of());
        when(encaminhamentoRepository.buscarPorMentorado(eq(mentorado.getId()), isNull(), isNull())).thenReturn(List.of());
    }

    @Test
    void semNenhumaAtividadeFicaNoBronzeComXpZeroENenhumaConquista() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID(), BigDecimal.ZERO, 0, 0);
        mockarZerado(usuarioId, mentorado);

        JornadaResponse resposta = service().jornada(usuarioId);

        assertThat(resposta.nivelAtual()).isEqualTo(NivelJornada.BRONZE);
        assertThat(resposta.xp()).isZero();
        assertThat(resposta.xpProximoNivel()).isEqualTo(1500);
        assertThat(resposta.progressoPct()).isZero();
        assertThat(resposta.conquistas()).allMatch(c -> !c.desbloqueada());
    }

    @Test
    void statsESeusMultiplicadoresComputamOXpCorreto() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID(), BigDecimal.ZERO, 0, 0);
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));

        // 2 materiais acessados (favorito/assistido, tipo != VIDEO) + 1 dica assistida (VIDEO)
        var material1 = conteudo(TipoConteudo.DOCUMENTO, true, false);
        var material2 = conteudo(TipoConteudo.PLANILHA, false, true);
        var materialNaoAcessado = conteudo(TipoConteudo.DOCUMENTO, false, false);
        var dicaAssistida = conteudo(TipoConteudo.VIDEO, false, true);
        var dicaNaoAssistida = conteudo(TipoConteudo.VIDEO, true, false); // favoritada mas não assistida — não conta como "dica assistida"
        when(conteudoMentoradoService.buscarCatalogo(eq(usuarioId), isNull(), isNull()))
                .thenReturn(List.of(material1, material2, materialNaoAcessado, dicaAssistida, dicaNaoAssistida));

        var participou = inscricao(StatusInscricao.PARTICIPOU);
        var inscrita = inscricao(StatusInscricao.INSCRITA);
        when(inscricaoEventoRepository.findByMentoradoId(mentorado.getId())).thenReturn(List.of(participou, inscrita));
        when(mentoriaRepository.buscarPorMentorado(mentorado)).thenReturn(List.of());
        when(metaRepository.buscarPorMentorado(eq(mentorado.getId()), isNull())).thenReturn(List.of());
        when(encaminhamentoRepository.buscarPorMentorado(eq(mentorado.getId()), isNull(), isNull())).thenReturn(List.of());

        JornadaResponse resposta = service().jornada(usuarioId);

        assertThat(resposta.stats().materiaisAcessados()).isEqualTo(2);
        assertThat(resposta.stats().dicasAssistidas()).isEqualTo(1);
        assertThat(resposta.stats().eventosParticipados()).isEqualTo(1);
        // xp = 2*10 (materiais) + 1*15 (dicas) + 1*150 (eventos) = 185
        assertThat(resposta.xp()).isEqualTo(185);
    }

    @Test
    void conquistasDesbloqueiamNosLimiaresCorretos() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID(), new BigDecimal("5.0"), 3, 3);
        mockarZerado(usuarioId, mentorado);
        var mentoriaRealizada = mentoriaRealizada();
        when(mentoriaRepository.buscarPorMentorado(mentorado)).thenReturn(List.of(mentoriaRealizada));

        JornadaResponse resposta = service().jornada(usuarioId);

        assertThat(codigosDesbloqueados(resposta)).containsExactlyInAnyOrder(
                "MENTORIA_REALIZADA", "EM_CRESCIMENTO", "FERRAMENTAS_EM_DIA");
    }

    @Test
    void progressoPct100NoTopoDaEscalaSemProximoNivel() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID(), BigDecimal.ZERO, 0, 0);
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(conteudoMentoradoService.buscarCatalogo(eq(usuarioId), isNull(), isNull())).thenReturn(List.of());
        // 60 mentorias realizadas * 200 = 12000 xp, bem acima do teto DIAMANTE (8000)
        var mentoriaRealizada = mentoriaRealizada();
        when(mentoriaRepository.buscarPorMentorado(mentorado))
                .thenReturn(java.util.Collections.nCopies(60, mentoriaRealizada));
        when(inscricaoEventoRepository.findByMentoradoId(mentorado.getId())).thenReturn(List.of());
        when(metaRepository.buscarPorMentorado(eq(mentorado.getId()), isNull())).thenReturn(List.of());
        when(encaminhamentoRepository.buscarPorMentorado(eq(mentorado.getId()), isNull(), isNull())).thenReturn(List.of());

        JornadaResponse resposta = service().jornada(usuarioId);

        assertThat(resposta.nivelAtual()).isEqualTo(NivelJornada.DIAMANTE);
        assertThat(resposta.xpProximoNivel()).isNull();
        assertThat(resposta.progressoPct()).isEqualTo(100);
    }

    private static List<String> codigosDesbloqueados(JornadaResponse resposta) {
        return resposta.conquistas().stream().filter(JornadaResponse.Conquista::desbloqueada)
                .map(JornadaResponse.Conquista::codigo).toList();
    }

    private static ConteudoMentoradoResponse conteudo(TipoConteudo tipo, boolean favorito, boolean assistido) {
        return new ConteudoMentoradoResponse(UUID.randomUUID(), "Título", tipo, "https://x.com/a",
                Plano.GRATUITO, true, java.time.Instant.now(), favorito, assistido);
    }

    private static InscricaoEvento inscricao(StatusInscricao status) {
        InscricaoEvento i = org.mockito.Mockito.mock(InscricaoEvento.class);
        when(i.getStatus()).thenReturn(status);
        return i;
    }

    private static com.sawhub.hub.mentoria.Mentoria mentoriaRealizada() {
        com.sawhub.hub.mentoria.Mentoria m = org.mockito.Mockito.mock(com.sawhub.hub.mentoria.Mentoria.class);
        when(m.getStatus()).thenReturn(com.sawhub.hub.mentoria.StatusMentoria.REALIZADA);
        return m;
    }
}
