package com.sawhub.hub.atividade;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/** M25 — achado alto do revisor-seguranca: `descricao` costuma conter nome de lead/mentorado
 * (ex. "Lead fechado: Maria Souza"), PII que precisa do mesmo tratamento pgcrypto já usado em
 * Lead/Mentorado (V19/V28). @DataJpaTest de propósito (sessão real do Hibernate) — mesmo
 * raciocínio de LeadRepositoryTest/ContaPagarReceberRepositoryTest: só um teste contra banco real
 * prova que o @ColumnTransformer lê/escreve a coluna bytea corretamente. Não existia teste de
 * repository pra esta entidade antes desta leva (só AtividadeLogServiceTest, Mockito puro, que
 * nunca exercitou o SQL real). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class AtividadeLogRepositoryTest {

    @Autowired
    private AtividadeLogRepository atividadeLogRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void descricaoSobrevivePgcryptoRoundTripForaDaTransacaoOriginal() {
        AtividadeLog salvo = atividadeLogRepository.save(new AtividadeLog("LEAD_FECHADO", "Lead fechado: Maria Souza"));
        entityManager.flush();
        entityManager.clear();

        AtividadeLog recarregado = atividadeLogRepository.findById(salvo.getId()).orElseThrow();
        entityManager.clear();

        assertThat(recarregado.getDescricao()).isEqualTo("Lead fechado: Maria Souza");
        assertThat(recarregado.getTipo()).isEqualTo("LEAD_FECHADO");
    }

    @Test
    void findAllByOrderByCriadoEmDescInicializaDescricaoForaDaTransacaoOriginal() {
        AtividadeLog salvo = atividadeLogRepository.save(new AtividadeLog("EVENTO_CRIADO", "Evento criado: Encontro Nacional"));
        entityManager.flush();
        entityManager.clear();

        AtividadeLog recarregado = atividadeLogRepository.findAllByOrderByCriadoEmDesc().stream()
                .filter(a -> a.getId().equals(salvo.getId())).findFirst().orElseThrow();
        entityManager.clear();

        assertThat(recarregado.getDescricao()).isEqualTo("Evento criado: Encontro Nacional");
    }
}
