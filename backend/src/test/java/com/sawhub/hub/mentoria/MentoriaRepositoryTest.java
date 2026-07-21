package com.sawhub.hub.mentoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.sawhub.hub.conteudo.Conteudo;
import com.sawhub.hub.conteudo.ConteudoRepository;
import com.sawhub.hub.conteudo.TipoConteudo;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.mentorado.Plano;
import com.sawhub.hub.security.Perfil;
import com.sawhub.hub.security.Usuario;
import com.sawhub.hub.security.UsuarioRepository;
import com.sawhub.hub.team.Area;
import com.sawhub.hub.team.Colaborador;
import com.sawhub.hub.team.ColaboradorRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/** Bug achado ao vivo durante a verificação do M12 pela suíte completa de E2E:
 * {@code MentoriaResponse.from()} passou a ler {@code m.getMateriaisRecomendados()}, mas
 * {@code buscarPorStatus}/{@code buscarPorIdComDetalhes} (usadas pela listagem/avanço de status do
 * Admin) nunca faziam FETCH JOIN nessa coleção — {@code LazyInitializationException} fora da
 * transação (open-in-view=false), quebrando silenciosamente {@code mentorados.spec.ts} (o "Confirmar"
 * de uma mentoria recém-criada nunca aparecia porque a listagem estourava 500). Mesma classe de bug
 * do {@code LeadRepositoryTest} (M05)/{@code EncaminhamentoRepositoryTest} (M10) — só reproduz com
 * uma sessão real do Hibernate, não com mocks. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class MentoriaRepositoryTest {

    @Autowired
    private MentoriaRepository mentoriaRepository;
    @Autowired
    private ColaboradorRepository colaboradorRepository;
    @Autowired
    private MentoradoRepository mentoradoRepository;
    @Autowired
    private ConteudoRepository conteudoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private EntityManager entityManager;

    private Colaborador criarMentor(String sufixo) {
        Usuario usuario = usuarioRepository.save(new Usuario("mentor" + sufixo + "@sawhub.com.br", "hash", Perfil.ADMIN));
        return colaboradorRepository.save(new Colaborador(usuario, "Mentor " + sufixo, Area.GESTAO_PERFORMANCE));
    }

    private Mentorado criarMentorado(String sufixo) {
        Usuario usuario = usuarioRepository.save(new Usuario("mentorado" + sufixo + "@sawhub.com.br", "hash", Perfil.MENTORADO));
        return mentoradoRepository.save(new Mentorado(usuario, "Mentorado " + sufixo, null, Plano.ESSENCIAL, BigDecimal.ZERO, 0, 0));
    }

    private Mentoria criarMentoriaComMaterial(Colaborador mentor, Mentorado mentorado) {
        Conteudo conteudo = conteudoRepository.save(
                new Conteudo("Ficha técnica", TipoConteudo.PLANILHA, "https://cdn.sawhub.com.br/x"));
        Mentoria mentoria = new Mentoria(TipoMentoria.INDIVIDUAL, mentor, Set.of(mentorado),
                Instant.parse("2026-07-15T14:00:00Z"), 60, "https://meet.google.com/x", null);
        mentoria.atualizarMateriaisRecomendados(Set.of(conteudo));
        return mentoriaRepository.save(mentoria);
    }

    @Test
    void buscarPorStatusInicializaMateriaisRecomendadosForaDaTransacaoOriginal() {
        Colaborador mentor = criarMentor("1");
        Mentorado mentorado = criarMentorado("1");
        criarMentoriaComMaterial(mentor, mentorado);
        entityManager.flush();
        entityManager.clear();

        var resultado = mentoriaRepository.buscarPorStatus(StatusMentoria.AGENDADA);
        entityManager.clear(); // simula o fim da transação de listar(), como no bug real

        assertThatCode(() -> resultado.get(0).getMateriaisRecomendados().size()).doesNotThrowAnyException();
        assertThat(resultado.get(0).getMateriaisRecomendados()).hasSize(1);
    }

    @Test
    void buscarPorIdComDetalhesInicializaMateriaisRecomendadosForaDaTransacaoOriginal() {
        Colaborador mentor = criarMentor("2");
        Mentorado mentorado = criarMentorado("2");
        Mentoria mentoria = criarMentoriaComMaterial(mentor, mentorado);
        entityManager.flush();
        entityManager.clear();

        var resultado = mentoriaRepository.buscarPorIdComDetalhes(mentoria.getId()).orElseThrow();
        entityManager.clear();

        assertThatCode(() -> resultado.getMateriaisRecomendados().size()).doesNotThrowAnyException();
        assertThat(resultado.getMateriaisRecomendados()).hasSize(1);
    }
}
