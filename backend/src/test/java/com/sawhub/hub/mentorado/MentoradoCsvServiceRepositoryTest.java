package com.sawhub.hub.mentorado;

import static org.assertj.core.api.Assertions.assertThat;

import com.sawhub.hub.common.dto.ImportResultResponse;
import com.sawhub.hub.security.Perfil;
import com.sawhub.hub.security.Usuario;
import com.sawhub.hub.security.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

/** M22 — a garantia central de {@link MentoradoCsvService} (duas passadas: valida tudo antes de
 * mutar qualquer entidade) já é coberta por {@code MentoradoCsvServiceTest} (Mockito, prova que o
 * método de mutação nunca é chamado). Este teste fecha a lacuna que o Mockito não cobre: mesmo com
 * uma sessão REAL do Hibernate — onde um {@code SELECT} no meio do loop poderia disparar um
 * auto-flush (`FlushMode.AUTO`) de uma mutação já feita — nada chega a ser persistido no Postgres
 * quando qualquer linha falha. @DataJpaTest de propósito (mesmo raciocínio do
 * `ContaPagarReceberRepositoryTest`, M21/M05): mock nunca reproduz timing real de flush. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class MentoradoCsvServiceRepositoryTest {

    @Autowired
    private MentoradoRepository mentoradoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private EntityManager entityManager;

    private MentoradoCsvService service() {
        return new MentoradoCsvService(mentoradoRepository, usuarioRepository);
    }

    private Mentorado criarMentorado(String sufixo) {
        Usuario usuario = usuarioRepository.save(
                new Usuario("mentorado-" + sufixo + "@sawhub-teste.com.br", "hash", Perfil.MENTORADO));
        return mentoradoRepository.save(new Mentorado(usuario, "Nome Original " + sufixo, "Negócio " + sufixo,
                Plano.GRATUITO, BigDecimal.ZERO, 0, 0));
    }

    @Test
    void importarComUmaLinhaInvalidaNaoPersisteNadaNoPostgresMesmoComSessaoRealDoHibernate() {
        String sufixo = UUID.randomUUID().toString();
        Mentorado valido = criarMentorado(sufixo);
        String emailValido = valido.getUsuario().getEmail();
        entityManager.flush();
        entityManager.clear();

        String conteudo = "email;nome;negocio;plano;vencimentoPlano;status\n"
                + emailValido + ";Nome Mudou;;ESSENCIAL;;ATIVO\n"
                + "naoexiste-" + sufixo + "@sawhub-teste.com.br;Qualquer;;GRATUITO;;ATIVO\n";
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "mentorados.csv", "text/csv",
                conteudo.getBytes(StandardCharsets.UTF_8));

        ImportResultResponse resultado = service().importar(arquivo);

        assertThat(resultado.importados()).isZero();
        assertThat(resultado.erros()).hasSize(1);

        entityManager.flush();
        entityManager.clear();

        Usuario usuarioRecarregado = usuarioRepository.findByEmail(emailValido).orElseThrow();
        Mentorado recarregado = mentoradoRepository.findByUsuario(usuarioRecarregado).orElseThrow();
        assertThat(recarregado.getNome()).isEqualTo("Nome Original " + sufixo);
        assertThat(recarregado.getPlano()).isEqualTo(Plano.GRATUITO);
    }

    @Test
    void importarComTudoValidoPersisteDeVerdadeNoPostgres() {
        String sufixo = UUID.randomUUID().toString();
        Mentorado valido = criarMentorado(sufixo);
        String emailValido = valido.getUsuario().getEmail();
        entityManager.flush();
        entityManager.clear();

        String conteudo = "email;nome;negocio;plano;vencimentoPlano;status\n"
                + emailValido + ";Nome Mudou;;ESSENCIAL;;ATIVO\n";
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "mentorados.csv", "text/csv",
                conteudo.getBytes(StandardCharsets.UTF_8));

        ImportResultResponse resultado = service().importar(arquivo);

        assertThat(resultado.importados()).isEqualTo(1);
        entityManager.flush();
        entityManager.clear();

        Usuario usuarioRecarregado = usuarioRepository.findByEmail(emailValido).orElseThrow();
        Mentorado recarregado = mentoradoRepository.findByUsuario(usuarioRecarregado).orElseThrow();
        assertThat(recarregado.getNome()).isEqualTo("Nome Mudou");
        assertThat(recarregado.getPlano()).isEqualTo(Plano.ESSENCIAL);
    }
}
