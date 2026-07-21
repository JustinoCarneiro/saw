package com.sawhub.hub.mentorado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sawhub.hub.mentorado.dto.ImportMentoradoDiretoResultResponse;
import com.sawhub.hub.mentorado.dto.ImportarMentoradoDiretoLinha;
import com.sawhub.hub.security.Perfil;
import com.sawhub.hub.security.Usuario;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

/** M23 item 4 (bulk-CREATE, 19/07/2026), estendido no M28 (change request, 21/07/2026, "import
 * único") — mesmo padrão de MentoradoCsvServiceTest/TeamCsvServiceTest: duas passadas (validação
 * isolada de criação/atualização), tudo-ou-nada. Sem stub, os mocks de MentoradoAdminService
 * devolvem null/false por padrão (Mockito) — equivalente a "e-mail não cadastrado ainda", por isso
 * a maioria dos testes de criação não precisa estubar buscarPorEmail/existeContaComEmail. */
@ExtendWith(MockitoExtension.class)
class MentoradoDiretoCsvServiceTest {

    // Ordem alinhada ao contrato do Blueprint M24 (ROADMAP.md).
    private static final String[] COLUNAS = {
            "email", "nome", "negocio", "nomeFantasia", "cnpj", "socios", "telefone", "tipoContrato",
            "valorContrato", "dataFechamentoContrato", "faturamentoAnual", "quantidadeColaboradores",
            "empresaRegularizada", "quantidadeLojas", "cmvDefinido", "cmvDetalhe", "tempoMedioAtendimento",
            "culturaConstruida", "processosDesenhados"
    };

    @Mock
    private MentoradoAdminService mentoradoAdminService;

    private MentoradoDiretoCsvService service() {
        return new MentoradoDiretoCsvService(mentoradoAdminService);
    }

    private static String cabecalho() {
        return String.join(";", COLUNAS);
    }

    private static String linha(Map<String, String> campos) {
        String[] valores = new String[COLUNAS.length];
        for (int i = 0; i < COLUNAS.length; i++) {
            valores[i] = campos.getOrDefault(COLUNAS[i], "");
        }
        return String.join(";", valores);
    }

    private static Map<String, String> linhaBasica(String email, String nome, String tipoContrato) {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("email", email);
        campos.put("nome", nome);
        campos.put("tipoContrato", tipoContrato);
        return campos;
    }

    private static MockMultipartFile csv(String conteudo) {
        return new MockMultipartFile("arquivo", "mentorados-direto.csv", "text/csv", conteudo.getBytes(StandardCharsets.UTF_8));
    }

    private static MentoradoAdminService.MentoradoCriado criadoFake(String email, String nome) {
        Usuario usuario = new Usuario(email, "hash", Perfil.MENTORADO);
        Mentorado mentorado = new Mentorado(usuario, nome, null, Plano.GRATUITO, BigDecimal.ZERO, 0, 0);
        return new MentoradoAdminService.MentoradoCriado(mentorado, "senha-temp-123");
    }

    @Test
    void importarValidaFormatoEDelegaCadaLinhaParaCriarDiretoDeImportacao() {
        when(mentoradoAdminService.criarDiretoDeImportacao(any()))
                .thenReturn(criadoFake("dono@restaurante.com", "Maria Souza"));

        Map<String, String> campos = linhaBasica("dono@restaurante.com", "Maria Souza", "MENTORIA_CONTINUA");
        campos.put("negocio", "Menu Caseirinho");
        campos.put("telefone", "11999998888");
        campos.put("valorContrato", "26000.00");
        campos.put("dataFechamentoContrato", "17/07/2026");
        campos.put("nomeFantasia", "Menu Caseirinho Ltda");
        campos.put("cnpj", "42.521.899/0001-38");
        // Sem ";" de propósito — o delimitador do arquivo de teste é ";", um valor de campo com
        // esse caractere quebraria a contagem de colunas do CSV cru montado à mão aqui.
        campos.put("socios", "Girlandia Aragão de Sousa e Jaene Oliveira de Araujo");
        String conteudo = cabecalho() + "\n" + linha(campos) + "\n";

        ImportMentoradoDiretoResultResponse resultado = service().importar(csv(conteudo));

        assertThat(resultado.importados()).isEqualTo(1);
        assertThat(resultado.atualizados()).isZero();
        assertThat(resultado.erros()).isEmpty();
        assertThat(resultado.criados()).hasSize(1);
        assertThat(resultado.criados().get(0).senhaTemporaria()).isEqualTo("senha-temp-123");

        ArgumentCaptor<ImportarMentoradoDiretoLinha> captor = ArgumentCaptor.forClass(ImportarMentoradoDiretoLinha.class);
        verify(mentoradoAdminService).criarDiretoDeImportacao(captor.capture());
        ImportarMentoradoDiretoLinha v = captor.getValue();
        assertThat(v.email()).isEqualTo("dono@restaurante.com");
        assertThat(v.nome()).isEqualTo("Maria Souza");
        assertThat(v.negocio()).isEqualTo("Menu Caseirinho");
        assertThat(v.tipoContrato()).isEqualTo(TipoContrato.MENTORIA_CONTINUA);
        assertThat(v.valorContrato()).isEqualByComparingTo("26000.00");
        assertThat(v.nomeFantasia()).isEqualTo("Menu Caseirinho Ltda");
        assertThat(v.cnpj()).isEqualTo("42.521.899/0001-38");
        assertThat(v.socios()).contains("Girlandia", "Jaene");
        assertThat(v.temDadosDeDiagnostico()).isFalse();
        assertThat(v.mentoradoExistenteId()).isNull();
    }

