package com.sawhub.hub.conteudo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sawhub.hub.conteudo.dto.AtualizarConteudoMentoradoRequest;
import com.sawhub.hub.conteudo.dto.ConteudoMentoradoResponse;
import com.sawhub.hub.conteudo.dto.IndicadoresConsumoResponse;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.mentorado.Plano;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ConteudoMentoradoServiceTest {

    @Mock
    private ConteudoMentoradoRepository conteudoMentoradoRepository;
    @Mock
    private ConteudoRepository conteudoRepository;
    @Mock
    private MentoradoRepository mentoradoRepository;

    private ConteudoMentoradoService service() {
        return new ConteudoMentoradoService(conteudoMentoradoRepository, conteudoRepository, mentoradoRepository);
    }

    @Test
    void buscarCatalogoRepassaFiltrosEInjetaJoin() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = new Mentorado(null, "Mentorado 1", "123", Plano.BASICO, null, null, null);
        ReflectionTestUtils.setField(mentorado, "id", UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));

        Conteudo conteudo = new Conteudo("Video", TipoConteudo.VIDEO, "url", Plano.GRATUITO);
        ReflectionTestUtils.setField(conteudo, "id", UUID.randomUUID());
        Object[] row = new Object[]{conteudo, null};

        when(conteudoMentoradoRepository.buscarCatalogo(eq(mentorado.getId()), eq(TipoConteudo.VIDEO), eq(true)))
                .thenReturn(List.<Object[]>of(row));

        List<ConteudoMentoradoResponse> resp = service().buscarCatalogo(usuarioId, TipoConteudo.VIDEO, true);

        assertThat(resp).hasSize(1);
        assertThat(resp.get(0).titulo()).isEqualTo("Video");
        assertThat(resp.get(0).favorito()).isFalse(); // cm is null
    }

    @Test
    void atualizarStatusCriaNovoVinculoSeNaoExistir() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = new Mentorado(null, "M1", "t", Plano.ESSENCIAL, null, null, null);
        ReflectionTestUtils.setField(mentorado, "id", UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));

        Conteudo conteudo = new Conteudo("Doc", TipoConteudo.DOCUMENTO, "url", Plano.BASICO);
        conteudo.publicar();
        ReflectionTestUtils.setField(conteudo, "id", UUID.randomUUID());
        when(conteudoRepository.findById(conteudo.getId())).thenReturn(Optional.of(conteudo));

        when(conteudoMentoradoRepository.findById(any())).thenReturn(Optional.empty());
        when(conteudoMentoradoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new AtualizarConteudoMentoradoRequest(true, true);
        ConteudoMentoradoResponse resp = service().atualizarStatus(usuarioId, conteudo.getId(), req);

        assertThat(resp.favorito()).isTrue();
        assertThat(resp.assistido()).isTrue();
        verify(conteudoMentoradoRepository).save(any());
    }

    @Test
    void atualizarStatusRejeitaConteudoInexistenteComoNaoEncontrado() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = new Mentorado(null, "M1", "t", Plano.PROFISSIONAL, null, null, null);
        ReflectionTestUtils.setField(mentorado, "id", UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        UUID conteudoInexistente = UUID.randomUUID();
        when(conteudoRepository.findById(conteudoInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().atualizarStatus(usuarioId, conteudoInexistente,
                new AtualizarConteudoMentoradoRequest(true, null)))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void atualizarStatusRejeitaConteudoNaoPublicadoComoNaoEncontrado() {
        // Achado nesta verificação: findById() sozinho não filtra publicado — sem o guard, um
        // mentorado que soubesse/adivinhasse o UUID de um conteúdo ainda em curadoria (rascunho
        // do Admin) conseguiria favoritar/marcar assistido, e a resposta vazaria título/url dele.
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = new Mentorado(null, "M1", "t", Plano.PROFISSIONAL, null, null, null);
        ReflectionTestUtils.setField(mentorado, "id", UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));

        Conteudo naoPublicado = new Conteudo("Rascunho em curadoria", TipoConteudo.DOCUMENTO, "url", Plano.GRATUITO);
        ReflectionTestUtils.setField(naoPublicado, "id", UUID.randomUUID());
        when(conteudoRepository.findById(naoPublicado.getId())).thenReturn(Optional.of(naoPublicado));

        assertThatThrownBy(() -> service().atualizarStatus(usuarioId, naoPublicado.getId(),
                new AtualizarConteudoMentoradoRequest(true, null)))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void indicadoresConsumoContaDiasDistintosNaoLinhas() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = new Mentorado(null, "M1", "t", Plano.PROFISSIONAL, null, null, null);
        ReflectionTestUtils.setField(mentorado, "id", UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));

        // 3 conteúdos assistidos, mas só 2 dias-calendário distintos em America/Sao_Paulo (-03:00)
        // — cm1 e cm2 caem no mesmo dia 10 em Brasil mesmo com timestamps UTC de dias diferentes
        // (02h UTC de 11/07 ainda é 23h de 10/07 em -03:00), então "dias assistidos" tem que dar
        // 2, não 3 — prova que a conversão de fuso é aplicada, não uma comparação ingênua em UTC.
        ConteudoMentorado cm1 = criarAssistido(mentorado, Instant.parse("2026-07-10T14:00:00Z"));
        ConteudoMentorado cm2 = criarAssistido(mentorado, Instant.parse("2026-07-11T02:00:00Z"));
        ConteudoMentorado cm3 = criarAssistido(mentorado, Instant.parse("2026-07-11T18:00:00Z"));
        when(conteudoMentoradoRepository.findByMentoradoIdAndAssistidoTrue(mentorado.getId()))
                .thenReturn(List.of(cm1, cm2, cm3));
        when(conteudoMentoradoRepository.countByMentoradoIdAndFavoritoTrue(mentorado.getId())).thenReturn(4L);

        IndicadoresConsumoResponse resp = service().indicadoresConsumo(usuarioId);

        assertThat(resp.diasAssistidos()).isEqualTo(2);
        assertThat(resp.favoritas()).isEqualTo(4L);
    }

    @Test
    void indicadoresConsumoSomaSoConteudosComDuracaoCadastrada() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = new Mentorado(null, "M1", "t", Plano.PROFISSIONAL, null, null, null);
        ReflectionTestUtils.setField(mentorado, "id", UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));

        // Um vídeo com duração cadastrada (12min) + um documento sem duração (opcional, não
        // preenchido) — soma tem que dar 12, não quebrar/contar o segundo como 0 explícito vs
        // ausente faz diferença nenhuma no resultado, mas prova que null não derruba o stream.
        ConteudoMentorado comDuracao = criarAssistido(mentorado, Instant.parse("2026-07-10T14:00:00Z"), 12);
        ConteudoMentorado semDuracao = criarAssistido(mentorado, Instant.parse("2026-07-10T15:00:00Z"), null);
        when(conteudoMentoradoRepository.findByMentoradoIdAndAssistidoTrue(mentorado.getId()))
                .thenReturn(List.of(comDuracao, semDuracao));
        when(conteudoMentoradoRepository.countByMentoradoIdAndFavoritoTrue(mentorado.getId())).thenReturn(0L);

        IndicadoresConsumoResponse resp = service().indicadoresConsumo(usuarioId);

        assertThat(resp.minutosAssistidos()).isEqualTo(12);
    }

    @Test
    void indicadoresConsumoSemNenhumConsumoDevolveZerado() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = new Mentorado(null, "M1", "t", Plano.GRATUITO, null, null, null);
        ReflectionTestUtils.setField(mentorado, "id", UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(conteudoMentoradoRepository.findByMentoradoIdAndAssistidoTrue(mentorado.getId())).thenReturn(List.of());
        when(conteudoMentoradoRepository.countByMentoradoIdAndFavoritoTrue(mentorado.getId())).thenReturn(0L);

        IndicadoresConsumoResponse resp = service().indicadoresConsumo(usuarioId);

        assertThat(resp.diasAssistidos()).isZero();
        assertThat(resp.favoritas()).isZero();
        assertThat(resp.minutosAssistidos()).isZero();
    }

    private ConteudoMentorado criarAssistido(Mentorado mentorado, Instant quando) {
        return criarAssistido(mentorado, quando, null);
    }

    private ConteudoMentorado criarAssistido(Mentorado mentorado, Instant quando, Integer duracaoMinutos) {
        Conteudo conteudo = new Conteudo("Doc", TipoConteudo.DOCUMENTO, "url", Plano.GRATUITO);
        ReflectionTestUtils.setField(conteudo, "id", UUID.randomUUID());
        conteudo.definirDuracaoMinutos(duracaoMinutos);
        ConteudoMentorado cm = new ConteudoMentorado(mentorado, conteudo);
        cm.setAssistido(true);
        ReflectionTestUtils.setField(cm, "dataConsumo", quando);
        return cm;
    }
}
