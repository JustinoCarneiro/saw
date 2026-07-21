package com.sawhub.hub.mentoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.security.Perfil;
import com.sawhub.hub.security.Usuario;
import com.sawhub.hub.security.UsuarioRepository;
import com.sawhub.hub.team.Area;
import com.sawhub.hub.team.Colaborador;
import com.sawhub.hub.team.ColaboradorRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/** E17/M27 (change request pós-MVP, 19/07/2026) — presença por mentorado em mentoria coletiva
 * (ver ROADMAP.md § "Blueprint (M27)"). @DataJpaTest de propósito (sessão real do Hibernate,
 * mesmo raciocínio de MentoriaRepositoryTest): a query agregada de frequência e o índice único
 * só se provam contra Postgres de verdade. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class PresencaMentoriaRepositoryTest {

    @Autowired
    private PresencaMentoriaRepository presencaMentoriaRepository;
    @Autowired
    private MentoriaRepository mentoriaRepository;
    @Autowired
    private ColaboradorRepository colaboradorRepository;
    @Autowired
    private MentoradoRepository mentoradoRepository;
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
        return mentoradoRepository.save(new Mentorado(usuario, "Mentorado " + sufixo, null, BigDecimal.ZERO, 0, 0));
    }

    private Mentoria criarMentoriaRealizada(TipoMentoria tipo, Colaborador mentor, Set<Mentorado> mentorados) {
        Mentoria mentoria = new Mentoria(tipo, mentor, mentorados, Instant.parse("2026-07-15T14:00:00Z"),
                60, "https://meet.google.com/x", null);
        mentoria.confirmar();
        mentoria.realizar();
        return mentoriaRepository.save(mentoria);
    }

    @Test
    void naoPermiteDuasPresencasParaOMesmoParMentoriaMentorado() {
        Colaborador mentor = criarMentor("1");
        Mentorado mentorado = criarMentorado("1");
        Mentoria mentoria = criarMentoriaRealizada(TipoMentoria.GRUPO, mentor, Set.of(mentorado));
        presencaMentoriaRepository.saveAndFlush(new PresencaMentoria(mentoria, mentorado, true));

        // saveAndFlush (não save()+entityManager.flush() cru) de propósito: só assim a exceção
        // passa pelo proxy do Spring Data e vem traduzida pra DataIntegrityViolationException —
        // flush direto na EntityManager propaga a ConstraintViolationException crua do Hibernate.
        assertThatThrownBy(() -> presencaMentoriaRepository.saveAndFlush(new PresencaMentoria(mentoria, mentorado, false)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void buscarResumoPorMentoriaInicializaSemLazyInitializationException() {
        Colaborador mentor = criarMentor("2");
        Mentorado mentorado = criarMentorado("2");
        Mentoria mentoria = criarMentoriaRealizada(TipoMentoria.GRUPO, mentor, Set.of(mentorado));
        presencaMentoriaRepository.save(new PresencaMentoria(mentoria, mentorado, true));
        entityManager.flush();
        entityManager.clear();

        List<com.sawhub.hub.mentoria.dto.PresencaResumoRow> resumo =
                presencaMentoriaRepository.buscarResumoPorMentoria(mentoria.getId());
        entityManager.clear();

        assertThat(resumo).hasSize(1);
        assertThat(resumo.get(0).mentoradoId()).isEqualTo(mentorado.getId());
        assertThat(resumo.get(0).presente()).isTrue();
    }

    // Contra banco real de propósito: prova que a janela GRUPO/REALIZADA + LEFT JOIN realmente
    // conta "participou mas sem presença marcada" no total, só não no numerador — não só que a
    // query compila.
    @Test
    void buscarFrequenciaContaSoMentoriaGrupoRealizadaESoPresencaConfirmadaNoNumerador() {
        Colaborador mentor = criarMentor("3");
        Mentorado mentorado = criarMentorado("3");

        Mentoria grupoComPresenca = criarMentoriaRealizada(TipoMentoria.GRUPO, mentor, Set.of(mentorado));
        presencaMentoriaRepository.save(new PresencaMentoria(grupoComPresenca, mentorado, true));

        // Participou, mas ninguém marcou presença ainda — conta no total, não no numerador.
        criarMentoriaRealizada(TipoMentoria.GRUPO, mentor, Set.of(mentorado));

        // Mentoria em grupo AGENDADA (não REALIZADA) — não deve contar em nada.
        Mentoria grupoAgendada = new Mentoria(TipoMentoria.GRUPO, mentor, Set.of(mentorado),
                Instant.parse("2026-08-01T14:00:00Z"), 60, null, null);
        mentoriaRepository.save(grupoAgendada);

        // Mentoria individual realizada — não entra na conta de "mentoria em grupo".
        criarMentoriaRealizada(TipoMentoria.INDIVIDUAL, mentor, Set.of(mentorado));

        entityManager.flush();
        entityManager.clear();

        var frequencia = presencaMentoriaRepository.buscarFrequencia();
        entityManager.clear();

        var linha = frequencia.stream().filter(f -> f.mentoradoId().equals(mentorado.getId())).findFirst().orElseThrow();
        assertThat(linha.totalMentoriasGrupo()).isEqualTo(2L);
        assertThat(linha.presencasConfirmadas()).isEqualTo(1L);
    }
}
