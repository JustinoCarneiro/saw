package com.sawhub.hub.conteudo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sawhub.hub.conteudo.dto.AtualizarConteudoRequest;
import com.sawhub.hub.conteudo.dto.CriarConteudoRequest;
import com.sawhub.hub.mentorado.Plano;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** H11.3 — RED primeiro: ConteudoService ainda não existe neste ponto do ciclo. */
@ExtendWith(MockitoExtension.class)
class ConteudoServiceTest {

    @Mock
    private ConteudoRepository conteudoRepository;

    private ConteudoService service() {
        return new ConteudoService(conteudoRepository);
    }

    @Test
    void criarPersisteConteudoNaoPublicado() {
        when(conteudoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new CriarConteudoRequest("Ficha técnica modelo", TipoConteudo.PLANILHA,
                "https://cdn.sawhub.com.br/ficha.xlsx", Plano.BASICO);

        Conteudo conteudo = service().criar(request);

        assertThat(conteudo.getTitulo()).isEqualTo("Ficha técnica modelo");
        assertThat(conteudo.getPlanoMinimo()).isEqualTo(Plano.BASICO);
        assertThat(conteudo.isPublicado()).isFalse();
    }

    @Test
    void criarSemPlanoUsaGratuitoComoDefault() {
        when(conteudoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new CriarConteudoRequest("Manual da cultura", TipoConteudo.DOCUMENTO,
                "https://cdn.sawhub.com.br/manual.pdf", null);

        Conteudo conteudo = service().criar(request);

        assertThat(conteudo.getPlanoMinimo()).isEqualTo(Plano.GRATUITO);
    }

    @Test
    void listarDelegaFiltroParaOBanco() {
        when(conteudoRepository.buscarComFiltro(TipoConteudo.VIDEO, Plano.ESSENCIAL, true)).thenReturn(List.of());

        service().listar(TipoConteudo.VIDEO, Plano.ESSENCIAL, true);

        verify(conteudoRepository).buscarComFiltro(TipoConteudo.VIDEO, Plano.ESSENCIAL, true);
    }

    @Test
    void publicarMudaFlag() {
        UUID id = UUID.randomUUID();
        Conteudo conteudo = new Conteudo("DRE modelo", TipoConteudo.PLANILHA, "https://x", Plano.GRATUITO);
        when(conteudoRepository.findById(id)).thenReturn(Optional.of(conteudo));
        when(conteudoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Conteudo publicado = service().publicar(id);

        assertThat(publicado.isPublicado()).isTrue();
    }

    @Test
    void atualizarConteudoInexistenteLancaErro() {
        UUID id = UUID.randomUUID();
        when(conteudoRepository.findById(id)).thenReturn(Optional.empty());

        var request = new AtualizarConteudoRequest("X", TipoConteudo.OUTRO, "https://x", Plano.GRATUITO);

        assertThatThrownBy(() -> service().atualizar(id, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não encontrado");
    }
}
