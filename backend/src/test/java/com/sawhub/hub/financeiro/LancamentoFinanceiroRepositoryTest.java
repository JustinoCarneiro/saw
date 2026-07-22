package com.sawhub.hub.financeiro;

import static org.assertj.core.api.Assertions.assertThat;

import com.sawhub.hub.evento.Evento;
import com.sawhub.hub.evento.EventoRepository;
import com.sawhub.hub.evento.TipoEvento;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
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
 * reaparece em claro nesta terceira coluna. M26 fundiu ContaPagarReceberRepositoryTest aqui
 * (merge de entidade, ver ROADMAP.md § "Blueprint (M26)"). @DataJpaTest de propósito (sessão real
 * do Hibernate) — mesmo raciocínio de AtividadeLogRepositoryTest. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class LancamentoFinanceiroRepositoryTest {

    @Autowired
    private LancamentoFinanceiroRepository lancamentoRepository;
    @Autowired
    private CategoriaFinanceiraRepository categoriaRepository;
    @Autowired
    private EventoRepository eventoRepository;
    @Autowired
    private EntityManager entityManager;

    // Sentinela pro filtro de período "desligado" — mesma faixa usada por LancamentoService,
    // nunca null: Postgres não consegue inferir o tipo de um parâmetro LocalDate nulo nesta query
    // (muita coluna bytea de pgcrypto ao redor, ver comentário em
    // LancamentoFinanceiroRepository.buscarComFiltroPorVencimento).
    private static final LocalDate SEM_FILTRO_INICIO = LocalDate.of(1900, 1, 1);
    private static final LocalDate SEM_FILTRO_FIM = LocalDate.of(2999, 12, 31);

    private CategoriaFinanceira criarCategoria() {
        return categoriaRepository.save(new CategoriaFinanceira("Infra teste " + UUID.randomUUID(),
                TipoLancamento.DESPESA, GrupoDre.CUSTOS, null));
    }

    private Evento criarEvento(String sufixo) {
        return eventoRepository.save(new Evento("Evento teste " + sufixo, TipoEvento.PRESENCIAL, null,
                Instant.now(), "Recife", null, 100));
    }

    private LancamentoFinanceiro salvarComVencimento(CategoriaFinanceira categoria, String descricao,
                                                       String valor, LocalDate dataVencimento) {
        return lancamentoRepository.save(new LancamentoFinanceiro(TipoLancamento.DESPESA, categoria, descricao,
                new BigDecimal(valor), dataVencimento, StatusLancamento.PREVISTO, null, dataVencimento));
    }

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

    // Evento no Financeiro — mesmo raciocínio de ContaPagarReceberRepositoryTest: sem o LEFT JOIN
    // FETCH l.evento nas duas queries de LancamentoFinanceiroRepository, LancamentoResponse.from()
    // dispara LazyInitializationException ao ler l.getEvento().getTitulo() fora da transação.
    @Test
    void findByDataCompetenciaBetweenInicializaEventoMesmoForaDaTransacaoOriginal() {
        CategoriaFinanceira categoria = categoriaRepository.save(new CategoriaFinanceira(
                "Ingressos teste " + UUID.randomUUID(), TipoLancamento.RECEITA, GrupoDre.RECEITA_BRUTA, null));
        Evento evento = eventoRepository.save(new Evento("Workshop teste", TipoEvento.PRESENCIAL, null,
                Instant.now(), "Recife", null, 100));
        LancamentoFinanceiro salvo = lancamentoRepository.save(new LancamentoFinanceiro(TipoLancamento.RECEITA, categoria,
                "Ingresso Workshop teste", new BigDecimal("300.00"), LocalDate.of(2026, 8, 20),
                StatusLancamento.REALIZADO, evento));
        entityManager.flush();
        entityManager.clear();

        // Escopado pelo próprio id, não anySatisfy solto — o intervalo (1º-31/ago) não é exclusivo
        // deste teste: banco de dev real (@AutoConfigureTestDatabase Replace.NONE) pode ter outros
        // lançamentos na mesma janela (achado ao vivo: anySatisfy caiu num lançamento pré-existente
        // sem evento e a lambda tomou NPE antes de chegar no nosso). Mesmo padrão já usado logo
        // abaixo em buscarComFiltroPorVencimentoSemFiltroNenhumInicializaCategoriaMesmoForaDaTransacaoOriginal.
        var lancamentos = lancamentoRepository.findByDataCompetenciaBetweenOrderByDataCompetenciaDesc(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));
        entityManager.clear();

        LancamentoFinanceiro recarregado = lancamentos.stream()
                .filter(l -> l.getId().equals(salvo.getId())).findFirst().orElseThrow();
        assertThat(recarregado.getEvento().getTitulo()).isEqualTo("Workshop teste");
    }

    // M26 (absorvido de ContaPagarReceberRepositoryTest.buscarComFiltroSemFiltroNenhum...).
    @Test
    void buscarComFiltroPorVencimentoSemFiltroNenhumInicializaCategoriaMesmoForaDaTransacaoOriginal() {
        CategoriaFinanceira categoria = criarCategoria();
        LancamentoFinanceiro salvo = salvarComVencimento(categoria, "Servidor", "180.00", LocalDate.of(2026, 7, 20));
        entityManager.flush();
        entityManager.clear();

        LancamentoFinanceiro recarregado = lancamentoRepository
                .buscarComFiltroPorVencimento(null, null, null, SEM_FILTRO_INICIO, SEM_FILTRO_FIM).stream()
                .filter(l -> l.getId().equals(salvo.getId())).findFirst().orElseThrow();
        entityManager.clear();

        assertThat(recarregado.getCategoria().getNome()).isEqualTo(categoria.getNome());
    }

    @Test
    void buscarComFiltroPorVencimentoPorTipoInicializaCategoriaMesmoForaDaTransacaoOriginal() {
        CategoriaFinanceira categoria = criarCategoria();
        LancamentoFinanceiro salvo = salvarComVencimento(categoria, "Servidor", "180.00", LocalDate.of(2026, 7, 20));
        entityManager.flush();
        entityManager.clear();

        LancamentoFinanceiro recarregado = lancamentoRepository
                .buscarComFiltroPorVencimento(TipoLancamento.DESPESA, null, null, SEM_FILTRO_INICIO, SEM_FILTRO_FIM).stream()
                .filter(l -> l.getId().equals(salvo.getId())).findFirst().orElseThrow();
        entityManager.clear();

        assertThat(recarregado.getCategoria().getNome()).isEqualTo(categoria.getNome());
    }

    @Test
    void buscarComFiltroPorVencimentoPorStatusInicializaCategoriaMesmoForaDaTransacaoOriginal() {
        CategoriaFinanceira categoria = criarCategoria();
        LancamentoFinanceiro salvo = salvarComVencimento(categoria, "Servidor", "180.00", LocalDate.of(2026, 7, 20));
        entityManager.flush();
        entityManager.clear();

        LancamentoFinanceiro recarregado = lancamentoRepository
                .buscarComFiltroPorVencimento(null, StatusLancamento.PREVISTO, null, SEM_FILTRO_INICIO, SEM_FILTRO_FIM).stream()
                .filter(l -> l.getId().equals(salvo.getId())).findFirst().orElseThrow();
        entityManager.clear();

        assertThat(recarregado.getCategoria().getNome()).isEqualTo(categoria.getNome());
    }

    // Change request 17/07/2026 ("filtro mensal") — contra banco real de propósito: prova que o
    // filtro [inicio, fim) sobre dataVencimento realmente inclui/exclui a linha certa, não só que
    // a query compila. Também prova, por construção, que um lançamento sem vencimento (dataVencimento
    // NULL) nunca aparece neste filtro — comparação com NULL é sempre falsa em SQL.
    @Test
    void buscarComFiltroPorVencimentoIncluiSoLancamentosDentroDaJanela() {
        CategoriaFinanceira categoria = criarCategoria();
        LancamentoFinanceiro dentroDoMes = salvarComVencimento(categoria, "Dentro", "100.00", LocalDate.of(2026, 7, 15));
        LancamentoFinanceiro foraDoMes = salvarComVencimento(categoria, "Fora", "100.00", LocalDate.of(2026, 8, 1));
        LancamentoFinanceiro semVencimento = lancamentoRepository.save(new LancamentoFinanceiro(TipoLancamento.DESPESA,
                categoria, "Sem vencimento", new BigDecimal("100.00"), LocalDate.of(2026, 7, 15), StatusLancamento.REALIZADO, null));
        entityManager.flush();
        entityManager.clear();

        var resultado = lancamentoRepository.buscarComFiltroPorVencimento(null, null, null,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 1));
        entityManager.clear();

        assertThat(resultado).extracting(LancamentoFinanceiro::getId).contains(dentroDoMes.getId());
        assertThat(resultado).extracting(LancamentoFinanceiro::getId)
                .doesNotContain(foraDoMes.getId(), semVencimento.getId());
    }

    // Change request 17/07/2026 ("evento no financeiro") — contra banco real de propósito: prova
    // que buscarPorIdComEvento inicializa o evento (LEFT JOIN FETCH), evitando o
    // LazyInitializationException que os outros finders já documentam pra este mesmo repositório.
    @Test
    void buscarPorIdComEventoInicializaEventoMesmoForaDaTransacaoOriginal() {
        CategoriaFinanceira categoria = criarCategoria();
        Evento evento = criarEvento("1");
        LancamentoFinanceiro salvo = lancamentoRepository.save(new LancamentoFinanceiro(TipoLancamento.DESPESA,
                categoria, "Buffet do evento", new BigDecimal("2000.00"), LocalDate.of(2026, 8, 20),
                StatusLancamento.PREVISTO, evento, LocalDate.of(2026, 8, 20)));
        entityManager.flush();
        entityManager.clear();

        LancamentoFinanceiro recarregado = lancamentoRepository.buscarPorIdComEvento(salvo.getId()).orElseThrow();
        entityManager.clear();

        assertThat(recarregado.getEvento().getTitulo()).isEqualTo(evento.getTitulo());
    }

    @Test
    void buscarComFiltroPorVencimentoPorEventoIncluiSoLancamentosDoEvento() {
        CategoriaFinanceira categoria = criarCategoria();
        Evento eventoA = criarEvento("A");
        Evento eventoB = criarEvento("B");
        LancamentoFinanceiro doEventoA = lancamentoRepository.save(new LancamentoFinanceiro(TipoLancamento.DESPESA,
                categoria, "Do evento A", new BigDecimal("100.00"), LocalDate.of(2026, 8, 1), StatusLancamento.PREVISTO,
                eventoA, LocalDate.of(2026, 8, 1)));
        LancamentoFinanceiro doEventoB = lancamentoRepository.save(new LancamentoFinanceiro(TipoLancamento.DESPESA,
                categoria, "Do evento B", new BigDecimal("100.00"), LocalDate.of(2026, 8, 1), StatusLancamento.PREVISTO,
                eventoB, LocalDate.of(2026, 8, 1)));
        entityManager.flush();
        entityManager.clear();

        var resultado = lancamentoRepository.buscarComFiltroPorVencimento(null, null, eventoA.getId(),
                SEM_FILTRO_INICIO, SEM_FILTRO_FIM);
        entityManager.clear();

        assertThat(resultado).extracting(LancamentoFinanceiro::getId).contains(doEventoA.getId());
        assertThat(resultado).extracting(LancamentoFinanceiro::getId).doesNotContain(doEventoB.getId());
    }

    // M26 — buscarComFiltroPorCompetencia é o filtro novo de GET /lancamentos (ganhou status e
    // eventoId, absorvidos de Contas). Contra banco real de propósito, mesmo raciocínio dos testes
    // acima: prova a janela [inicio, fim) e os filtros opcionais de verdade, não só que compila.
    @Test
    void buscarComFiltroPorCompetenciaFiltraPorStatusECategoria() {
        CategoriaFinanceira categoria = criarCategoria();
        LancamentoFinanceiro realizado = lancamentoRepository.save(new LancamentoFinanceiro(TipoLancamento.DESPESA,
                categoria, "Realizado", new BigDecimal("100.00"), LocalDate.of(2026, 7, 15), StatusLancamento.REALIZADO, null));
        LancamentoFinanceiro previsto = lancamentoRepository.save(new LancamentoFinanceiro(TipoLancamento.DESPESA,
                categoria, "Previsto", new BigDecimal("100.00"), LocalDate.of(2026, 7, 16), StatusLancamento.PREVISTO, null));
        entityManager.flush();
        entityManager.clear();

        var resultado = lancamentoRepository.buscarComFiltroPorCompetencia(null, categoria.getId(),
                StatusLancamento.REALIZADO, null, null, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));
        entityManager.clear();

        assertThat(resultado).extracting(LancamentoFinanceiro::getId).contains(realizado.getId());
        assertThat(resultado).extracting(LancamentoFinanceiro::getId).doesNotContain(previsto.getId());
    }

    // Gap 1 (raio-x, 18/07/2026) — contra banco real de propósito: só isso prova que o CHECK
    // constraint chk_lancamento_status aceita PARCIAL/VENCIDO depois da V40 (mesmo tipo de bug já
    // achado ao vivo com chk_lead_status na V26 — um mock nunca estoura violação de CHECK).
    @Test
    void liquidarParcialSobreviveCheckConstraintForaDaTransacaoOriginal() {
        CategoriaFinanceira categoria = categoriaRepository.save(new CategoriaFinanceira(
                "Mensalidades teste " + UUID.randomUUID(), TipoLancamento.RECEITA, GrupoDre.RECEITA_BRUTA, null));
        LancamentoFinanceiro salvo = lancamentoRepository.save(new LancamentoFinanceiro(TipoLancamento.RECEITA,
                categoria, "Mensalidade parcial", new BigDecimal("1000.00"), LocalDate.of(2026, 8, 20),
                StatusLancamento.PREVISTO, null, LocalDate.of(2026, 8, 20)));
        salvo.liquidarParcial(new BigDecimal("400.00"), LocalDate.of(2026, 7, 19));
        lancamentoRepository.save(salvo);
        entityManager.flush();
        entityManager.clear();

        LancamentoFinanceiro recarregado = lancamentoRepository.findById(salvo.getId()).orElseThrow();
        entityManager.clear();

        assertThat(recarregado.getStatus()).isEqualTo(StatusLancamento.PARCIAL);
        assertThat(recarregado.getValorPago()).isEqualByComparingTo("400.00");
    }
}
