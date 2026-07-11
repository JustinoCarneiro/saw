package com.sawhub.hub.mentorado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sawhub.hub.common.dto.ImportResultResponse;
import com.sawhub.hub.security.Perfil;
import com.sawhub.hub.security.Usuario;
import com.sawhub.hub.security.UsuarioRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

/** M22 — RED primeiro: MentoradoCsvService ainda não existe neste ponto do ciclo. */
@ExtendWith(MockitoExtension.class)
class MentoradoCsvServiceTest {

    @Mock
    private MentoradoRepository mentoradoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private MentoradoCsvService service() {
        return new MentoradoCsvService(mentoradoRepository, usuarioRepository);
    }

    private static Usuario usuario(String email) {
        return new Usuario(email, "hash", Perfil.MENTORADO);
    }

    private static Mentorado mentorado(Usuario usuario, String nome) {
        return new Mentorado(usuario, nome, "Negócio Antigo", Plano.GRATUITO, BigDecimal.ZERO, 0, 0);
    }

    private static MockMultipartFile csv(String conteudo) {
        return new MockMultipartFile("arquivo", "mentorados.csv", "text/csv", conteudo.getBytes(StandardCharsets.UTF_8));
    }

    // --- exportar ---

    @Test
    void exportarProduzCsvComPontoEVirgulaEDataPtBr() {
        Usuario usuario = usuario("joao@saborearte.com.br");
        Mentorado m = new Mentorado(usuario, "João Silva", "Sabor & Arte", Plano.PROFISSIONAL,
                BigDecimal.ZERO, 0, 0);
        m.definirVencimentoPlano(LocalDate.of(2026, 12, 1));
        when(mentoradoRepository.buscarComFiltro(null, null, null)).thenReturn(List.of(m));

        String csv = service().exportar(null, null, null);

        assertThat(csv).contains("email;nome;negocio;plano;vencimentoPlano;status");
        assertThat(csv).contains("joao@saborearte.com.br;João Silva;Sabor & Arte;PROFISSIONAL;01/12/2026;ATIVO");
    }

    @Test
    void exportarNeutralizaNomeQueComecaComSinalDeFormula() {
        Usuario usuario = usuario("x@x.com");
        Mentorado m = new Mentorado(usuario, "=SOMA(A1:A2)", null, Plano.GRATUITO, BigDecimal.ZERO, 0, 0);
        when(mentoradoRepository.buscarComFiltro(any(), any(), any())).thenReturn(List.of(m));

        String csv = service().exportar(null, null, null);

        assertThat(csv).contains("'=SOMA(A1:A2)");
    }

    // --- importar: caminho feliz (bulk-update) ---

    @Test
    void importarAtualizaMentoradoExistenteResolvidoPorEmail() {
        Usuario usuario = usuario("joao@saborearte.com.br");
        Mentorado m = mentorado(usuario, "Nome Antigo");
        when(usuarioRepository.findByEmail("joao@saborearte.com.br")).thenReturn(Optional.of(usuario));
        when(mentoradoRepository.findByUsuario(usuario)).thenReturn(Optional.of(m));

        String conteudo = "email;nome;negocio;plano;vencimentoPlano;status\n"
                + "joao@saborearte.com.br;Nome Novo;Negócio Novo;ESSENCIAL;01/12/2026;ATIVO\n";

        ImportResultResponse resultado = service().importar(csv(conteudo));

        assertThat(resultado.importados()).isEqualTo(1);
        assertThat(resultado.erros()).isEmpty();
        assertThat(m.getNome()).isEqualTo("Nome Novo");
        assertThat(m.getNegocio()).isEqualTo("Negócio Novo");
        assertThat(m.getPlano()).isEqualTo(Plano.ESSENCIAL);
        assertThat(m.getVencimentoPlano()).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(m.getStatus()).isEqualTo(StatusMentorado.ATIVO);
        verify(mentoradoRepository).saveAll(List.of(m));
    }

