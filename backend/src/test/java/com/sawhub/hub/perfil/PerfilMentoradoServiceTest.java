package com.sawhub.hub.perfil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.mentorado.Plano;
import com.sawhub.hub.perfil.dto.AtualizarPerfilMentoradoRequest;
import com.sawhub.hub.perfil.dto.PerfilMentoradoResponse;
import com.sawhub.hub.security.Usuario;
import com.sawhub.hub.security.Perfil;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** H9.1/H9.3 — RED primeiro: PerfilMentoradoService ainda não existia neste ponto do ciclo. */
@ExtendWith(MockitoExtension.class)
class PerfilMentoradoServiceTest {

    @Mock
    private MentoradoRepository mentoradoRepository;

    private PerfilMentoradoService service() {
        return new PerfilMentoradoService(mentoradoRepository);
    }

    private static Mentorado mentorado(UUID id, Plano plano) {
        Usuario usuario = new Usuario("ana@anacosta.com.br", "hash", Perfil.MENTORADO);
        Mentorado m = new Mentorado(usuario, "Ana Costa", "Cantina Ana Costa", plano, BigDecimal.TEN, 1, 3);
        ReflectionTestUtils.setField(m, "id", id);
        return m;
    }

    @Test
    void buscarIsolaPorTenantResolvendoSoPeloUsuarioAutenticado() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID(), Plano.ESSENCIAL);
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));

        PerfilMentoradoResponse resposta = service().buscar(usuarioId);

        assertThat(resposta.nome()).isEqualTo("Ana Costa");
        assertThat(resposta.email()).isEqualTo("ana@anacosta.com.br");
    }

    @Test
    void buscarSemMentoradoVinculadoFalhaAoInvesDeVazarDadoDeOutroUsuario() {
        UUID usuarioId = UUID.randomUUID();
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().buscar(usuarioId)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void atualizarGravaContatoEPreferenciasSemTocarNomeOuNegocio() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID(), Plano.ESSENCIAL);
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));

        var request = new AtualizarPerfilMentoradoRequest(
                "(11) 90000-0000", "Nova bio", "https://cdn.sawhub.com.br/foto.jpg");
        PerfilMentoradoResponse resposta = service().atualizar(usuarioId, request);

        assertThat(resposta.telefone()).isEqualTo("(11) 90000-0000");
        assertThat(resposta.bio()).isEqualTo("Nova bio");
        assertThat(resposta.fotoUrl()).isEqualTo("https://cdn.sawhub.com.br/foto.jpg");
        // Campos admin-only intocados:
        assertThat(resposta.nome()).isEqualTo("Ana Costa");
    }
}
