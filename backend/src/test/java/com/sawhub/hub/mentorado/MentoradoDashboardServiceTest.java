package com.sawhub.hub.mentorado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.sawhub.hub.aviso.AvisoMentoradoService;
import com.sawhub.hub.aviso.dto.AvisoMentoradoResponse;
import com.sawhub.hub.aviso.CategoriaAviso;
import com.sawhub.hub.conteudo.Conteudo;
import com.sawhub.hub.conteudo.ConteudoRepository;
import com.sawhub.hub.conteudo.TipoConteudo;
import com.sawhub.hub.mentoria.Mentoria;
import com.sawhub.hub.mentoria.MentoriaRepository;
import com.sawhub.hub.mentoria.TipoMentoria;
import com.sawhub.hub.team.Area;
import com.sawhub.hub.team.Colaborador;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** H2.1–H2.3 — RED primeiro: MentoradoDashboardService ainda não existe neste ponto do ciclo. */
@ExtendWith(MockitoExtension.class)
class MentoradoDashboardServiceTest {

    @Mock
    private MentoradoRepository mentoradoRepository;
    @Mock
    private EncaminhamentoRepository encaminhamentoRepository;
    @Mock
    private MentoriaRepository mentoriaRepository;
    @Mock
    private ConteudoRepository conteudoRepository;
    @Mock
    private AvisoMentoradoService avisoMentoradoService;

    private MentoradoDashboardService service() {
        return new MentoradoDashboardService(mentoradoRepository, encaminhamentoRepository, mentoriaRepository,
                conteudoRepository, avisoMentoradoService);
    }

    // lenient(): nem todo teste inspeciona avisos, mas a chamada sempre acontece dentro de
    // dashboard() — mesmo padrão de semCompromissosNemDica() abaixo pras outras dependências.
    private void semAvisos(UUID usuarioId) {
        lenient().when(avisoMentoradoService.listar(eq(usuarioId), isNull(), isNull())).thenReturn(List.of());
    }

    private static Mentorado mentorado(UUID id) {
        Mentorado m = new Mentorado(null, "Maria", "Restaurante da Maria", BigDecimal.ZERO, 0, 0);
        ReflectionTestUtils.setField(m, "id", id);
        return m;
    }

    private static Colaborador mentor() {
        Colaborador c = new Colaborador(null, "Lucas", Area.GESTAO_PERFORMANCE);
        ReflectionTestUtils.setField(c, "id", UUID.randomUUID());
        return c;
    }

    private void semCompromissosNemDica(Mentorado mentorado) {
        when(mentoriaRepository.buscarPorMentorado(mentorado)).thenReturn(List.of());
        when(conteudoRepository.buscarComFiltro(TipoConteudo.VIDEO, true)).thenReturn(List.of());
    }

    @Test
    void usuarioSemMentoradoVinculadoLancaErroClaro() {
        UUID usuarioId = UUID.randomUUID();
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().dashboard(usuarioId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void tarefasAbertasEEvolucaoGeralVemDoPesoDosEncaminhamentos() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        semAvisos(usuarioId);

        Encaminhamento concluido = new Encaminhamento(mentorado, "Ficha técnica", 2, true);
        Encaminhamento aberto1 = new Encaminhamento(mentorado, "DRE", 1, false);
        Encaminhamento aberto2 = new Encaminhamento(mentorado, "Manual da cultura", 1, false);
        when(encaminhamentoRepository.findByMentoradoIdOrderByCriadoEmDesc(mentorado.getId()))
                .thenReturn(List.of(concluido, aberto1, aberto2));
        semCompromissosNemDica(mentorado);

        var dashboard = service().dashboard(usuarioId);

        assertThat(dashboard.tarefasAbertas()).isEqualTo(2);
        // peso concluído 2 / peso total 4 = 50% — mesma fórmula do E17 (ProgressoCalculator).
        assertThat(dashboard.evolucaoGeralPct()).isEqualTo(50);
    }

    @Test
    void metaSemanalPctSempreNullPorqueE3NaoExisteAinda() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(encaminhamentoRepository.findByMentoradoIdOrderByCriadoEmDesc(mentorado.getId())).thenReturn(List.of());
        semCompromissosNemDica(mentorado);
        semAvisos(usuarioId);

        assertThat(service().dashboard(usuarioId).metaSemanalPct()).isNull();
    }

    @Test
    void avisosVemDoAvisoMentoradoServiceLimitadosATres() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(encaminhamentoRepository.findByMentoradoIdOrderByCriadoEmDesc(mentorado.getId())).thenReturn(List.of());
        semCompromissosNemDica(mentorado);

        List<AvisoMentoradoResponse> quatroAvisos = List.of(
                new AvisoMentoradoResponse(UUID.randomUUID(), "Aviso 1", "desc", CategoriaAviso.GERAL, false, Instant.now()),
                new AvisoMentoradoResponse(UUID.randomUUID(), "Aviso 2", "desc", CategoriaAviso.GERAL, false, Instant.now()),
                new AvisoMentoradoResponse(UUID.randomUUID(), "Aviso 3", "desc", CategoriaAviso.GERAL, false, Instant.now()),
                new AvisoMentoradoResponse(UUID.randomUUID(), "Aviso 4", "desc", CategoriaAviso.GERAL, false, Instant.now()));
        when(avisoMentoradoService.listar(usuarioId, null, null)).thenReturn(quatroAvisos);

