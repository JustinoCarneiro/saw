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

    // Sentinela pro filtro de período "desligado" — mesma faixa usada por
    // ContaPagarReceberService.listar (SEM_FILTRO_INICIO/FIM), nunca null: Postgres não consegue
    // inferir o tipo de um parâmetro LocalDate nulo nesta query (muita coluna bytea de pgcrypto
    // ao redor, ver comentário em ContaPagarReceberRepository.buscarComFiltro).
    private static final LocalDate SEM_FILTRO_INICIO = LocalDate.of(1900, 1, 1);
    private static final LocalDate SEM_FILTRO_FIM = LocalDate.of(2999, 12, 31);

    @Test
    void buscarComFiltroSemFiltroNenhumInicializaCategoriaMesmoForaDaTransacaoOriginal() {
        CategoriaFinanceira categoria = criarCategoria();
        ContaPagarReceber salva = contaRepository.save(new ContaPagarReceber(TipoConta.A_PAGAR, "Servidor",
                new BigDecimal("180.00"), LocalDate.of(2026, 7, 20), categoria));
        entityManager.flush();
        entityManager.clear();

        ContaPagarReceber recarregada = contaRepository.buscarComFiltro(null, null, SEM_FILTRO_INICIO, SEM_FILTRO_FIM).stream()
                .filter(c -> c.getId().equals(salva.getId())).findFirst().orElseThrow();
        entityManager.clear();

        assertThat(recarregada.getCategoria().getNome()).isEqualTo(categoria.getNome());
    }

    @Test
    void buscarComFiltroPorTipoInicializaCategoriaMesmoForaDaTransacaoOriginal() {
        CategoriaFinanceira categoria = criarCategoria();
        ContaPagarReceber salva = contaRepository.save(new ContaPagarReceber(TipoConta.A_PAGAR, "Servidor",
                new BigDecimal("180.00"), LocalDate.of(2026, 7, 20), categoria));
        entityManager.flush();
        entityManager.clear();

        ContaPagarReceber recarregada = contaRepository.buscarComFiltro(TipoConta.A_PAGAR, null, SEM_FILTRO_INICIO, SEM_FILTRO_FIM).stream()
                .filter(c -> c.getId().equals(salva.getId())).findFirst().orElseThrow();
        entityManager.clear();

        assertThat(recarregada.getCategoria().getNome()).isEqualTo(categoria.getNome());
    }

    @Test
    void buscarComFiltroPorStatusInicializaCategoriaMesmoForaDaTransacaoOriginal() {
        CategoriaFinanceira categoria = criarCategoria();
        ContaPagarReceber salva = contaRepository.save(new ContaPagarReceber(TipoConta.A_PAGAR, "Servidor",
                new BigDecimal("180.00"), LocalDate.of(2026, 7, 20), categoria));
        entityManager.flush();
        entityManager.clear();

        ContaPagarReceber recarregada = contaRepository.buscarComFiltro(null, StatusConta.PENDENTE, SEM_FILTRO_INICIO, SEM_FILTRO_FIM).stream()
                .filter(c -> c.getId().equals(salva.getId())).findFirst().orElseThrow();
        entityManager.clear();

        assertThat(recarregada.getCategoria().getNome()).isEqualTo(categoria.getNome());
    }

    // Change request 17/07/2026 ("filtro mensal") — contra banco real de propósito: prova que o
    // filtro [inicio, fim) sobre dataVencimento realmente inclui/exclui a linha certa, não só que
    // a query compila.
    @Test
    void buscarComFiltroPorPeriodoIncluiSoContasDentroDaJanela() {
        CategoriaFinanceira categoria = criarCategoria();
        ContaPagarReceber dentroDoMes = contaRepository.save(new ContaPagarReceber(TipoConta.A_PAGAR, "Dentro",
                new BigDecimal("100.00"), LocalDate.of(2026, 7, 15), categoria));
        ContaPagarReceber foraDoMes = contaRepository.save(new ContaPagarReceber(TipoConta.A_PAGAR, "Fora",
                new BigDecimal("100.00"), LocalDate.of(2026, 8, 1), categoria));
        entityManager.flush();
        entityManager.clear();

        var resultado = contaRepository.buscarComFiltro(null, null, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 1));
        entityManager.clear();

        assertThat(resultado).extracting(ContaPagarReceber::getId).contains(dentroDoMes.getId());
        assertThat(resultado).extracting(ContaPagarReceber::getId).doesNotContain(foraDoMes.getId());
    }

    // M25 — achado alto do revisor-seguranca: descricao costuma conter nome de lead/mentorado
    // (ex. "Parcela 1 - Maria Souza"), PII que precisa do mesmo tratamento pgcrypto da tabela de
    // origem (V19/V28). Teste contra banco real de propósito — só ele prova que o
    // @ColumnTransformer está lendo/escrevendo a coluna bytea corretamente (um mock nunca
    // exercitaria o SQL gerado pelo Hibernate).
    @Test
    void descricaoSobrevivePgcryptoRoundTripForaDaTransacaoOriginal() {
        CategoriaFinanceira categoria = criarCategoria();
        ContaPagarReceber salva = contaRepository.save(new ContaPagarReceber(TipoConta.A_RECEBER,
                "Parcela 1 - Maria Souza", new BigDecimal("2000.00"), LocalDate.of(2026, 8, 20), categoria));
        entityManager.flush();
        entityManager.clear();

        ContaPagarReceber recarregada = contaRepository.findById(salva.getId()).orElseThrow();
        entityManager.clear();

        assertThat(recarregada.getDescricao()).isEqualTo("Parcela 1 - Maria Souza");
    }

    // Gap 1 (raio-x, 18/07/2026) — contra banco real de propósito: só isso prova que a migração
    // V31 realmente trocou o CHECK constraint chk_conta_status pra aceitar PARCIAL (mesmo tipo de
    // bug já achado ao vivo com chk_lead_status na V26 — um mock nunca estoura violação de CHECK).
    @Test
    void liquidarParcialSobreviveCheckConstraintForaDaTransacaoOriginal() {
        CategoriaFinanceira categoria = criarCategoria();
        ContaPagarReceber salva = contaRepository.save(new ContaPagarReceber(TipoConta.A_RECEBER,
                "Mensalidade parcial", new BigDecimal("1000.00"), LocalDate.of(2026, 8, 20), categoria));
        salva.liquidarParcial(new BigDecimal("400.00"), LocalDate.of(2026, 7, 19));
        contaRepository.save(salva);
        entityManager.flush();
        entityManager.clear();

        ContaPagarReceber recarregada = contaRepository.findById(salva.getId()).orElseThrow();
        entityManager.clear();

        assertThat(recarregada.getStatus()).isEqualTo(StatusConta.PARCIAL);
        assertThat(recarregada.getValorPago()).isEqualByComparingTo("400.00");
    }
}
