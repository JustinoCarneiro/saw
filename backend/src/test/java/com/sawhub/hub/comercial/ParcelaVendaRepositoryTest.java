package com.sawhub.hub.comercial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sawhub.hub.financeiro.CategoriaFinanceira;
import com.sawhub.hub.financeiro.CategoriaFinanceiraRepository;
import com.sawhub.hub.financeiro.GrupoDre;
import com.sawhub.hub.financeiro.LancamentoFinanceiro;
import com.sawhub.hub.financeiro.LancamentoFinanceiroRepository;
import com.sawhub.hub.financeiro.StatusLancamento;
import com.sawhub.hub.financeiro.TipoLancamento;
import com.sawhub.hub.security.Perfil;
import com.sawhub.hub.security.Usuario;
import com.sawhub.hub.security.UsuarioRepository;
import com.sawhub.hub.team.Area;
import com.sawhub.hub.team.Colaborador;
import com.sawhub.hub.team.ColaboradorRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/** Change request 17/07/2026 ("conciliação") — {@code buscarPorLeadIdComConta} precisa de
 * LEFT JOIN FETCH em {@code lancamento} porque {@link ConciliacaoService} lê
 * {@code lancamento.getStatus()} fora da transação original. M26 repontou de ContaPagarReceber
 * pra LancamentoFinanceiro (merge de entidade, ver ROADMAP.md § "Blueprint (M26)").
 * @DataJpaTest de propósito (sessão real do Hibernate) — mesmo raciocínio do
 * {@code LeadRepositoryTest} (M05): um mock nunca reproduz um proxy LAZY não inicializado. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ParcelaVendaRepositoryTest {

    @Autowired
    private ParcelaVendaRepository parcelaVendaRepository;
    @Autowired
    private LeadRepository leadRepository;
    @Autowired
    private ColaboradorRepository colaboradorRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private CategoriaFinanceiraRepository categoriaRepository;
    @Autowired
    private LancamentoFinanceiroRepository lancamentoRepository;
    @Autowired
    private EntityManager entityManager;

    private Lead criarLeadFechado(String sufixo) {
        Usuario usuario = usuarioRepository.save(new Usuario("vendedor" + sufixo + "@sawhub.com.br", "hash", Perfil.ADMIN));
        Colaborador vendedor = colaboradorRepository.save(new Colaborador(usuario, "Paula" + sufixo, Area.COMERCIAL));
        Lead lead = new Lead("Comprador " + sufixo, "comprador" + sufixo + "@example.com", null, null, null);
        lead.moverParaEmContato(vendedor);
        lead.moverParaProposta();
        lead.fecharVenda(ProdutoVenda.MENTORIA_CONTINUA, OrigemVenda.DIRETA, new BigDecimal("10000.00"),
                new BigDecimal("2000.00"), FormaPagamento.PIX);
        return leadRepository.save(lead);
    }

    @Test
    void findByLeadIdPuroMantemLancamentoComoProxyLazyNaoInicializado() {
        // RED — documenta o bug: findByLeadId() (derivado, sem JOIN FETCH) é a causa raiz.
        Lead lead = criarLeadFechado("1");
        CategoriaFinanceira categoria = categoriaRepository.save(
                new CategoriaFinanceira("Assinaturas", TipoLancamento.RECEITA, GrupoDre.RECEITA_BRUTA, null));
        LancamentoFinanceiro lancamento = lancamentoRepository.save(new LancamentoFinanceiro(TipoLancamento.RECEITA,
                categoria, "Parcela 1", new BigDecimal("8000.00"), LocalDate.of(2026, 8, 17),
                StatusLancamento.PREVISTO, null, null, LocalDate.of(2026, 8, 17)));
        ParcelaVenda parcela = new ParcelaVenda(lead, 1, new BigDecimal("8000.00"), LocalDate.of(2026, 8, 17));
        parcela.vincularLancamento(lancamento);
        parcelaVendaRepository.save(parcela);
        entityManager.flush();
        entityManager.clear();

        ParcelaVenda recarregada = parcelaVendaRepository.findByLeadId(lead.getId()).get(0);
        entityManager.clear();

        assertThatThrownBy(() -> recarregada.getLancamento().getStatus())
                .isInstanceOf(LazyInitializationException.class);
    }

    @Test
    void buscarPorLeadIdComContaInicializaLancamentoMesmoForaDaTransacaoOriginal() {
        Lead lead = criarLeadFechado("2");
        CategoriaFinanceira categoria = categoriaRepository.save(
                new CategoriaFinanceira("Assinaturas2", TipoLancamento.RECEITA, GrupoDre.RECEITA_BRUTA, null));
        LancamentoFinanceiro lancamento = lancamentoRepository.save(new LancamentoFinanceiro(TipoLancamento.RECEITA,
                categoria, "Parcela 1", new BigDecimal("8000.00"), LocalDate.of(2026, 8, 17),
                StatusLancamento.PREVISTO, null, null, LocalDate.of(2026, 8, 17)));
        ParcelaVenda parcela = new ParcelaVenda(lead, 1, new BigDecimal("8000.00"), LocalDate.of(2026, 8, 17));
        parcela.vincularLancamento(lancamento);
        parcelaVendaRepository.save(parcela);
        entityManager.flush();
        entityManager.clear();

        ParcelaVenda recarregada = parcelaVendaRepository.buscarPorLeadIdComConta(lead.getId()).get(0);
        entityManager.clear();

        assertThat(recarregada.getLancamento().getStatus()).isNotNull();
    }

    @Test
    void buscarPorLeadIdComContaFuncionaQuandoParcelaAindaNaoTemContaVinculada() {
        Lead lead = criarLeadFechado("3");
        ParcelaVenda semConta = new ParcelaVenda(lead, 1, new BigDecimal("8000.00"), LocalDate.of(2026, 8, 17));
        parcelaVendaRepository.save(semConta);
        entityManager.flush();
        entityManager.clear();

        var resultado = parcelaVendaRepository.buscarPorLeadIdComConta(lead.getId());
        entityManager.clear();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getLancamento()).isNull();
    }
}
