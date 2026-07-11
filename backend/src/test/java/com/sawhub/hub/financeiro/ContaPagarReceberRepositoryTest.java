package com.sawhub.hub.financeiro;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/** Bug achado ao vivo durante a verificação do M21 via curl: {@code ContaCsvService.exportar()}
 * foi o primeiro consumidor de {@code ContaPagarReceberService.listar()} a ler
 * {@code conta.getCategoria().getNome()} — {@code ContaResponse} nunca expunha o nome da
 * categoria, então o proxy LAZY nunca precisava ser inicializado antes. 500 genérico
 * (LazyInitializationException, open-in-view=false) até os finders ganharem LEFT JOIN FETCH.
 * @DataJpaTest de propósito (sessão real do Hibernate, banco dev com dados de outras execuções —
 * por isso os testes buscam a própria conta criada por id, nunca assumem get(0)) — mesmo
 * raciocínio do LeadRepositoryTest (M05): um mock nunca reproduz um proxy LAZY não inicializado. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ContaPagarReceberRepositoryTest {

    @Autowired
    private ContaPagarReceberRepository contaRepository;
    @Autowired
    private CategoriaFinanceiraRepository categoriaRepository;
    @Autowired
    private EntityManager entityManager;

    private CategoriaFinanceira criarCategoria() {
        return categoriaRepository.save(new CategoriaFinanceira("Infra teste " + UUID.randomUUID(),
                TipoLancamento.DESPESA, GrupoDre.CUSTOS, null));
    }

    @Test
    void findAllInicializaCategoriaMesmoForaDaTransacaoOriginal() {
        CategoriaFinanceira categoria = criarCategoria();
        ContaPagarReceber salva = contaRepository.save(new ContaPagarReceber(TipoConta.A_PAGAR, "Servidor",
                new BigDecimal("180.00"), LocalDate.of(2026, 7, 20), categoria));
        entityManager.flush();
        entityManager.clear();

        ContaPagarReceber recarregada = contaRepository.findAllByOrderByDataVencimentoAsc().stream()
                .filter(c -> c.getId().equals(salva.getId())).findFirst().orElseThrow();
        entityManager.clear();

        assertThat(recarregada.getCategoria().getNome()).isEqualTo(categoria.getNome());
    }

    @Test
    void findByTipoInicializaCategoriaMesmoForaDaTransacaoOriginal() {
        CategoriaFinanceira categoria = criarCategoria();
        ContaPagarReceber salva = contaRepository.save(new ContaPagarReceber(TipoConta.A_PAGAR, "Servidor",
                new BigDecimal("180.00"), LocalDate.of(2026, 7, 20), categoria));
        entityManager.flush();
        entityManager.clear();

        ContaPagarReceber recarregada = contaRepository.findByTipoOrderByDataVencimentoAsc(TipoConta.A_PAGAR).stream()
                .filter(c -> c.getId().equals(salva.getId())).findFirst().orElseThrow();
        entityManager.clear();

        assertThat(recarregada.getCategoria().getNome()).isEqualTo(categoria.getNome());
    }

    @Test
    void findByStatusInicializaCategoriaMesmoForaDaTransacaoOriginal() {
        CategoriaFinanceira categoria = criarCategoria();
        ContaPagarReceber salva = contaRepository.save(new ContaPagarReceber(TipoConta.A_PAGAR, "Servidor",
                new BigDecimal("180.00"), LocalDate.of(2026, 7, 20), categoria));
        entityManager.flush();
        entityManager.clear();

        ContaPagarReceber recarregada = contaRepository.findByStatusOrderByDataVencimentoAsc(StatusConta.PENDENTE).stream()
                .filter(c -> c.getId().equals(salva.getId())).findFirst().orElseThrow();
        entityManager.clear();

        assertThat(recarregada.getCategoria().getNome()).isEqualTo(categoria.getNome());
    }
}