        assertThat(service().dashboard(usuarioId).avisos()).hasSize(3);
    }

    @Test
    void compromissosSaoSoMentoriasFuturasAgendadasOuConfirmadasOrdenadasPorData() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(encaminhamentoRepository.findByMentoradoIdOrderByCriadoEmDesc(mentorado.getId())).thenReturn(List.of());
        when(conteudoRepository.buscarComFiltro(TipoConteudo.VIDEO, true)).thenReturn(List.of());
        semAvisos(usuarioId);

        Colaborador mentor = mentor();
        Instant agora = Instant.now();

        Mentoria passada = new Mentoria(TipoMentoria.INDIVIDUAL, mentor, Set.of(mentorado), agora.minusSeconds(3600), 60, null, null);
        ReflectionTestUtils.setField(passada, "id", UUID.randomUUID());

        Mentoria futuraAgendadaMaisLonge = new Mentoria(TipoMentoria.INDIVIDUAL, mentor, Set.of(mentorado), agora.plusSeconds(7200), 60, "link2", null);
        ReflectionTestUtils.setField(futuraAgendadaMaisLonge, "id", UUID.randomUUID());

        Mentoria futuraConfirmadaMaisProxima = new Mentoria(TipoMentoria.GRUPO, mentor, Set.of(mentorado), agora.plusSeconds(3600), 60, "link1", null);
        futuraConfirmadaMaisProxima.confirmar();
        ReflectionTestUtils.setField(futuraConfirmadaMaisProxima, "id", UUID.randomUUID());

        Mentoria futuraCancelada = new Mentoria(TipoMentoria.INDIVIDUAL, mentor, Set.of(mentorado), agora.plusSeconds(1800), 60, null, null);
        futuraCancelada.cancelar();
        ReflectionTestUtils.setField(futuraCancelada, "id", UUID.randomUUID());

        when(mentoriaRepository.buscarPorMentorado(mentorado))
                .thenReturn(List.of(passada, futuraAgendadaMaisLonge, futuraConfirmadaMaisProxima, futuraCancelada));

        var dashboard = service().dashboard(usuarioId);

        assertThat(dashboard.compromissos()).hasSize(2);
        assertThat(dashboard.compromissos().get(0).id()).isEqualTo(futuraConfirmadaMaisProxima.getId());
        assertThat(dashboard.compromissos().get(1).id()).isEqualTo(futuraAgendadaMaisLonge.getId());
        assertThat(dashboard.proximaReuniao().id()).isEqualTo(futuraConfirmadaMaisProxima.getId());
    }

    @Test
    void semCompromissoFuturoProximaReuniaoENull() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(encaminhamentoRepository.findByMentoradoIdOrderByCriadoEmDesc(mentorado.getId())).thenReturn(List.of());
        semCompromissosNemDica(mentorado);
        semAvisos(usuarioId);

        var dashboard = service().dashboard(usuarioId);

        assertThat(dashboard.proximaReuniao()).isNull();
        assertThat(dashboard.compromissos()).isEmpty();
    }

    @Test
    void dicaDestaqueEOVideoPublicadoMaisRecente() {
        // M28 — gating por Plano removido ("não existem planos, mas sim produtos",
        // docs/reuniao-2026-07-17-atualizacoes.md). dicaDestaque agora é só "vídeo publicado mais
        // recente", sem filtro de tier — repositório já devolve ordenado DESC por criadoEm
        // (buscarComFiltro real), então o primeiro item da lista simulada abaixo é o esperado.
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(encaminhamentoRepository.findByMentoradoIdOrderByCriadoEmDesc(mentorado.getId())).thenReturn(List.of());
        when(mentoriaRepository.buscarPorMentorado(mentorado)).thenReturn(List.of());
        semAvisos(usuarioId);

        Conteudo maisRecente = new Conteudo("Vídeo avançado", TipoConteudo.VIDEO, "url1");
        Conteudo maisAntigo1 = new Conteudo("Vídeo essencial", TipoConteudo.VIDEO, "url2");
        Conteudo maisAntigo2 = new Conteudo("Vídeo básico", TipoConteudo.VIDEO, "url3");
        when(conteudoRepository.buscarComFiltro(TipoConteudo.VIDEO, true))
                .thenReturn(List.of(maisRecente, maisAntigo1, maisAntigo2));

        var dashboard = service().dashboard(usuarioId);

        assertThat(dashboard.dicaDestaque().titulo()).isEqualTo("Vídeo avançado");
    }

    @Test
    void semVideoPublicadoDicaDestaqueENull() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(encaminhamentoRepository.findByMentoradoIdOrderByCriadoEmDesc(mentorado.getId())).thenReturn(List.of());
        when(mentoriaRepository.buscarPorMentorado(mentorado)).thenReturn(List.of());
        semAvisos(usuarioId);

        when(conteudoRepository.buscarComFiltro(TipoConteudo.VIDEO, true)).thenReturn(List.of());

        assertThat(service().dashboard(usuarioId).dicaDestaque()).isNull();
    }
}
