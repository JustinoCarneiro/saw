package com.sawhub.hub.meta;

import static org.assertj.core.api.Assertions.assertThat;

import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.mentorado.Plano;
import com.sawhub.hub.security.Perfil;
import com.sawhub.hub.security.Usuario;
import com.sawhub.hub.security.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/** Fase 5 — achado ao vivo (curl direto): GET /admin/metas/export usava findAll() puro (sem
 * fetch join) e devolvia Meta com `mentorado` LAZY não inicializado; MetaCsvService.exportar()
 * não roda numa transação, então ler meta.getMentorado().getNome() sempre lançava
 * LazyInitializationException (500 em produção, nunca coberto por E2E). @DataJpaTest (sessão real
 * do Hibernate) de propósito — mesmo padrão do precedente em EncaminhamentoRepositoryTest, um
 * teste baseado em mock nunca reproduziria isso. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class MetaRepositoryTest {

    @Autowired
    private MetaRepository metaRepository;
    @Autowired
    private MentoradoRepository mentoradoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private EntityManager entityManager;

    private Mentorado criarMentorado(String sufixo) {
        Usuario usuario = usuarioRepository.save(
                new Usuario("mentorado" + sufixo + "@sawhub.com.br", "hash", Perfil.MENTORADO));
        return mentoradoRepository.save(
                new Mentorado(usuario, "Mentorado " + sufixo, null, Plano.ESSENCIAL, BigDecimal.ZERO, 0, 0));
    }

    @Test
    void listarTodasComMentoradoContinuaLegivelForaDaTransacaoOriginal() {
        Mentorado mentorado = criarMentorado("Meta1");
        metaRepository.save(new Meta(mentorado, "Reduzir CMV", "Renegociar fornecedores",
                LocalDate.now().plusDays(60)));
        entityManager.flush();
        entityManager.clear();

        var lista = metaRepository.listarTodasComMentorado();
        entityManager.clear();

        assertThat(lista).isNotEmpty();
        Meta encontrada = lista.stream()
                .filter(m -> m.getMentorado().getId().equals(mentorado.getId()))
                .findFirst().orElseThrow();
        assertThat(encontrada.getMentorado().getNome()).isEqualTo("Mentorado Meta1");
        assertThat(encontrada.getMentorado().getUsuario().getEmail()).isEqualTo("mentoradoMeta1@sawhub.com.br");
    }
}