    @Test
    void importarCapturaCamposDeDiagnosticoQuandoPreenchidos() {
        when(mentoradoAdminService.criarDiretoDeImportacao(any()))
                .thenReturn(criadoFake("dono@restaurante.com", "Maria Souza"));

        Map<String, String> campos = linhaBasica("dono@restaurante.com", "Maria Souza", "CONSULTORIA");
        campos.put("faturamentoAnual", "600000");
        campos.put("quantidadeColaboradores", "6");
        campos.put("empresaRegularizada", "sim");
        campos.put("cmvDefinido", "SIM");
        campos.put("culturaConstruida", "EM_CONSTRUCAO");
        String conteudo = cabecalho() + "\n" + linha(campos) + "\n";

        service().importar(csv(conteudo));

        ArgumentCaptor<ImportarMentoradoDiretoLinha> captor = ArgumentCaptor.forClass(ImportarMentoradoDiretoLinha.class);
        verify(mentoradoAdminService).criarDiretoDeImportacao(captor.capture());
        ImportarMentoradoDiretoLinha v = captor.getValue();
        assertThat(v.temDadosDeDiagnostico()).isTrue();
        assertThat(v.faturamentoAnual()).isEqualByComparingTo("600000");
        assertThat(v.quantidadeColaboradores()).isEqualTo(6);
        assertThat(v.empresaRegularizada()).isTrue();
        assertThat(v.cmvDefinido()).isEqualTo(RespostaSimNao.SIM);
        assertThat(v.culturaConstruida()).isEqualTo(EstadoImplementacao.EM_CONSTRUCAO);
    }

    @Test
    void importarComLinhaSemDadosDeDiagnosticoNaoMarcaTemDadosDeDiagnostico() {
        when(mentoradoAdminService.criarDiretoDeImportacao(any()))
                .thenReturn(criadoFake("dono@restaurante.com", "Maria Souza"));

        String conteudo = cabecalho() + "\n" + linha(linhaBasica("dono@restaurante.com", "Maria Souza", "CONSULTORIA")) + "\n";

        service().importar(csv(conteudo));

        ArgumentCaptor<ImportarMentoradoDiretoLinha> captor = ArgumentCaptor.forClass(ImportarMentoradoDiretoLinha.class);
        verify(mentoradoAdminService).criarDiretoDeImportacao(captor.capture());
        assertThat(captor.getValue().temDadosDeDiagnostico()).isFalse();
    }

    // --- M28: e-mail já cadastrado como Mentorado vira atualização, não criação ---

    @Test
    void importarComEmailDeMentoradoExistenteDelegaParaAtualizarDeImportacaoEmVezDeCriar() {
        UUID mentoradoId = UUID.randomUUID();
        Mentorado existente = mock(Mentorado.class);
        when(existente.getId()).thenReturn(mentoradoId);
        when(mentoradoAdminService.buscarPorEmail("ja-e-mentorado@restaurante.com")).thenReturn(existente);

        String conteudo = cabecalho() + "\n"
                + linha(linhaBasica("ja-e-mentorado@restaurante.com", "Maria Souza Atualizada", "MENTORIA_CONTINUA")) + "\n";

        ImportMentoradoDiretoResultResponse resultado = service().importar(csv(conteudo));

        assertThat(resultado.erros()).isEmpty();
        assertThat(resultado.importados()).isZero();
        assertThat(resultado.atualizados()).isEqualTo(1);
        assertThat(resultado.criados()).isEmpty();

        ArgumentCaptor<ImportarMentoradoDiretoLinha> captor = ArgumentCaptor.forClass(ImportarMentoradoDiretoLinha.class);
        verify(mentoradoAdminService).atualizarDeImportacao(eq(mentoradoId), captor.capture());
        assertThat(captor.getValue().mentoradoExistenteId()).isEqualTo(mentoradoId);
        verify(mentoradoAdminService, never()).criarDiretoDeImportacao(any());
    }

