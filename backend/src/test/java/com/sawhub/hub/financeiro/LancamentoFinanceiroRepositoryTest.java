package com.sawhub.hub.financeiro;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/** M25 — lacuna encontrada confirmando a V28 (achado alto do revisor-seguranca):
 * ContaPagarReceberService.liquidar() copia ContaPagarReceber.descricao (já pgcrypto) pra um
 * LancamentoFinanceiro novo — sem V29/@ColumnTransformer aqui, o nome de lead/mentorado
 * reaparece em claro nesta terceira coluna. @DataJpaTest de propósito (sessão real do Hibernate)
 * — mesmo raciocínio de ContaPagarReceberRepositoryTest/AtividadeLogRepositoryTest. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class LancamentoFinanceiroRepositoryTest {

    @Autowired
    private LancamentoFinanceiroRepository lancamentoRepository;
    @Autowired
    private CategoriaFinanceiraRepository categoriaRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void descricaoSobrevivePgcryptoRoundTripForaDaTransacaoOriginal() {
        CategoriaFinanceira categoria = categoriaRepository.save(new CategoriaFinanceira(
                "Mensalidades teste " + UUID.randomUUID(), TipoLancamento.RECEITA, GrupoDre.RECEITA_BRUTA, null));
        LancamentoFinanceiro salvo = lancamentoRepository.save(new LancamentoFinanceiro(TipoLancamento.RECEITA,
                categoria, "Parcela 1 - Maria Souza", new BigDecimal("2000.00"), LocalDate.of(2026, 8, 20),
                StatusLancamento.REALIZADO, null));
        entityManager.flush();
        entityManager.clear();

        LancamentoFinanceiro recarregado = lancamentoRepository.findById(salvo.getId()).orElseThrow();
        entityManager.clear();

        assertThat(recarregado.getDescricao()).isEqualTo("Parcela 1 - Maria Souza");
    }
}
