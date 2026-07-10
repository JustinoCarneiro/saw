package com.sawhub.hub.aviso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sawhub.hub.aviso.dto.CriarAvisoRequest;
import com.sawhub.hub.mentorado.Plano;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/** H12.2 — RED primeiro: AvisoAdminService ainda não existia neste ponto do ciclo. */
@ExtendWith(MockitoExtension.class)
class AvisoAdminServiceTest {

    @Mock
    private AvisoRepository avisoRepository;

    private AvisoAdminService service() {
        return new AvisoAdminService(avisoRepository);
    }

    @Test
    void criarJaPublicaImediatamenteSemEstadoDeRascunho() {
        when(avisoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new CriarAvisoRequest("Nova mentoria em grupo", "Inscrições abertas.",
                CategoriaAviso.MENTORIAS, Plano.BASICO);
        Aviso criado = service().criar(request);

        assertThat(criado.getTitulo()).isEqualTo("Nova mentoria em grupo");
        assertThat(criado.getCategoria()).isEqualTo(CategoriaAviso.MENTORIAS);
        assertThat(criado.getPlanoMinimo()).isEqualTo(Plano.BASICO);

        ArgumentCaptor<Aviso> captor = ArgumentCaptor.forClass(Aviso.class);
        verify(avisoRepository).save(captor.capture());
        assertThat(captor.getValue().getTitulo()).isEqualTo("Nova mentoria em grupo");
    }

    @Test
    void listarDelegaParaRepositorioOrdenadoPorMaisRecente() {
        Aviso a = new Aviso("Título", "Desc", CategoriaAviso.GERAL, Plano.GRATUITO);
        when(avisoRepository.findAllByOrderByCriadoEmDesc()).thenReturn(List.of(a));

        assertThat(service().listar()).containsExactly(a);
    }
}
