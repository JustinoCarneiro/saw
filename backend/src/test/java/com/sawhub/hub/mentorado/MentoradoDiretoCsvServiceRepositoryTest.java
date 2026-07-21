package com.sawhub.hub.mentorado;

import static org.assertj.core.api.Assertions.assertThat;

import com.sawhub.hub.comercial.Lead;
import com.sawhub.hub.comercial.LeadRepository;
import com.sawhub.hub.mentorado.dto.ImportMentoradoDiretoResultResponse;
import com.sawhub.hub.security.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/** M23 item 4 (bulk-CREATE, 19/07/2026) — a garantia central de {@link MentoradoDiretoCsvService}
 * (duas passadas: valida tudo antes de criar qualquer entidade) já é coberta por Mockito em
 * {@code MentoradoDiretoCsvServiceTest}. Este teste fecha a lacuna que o Mockito não cobre: mesmo
 * com uma sessão REAL do Hibernate — 4 tipos de entidade por linha (Usuario/Lead/Mentorado/
 * Diagnóstico) — nada fica meio-criado no Postgres quando qualquer linha do arquivo falha, e tudo
 * fica ligado corretamente quando todas passam. @DataJpaTest de propósito, mesmo raciocínio do
 * {@code MentoradoCsvServiceRepositoryTest} (M22): mock nunca reproduz timing real de flush. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class MentoradoDiretoCsvServiceRepositoryTest {

    @Autowired
    private MentoradoRepository mentoradoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private LeadRepository leadRepository;
    @Autowired
    private MentoradoDiagnosticoInicialRepository diagnosticoInicialRepository;
    @Autowired
    private EntityManager entityManager;

    @TempDir
    private Path tempDir;

    // NoOpPasswordEncoder.getInstance() está deprecated — este teste não exercita hash de senha
    // (não é @SpringBootTest, então não tem o bean real de PasswordEncoder do SecurityConfig
    // disponível), só precisa de ALGUM PasswordEncoder pra construir MentoradoAdminService.
    private static final PasswordEncoder PASSWORD_ENCODER_SEM_HASH = new PasswordEncoder() {
        @Override
        public String encode(CharSequence rawPassword) {
            return rawPassword.toString();
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return rawPassword.toString().equals(encodedPassword);
        }
    };

    private MentoradoDiretoCsvService service() {
        var contratoStorage = new ContratoDocumentoStorageService(tempDir.toString());
        var mentoradoAdminService = new MentoradoAdminService(mentoradoRepository, usuarioRepository, leadRepository,
                diagnosticoInicialRepository, contratoStorage, PASSWORD_ENCODER_SEM_HASH);
        return new MentoradoDiretoCsvService(mentoradoAdminService);
    }

    // Ordem alinhada ao contrato do Blueprint M24 (ROADMAP.md).
    private static final String[] COLUNAS = {
            "email", "nome", "negocio", "nomeFantasia", "cnpj", "socios", "telefone", "tipoContrato",
            "valorContrato", "dataFechamentoContrato", "faturamentoAnual", "quantidadeColaboradores",
            "empresaRegularizada", "quantidadeLojas", "cmvDefinido", "cmvDetalhe", "tempoMedioAtendimento",
            "culturaConstruida", "processosDesenhados"
    };

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

    private static MockMultipartFile csv(String conteudo) {
        return new MockMultipartFile("arquivo", "mentorados-direto.csv", "text/csv", conteudo.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void importarComUmaLinhaInvalidaNaoCriaNadaNoPostgresMesmoComSessaoRealDoHibernate() {
        String sufixo = UUID.randomUUID().toString();
        String emailValido = "valida-" + sufixo + "@sawhub-teste.com.br";

        Map<String, String> linhaValida = new LinkedHashMap<>();
        linhaValida.put("email", emailValido);
        linhaValida.put("nome", "Nome Válido");
        linhaValida.put("tipoContrato", "MENTORIA_CONTINUA");

        Map<String, String> linhaInvalida = new LinkedHashMap<>();
        linhaInvalida.put("email", "sem-tipo-" + sufixo + "@sawhub-teste.com.br");
        linhaInvalida.put("nome", "Nome Sem Tipo");

        String conteudo = cabecalho() + "\n" + linha(linhaValida) + "\n" + linha(linhaInvalida) + "\n";

        ImportMentoradoDiretoResultResponse resultado = service().importar(csv(conteudo));

        assertThat(resultado.importados()).isZero();
        assertThat(resultado.erros()).hasSize(1);

        entityManager.flush();
        entityManager.clear();

        assertThat(usuarioRepository.findByEmail(emailValido)).isEmpty();
    }

    @Test
    void importarComTudoValidoPersisteLeadUsuarioMentoradoEDiagnosticoDeVerdadeNoPostgres() {
        String sufixo = UUID.randomUUID().toString();
        String email = "completo-" + sufixo + "@sawhub-teste.com.br";

        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("email", email);
        campos.put("nome", "Maria Souza");
        campos.put("negocio", "Menu Caseirinho");
        campos.put("nomeFantasia", "Menu Caseirinho Ltda");
        campos.put("cnpj", "42.521.899/0001-38");
        campos.put("socios", "Girlandia e Jaene");
        campos.put("telefone", "11999998888");
        campos.put("tipoContrato", "MENTORIA_CONTINUA");
        campos.put("valorContrato", "26000.00");
        campos.put("dataFechamentoContrato", "17/07/2026");
        campos.put("faturamentoAnual", "600000");
        campos.put("quantidadeColaboradores", "6");
        campos.put("empresaRegularizada", "true");
        campos.put("quantidadeLojas", "1");
        campos.put("cmvDefinido", "SIM");
        campos.put("cmvDetalhe", "margem apertada");
        campos.put("tempoMedioAtendimento", "5 a 10 minutos");
        campos.put("culturaConstruida", "EM_CONSTRUCAO");
        campos.put("processosDesenhados", "SIM");

        String conteudo = cabecalho() + "\n" + linha(campos) + "\n";

        ImportMentoradoDiretoResultResponse resultado = service().importar(csv(conteudo));

        assertThat(resultado.importados()).isEqualTo(1);
        assertThat(resultado.criados()).hasSize(1);
        assertThat(resultado.criados().get(0).senhaTemporaria()).isNotBlank();

        entityManager.flush();
        entityManager.clear();

        var usuario = usuarioRepository.findByEmail(email).orElseThrow();
        Mentorado mentorado = mentoradoRepository.findByUsuario(usuario).orElseThrow();
        assertThat(mentorado.getNome()).isEqualTo("Maria Souza");
        assertThat(mentorado.getNomeFantasia()).isEqualTo("Menu Caseirinho Ltda");
        assertThat(mentorado.getCnpj()).isEqualTo("42.521.899/0001-38");
        assertThat(mentorado.getTipoContrato()).isEqualTo(TipoContrato.MENTORIA_CONTINUA);

        List<Lead> leadsDoMentorado = leadRepository.findAll().stream()
                .filter(l -> mentorado.equals(l.getMentorado())).toList();
        assertThat(leadsDoMentorado).hasSize(1);

        MentoradoDiagnosticoInicial diagnostico = diagnosticoInicialRepository.findByMentoradoId(mentorado.getId())
                .orElseThrow();
        assertThat(diagnostico.getFaturamentoAnual()).isEqualByComparingTo("600000");
        assertThat(diagnostico.getCulturaConstruida()).isEqualTo(EstadoImplementacao.EM_CONSTRUCAO);
    }

    // M28 (change request, 21/07/2026, "import único") — mesma garantia de sessão real do
    // Hibernate que MentoradoCsvServiceRepositoryTest cobria pro bulk-UPDATE antigo (removido),
    // agora dentro deste service: uma linha com e-mail já cadastrado ATUALIZA o Mentorado existente
    // em vez de tentar criar um novo (o que quebraria a constraint de e-mail único).
    @Test
    void importarComEmailJaCadastradoAtualizaOMentoradoExistenteDeVerdadeNoPostgres() {
        String sufixo = UUID.randomUUID().toString();
        String email = "existente-" + sufixo + "@sawhub-teste.com.br";

        Map<String, String> campoOriginal = new LinkedHashMap<>();
        campoOriginal.put("email", email);
        campoOriginal.put("nome", "Nome Original");
        campoOriginal.put("tipoContrato", "MENTORIA_INDIVIDUAL");
        ImportMentoradoDiretoResultResponse criacao =
                service().importar(csv(cabecalho() + "\n" + linha(campoOriginal) + "\n"));
        assertThat(criacao.importados()).isEqualTo(1);
        entityManager.flush();
        entityManager.clear();

        Map<String, String> campoAtualizado = new LinkedHashMap<>();
        campoAtualizado.put("email", email);
        campoAtualizado.put("nome", "Nome Atualizado Via Import");
        campoAtualizado.put("negocio", "Negócio Novo");
        campoAtualizado.put("tipoContrato", "MENTORIA_CONTINUA");
        ImportMentoradoDiretoResultResponse atualizacao =
                service().importar(csv(cabecalho() + "\n" + linha(campoAtualizado) + "\n"));

        assertThat(atualizacao.erros()).isEmpty();
        assertThat(atualizacao.importados()).isZero();
        assertThat(atualizacao.atualizados()).isEqualTo(1);
        assertThat(atualizacao.criados()).isEmpty();

        entityManager.flush();
        entityManager.clear();

        var usuario = usuarioRepository.findByEmail(email).orElseThrow();
        List<Mentorado> todos = mentoradoRepository.findAll().stream()
                .filter(m -> usuario.equals(m.getUsuario())).toList();
        assertThat(todos).hasSize(1);
        Mentorado mentorado = todos.get(0);
        assertThat(mentorado.getNome()).isEqualTo("Nome Atualizado Via Import");
        assertThat(mentorado.getNegocio()).isEqualTo("Negócio Novo");
        assertThat(mentorado.getTipoContrato()).isEqualTo(TipoContrato.MENTORIA_CONTINUA);
    }
}