    @Test
    void importarRejeitaEmailQueJaExisteMasNaoEhMentorado() {
        when(mentoradoAdminService.buscarPorEmail("colaborador@sawhub.com.br")).thenReturn(null);
        when(mentoradoAdminService.existeContaComEmail("colaborador@sawhub.com.br")).thenReturn(true);

        String conteudo = cabecalho() + "\n"
                + linha(linhaBasica("colaborador@sawhub.com.br", "Maria Souza", "MENTORIA_CONTINUA")) + "\n";

        ImportMentoradoDiretoResultResponse resultado = service().importar(csv(conteudo));

        assertThat(resultado.erros()).hasSize(1);
        assertThat(resultado.erros().get(0).motivo()).contains("já existe").contains("não é um mentorado");
        verify(mentoradoAdminService, never()).criarDiretoDeImportacao(any());
        verify(mentoradoAdminService, never()).atualizarDeImportacao(any(), any());
    }

    // --- tudo-ou-nada ---

    @Test
    void importarNaoCriaNenhumMentoradoQuandoUmaLinhaEhInvalida() {
        String conteudo = cabecalho() + "\n"
                + linha(linhaBasica("valido@restaurante.com", "Nome Válido", "MENTORIA_CONTINUA")) + "\n"
                + linha(linhaBasica("sem-tipo@restaurante.com", "Nome Sem Tipo", "")) + "\n";

        ImportMentoradoDiretoResultResponse resultado = service().importar(csv(conteudo));

        assertThat(resultado.importados()).isZero();
        assertThat(resultado.atualizados()).isZero();
        assertThat(resultado.criados()).isEmpty();
        assertThat(resultado.erros()).hasSize(1);
        assertThat(resultado.erros().get(0).linha()).isEqualTo(3);
        // A linha válida em isolamento passou pela resolução criar-ou-atualizar (validarLinha
        // chama buscarPorEmail/existeContaComEmail pra TODA linha), mas nenhuma entidade chega a
        // ser criada/atualizada — a garantia tudo-ou-nada é sobre criarDiretoDeImportacao/
        // atualizarDeImportacao, não sobre a resolução em si.
        verify(mentoradoAdminService, never()).criarDiretoDeImportacao(any());
        verify(mentoradoAdminService, never()).atualizarDeImportacao(any(), any());
    }

    @Test
    void importarRejeitaEmailDuplicadoNoArquivo() {
        String conteudo = cabecalho() + "\n"
                + linha(linhaBasica("dono@restaurante.com", "Maria Souza", "MENTORIA_CONTINUA")) + "\n"
                + linha(linhaBasica("dono@restaurante.com", "Outro Nome", "CONSULTORIA")) + "\n";

        ImportMentoradoDiretoResultResponse resultado = service().importar(csv(conteudo));

        assertThat(resultado.erros()).hasSize(1);
        assertThat(resultado.erros().get(0).motivo()).contains("duplicado");
        verify(mentoradoAdminService, never()).criarDiretoDeImportacao(any());
        verify(mentoradoAdminService, never()).atualizarDeImportacao(any(), any());
    }

    @Test
    void importarRejeitaCnpjInvalido() {
        Map<String, String> campos = linhaBasica("dono@restaurante.com", "Maria Souza", "MENTORIA_CONTINUA");
        campos.put("cnpj", "123");
        String conteudo = cabecalho() + "\n" + linha(campos) + "\n";

        ImportMentoradoDiretoResultResponse resultado = service().importar(csv(conteudo));

        assertThat(resultado.erros()).hasSize(1);
        assertThat(resultado.erros().get(0).motivo()).contains("CNPJ");
    }

    @Test
    void importarRejeitaTipoContratoInvalido() {
        String conteudo = cabecalho() + "\n"
                + linha(linhaBasica("dono@restaurante.com", "Maria Souza", "PLANO_QUE_NAO_EXISTE")) + "\n";

        ImportMentoradoDiretoResultResponse resultado = service().importar(csv(conteudo));

        assertThat(resultado.erros()).hasSize(1);
        assertThat(resultado.erros().get(0).motivo()).contains("Tipo de contrato");
    }

    @Test
    void importarRejeitaArquivoComColunaFaltando() {
        String conteudo = "email;nome;tipoContrato\n"
                + "dono@restaurante.com;Maria Souza;MENTORIA_CONTINUA\n";

        assertThatThrownBy(() -> service().importar(csv(conteudo)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negocio");
    }

    @Test
    void importarRejeitaArquivoSemLinhaDeDado() {
        assertThatThrownBy(() -> service().importar(csv(cabecalho() + "\n")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nenhuma linha");
    }
}
