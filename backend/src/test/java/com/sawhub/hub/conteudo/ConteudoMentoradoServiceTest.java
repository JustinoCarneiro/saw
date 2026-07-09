package com.sawhub.hub.conteudo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sawhub.hub.conteudo.dto.AtualizarConteudoMentoradoRequest;
import com.sawhub.hub.conteudo.dto.ConteudoMentoradoResponse;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.mentorado.Plano;
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
    void buscarCatalogoVerificaPlanosEInjetaJoin() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = new Mentorado(null, "Mentorado 1", "123", Plano.BASICO, null, null, null);
        ReflectionTestUtils.setField(mentorado, "id", UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));

        Conteudo conteudo = new Conteudo("Video", TipoConteudo.VIDEO, "url", Plano.GRATUITO);
        ReflectionTestUtils.setField(conteudo, "id", UUID.randomUUID());
        Object[] row = new Object[]{conteudo, null};
        
        when(conteudoMentoradoRepository.buscarCatalogo(eq(mentorado.getId()), eq(List.of(Plano.GRATUITO, Plano.BASICO)), eq(TipoConteudo.VIDEO), eq(true)))
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
    void atualizarStatusRejeitaConteudoForaDoPlano() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = new Mentorado(null, "M1", "t", Plano.GRATUITO, null, null, null);
        ReflectionTestUtils.setField(mentorado, "id", UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));

        Conteudo conteudo = new Conteudo("Doc", TipoConteudo.DOCUMENTO, "url", Plano.PROFISSIONAL);
        conteudo.publicar();
        ReflectionTestUtils.setField(conteudo, "id", UUID.randomUUID());
        when(conteudoRepository.findById(conteudo.getId())).thenReturn(Optional.of(conteudo));

        var req = new AtualizarConteudoMentoradoRequest(true, null);

        // 403, não 409 — é uma negação de autorização (plano insuficiente), não conflito de
        // estado; mesmo padrão de GlobalExceptionHandler.handleAccessDenied já usado no projeto.
        assertThatThrownBy(() -> service().atualizarStatus(usuarioId, conteudo.getId(), req))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("Seu plano não permite");
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
}