    @Test
    void importarComStatusInativoDesativaOMentorado() {
        Usuario usuario = usuario("joao@saborearte.com.br");
        Mentorado m = mentorado(usuario, "Nome");
        when(usuarioRepository.findByEmail("joao@saborearte.com.br")).thenReturn(Optional.of(usuario));
        when(mentoradoRepository.findByUsuario(usuario)).thenReturn(Optional.of(m));

        String conteudo = "email;nome;negocio;plano;vencimentoPlano;status\n"
                + "joao@saborearte.com.br;Nome;;GRATUITO;;INATIVO\n";

        service().importar(csv(conteudo));

        assertThat(m.getStatus()).isEqualTo(StatusMentorado.INATIVO);
        assertThat(m.getNegocio()).isNull();
        assertThat(m.getVencimentoPlano()).isNull();
    }

    // --- importar: tudo-ou-nada, com a garantia de duas passadas ---

    @Test
    void importarNaoMutaNenhumMentoradoQuandoUmaLinhaEhInvalida() {
        Usuario usuarioValido = usuario("joao@saborearte.com.br");
        Mentorado mValido = mentorado(usuarioValido, "Nome Antigo João");
        when(usuarioRepository.findByEmail("joao@saborearte.com.br")).thenReturn(Optional.of(usuarioValido));
        when(mentoradoRepository.findByUsuario(usuarioValido)).thenReturn(Optional.of(mValido));
        when(usuarioRepository.findByEmail("naoexiste@x.com")).thenReturn(Optional.empty());

        String conteudo = "email;nome;negocio;plano;vencimentoPlano;status\n"
                + "joao@saborearte.com.br;Nome Novo João;;ESSENCIAL;;ATIVO\n"
                + "naoexiste@x.com;Qualquer;;GRATUITO;;ATIVO\n";

        ImportResultResponse resultado = service().importar(csv(conteudo));

        assertThat(resultado.importados()).isZero();
        assertThat(resultado.erros()).hasSize(1);
        assertThat(resultado.erros().get(0).linha()).isEqualTo(3);
        assertThat(resultado.erros().get(0).motivo()).contains("não encontrado");

        // A garantia central deste service (ver Blueprint M22): a primeira linha, válida em
        // isolamento, NÃO pode ter sido mutada — prova que a validação é uma passada só de
        // leitura antes de qualquer efeito colateral.
        assertThat(mValido.getNome()).isEqualTo("Nome Antigo João");
        assertThat(mValido.getPlano()).isEqualTo(Plano.GRATUITO);
        verify(mentoradoRepository, never()).saveAll(any());
    }

    @Test
    void importarRejeitaEmailNaoEncontrado() {
        when(usuarioRepository.findByEmail("fantasma@x.com")).thenReturn(Optional.empty());

        String conteudo = "email;nome;negocio;plano;vencimentoPlano;status\n"
                + "fantasma@x.com;Nome;;GRATUITO;;ATIVO\n";

        ImportResultResponse resultado = service().importar(csv(conteudo));

        assertThat(resultado.erros()).hasSize(1);
        assertThat(resultado.erros().get(0).motivo()).contains("fantasma@x.com").contains("não encontrado");
    }

    @Test
    void importarRejeitaPlanoInvalido() {
        Usuario usuario = usuario("joao@x.com");
        when(usuarioRepository.findByEmail("joao@x.com")).thenReturn(Optional.of(usuario));
        when(mentoradoRepository.findByUsuario(usuario)).thenReturn(Optional.of(mentorado(usuario, "Nome")));

        String conteudo = "email;nome;negocio;plano;vencimentoPlano;status\n"
                + "joao@x.com;Nome;;PLANO_QUE_NAO_EXISTE;;ATIVO\n";

        ImportResultResponse resultado = service().importar(csv(conteudo));

        assertThat(resultado.erros()).hasSize(1);
        assertThat(resultado.erros().get(0).motivo()).contains("Plano");
    }

    @Test
    void importarRejeitaArquivoComColunaFaltando() {
        String conteudo = "email;nome;plano;status\n"
                + "joao@x.com;Nome;GRATUITO;ATIVO\n";

        assertThatThrownBy(() -> service().importar(csv(conteudo)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negocio");
    }
}
