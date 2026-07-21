package com.sawhub.hub.comercial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sawhub.hub.security.Perfil;
import com.sawhub.hub.security.Usuario;
import com.sawhub.hub.security.UsuarioRepository;
import com.sawhub.hub.team.Area;
import com.sawhub.hub.team.Colaborador;
import com.sawhub.hub.team.ColaboradorRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

/** Bug achado ao vivo durante a verificação de M05 via curl: {@code LeadService.avancar()} usava
 * {@code findById()} puro, que devolve {@code vendedor} como proxy LAZY. Qualquer transição além
 * da própria atribuição de vendedor (ex.: EM_CONTATO -&gt; PROPOSTA) explodia com
 * LazyInitializationException ao montar o {@code LeadResponse} fora da transação
 * (open-in-view=false). Usa @DataJpaTest (sessão real do Hibernate) de propósito — um teste
 * baseado em mock nunca reproduziria isso, um mock nunca é um proxy LAZY. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class LeadRepositoryTest {

    @Autowired
    private LeadRepository leadRepository;
    @Autowired
    private ColaboradorRepository colaboradorRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private EntityManager entityManager;

    private Colaborador criarVendedor(String sufixo) {
        Usuario usuario = usuarioRepository.save(
                new Usuario("vendedor" + sufixo + "@sawhub.com.br", "hash", Perfil.ADMIN));
        return colaboradorRepository.save(new Colaborador(usuario, "Paula", Area.COMERCIAL));
    }

    @Test
    void findByIdPuroMantemVendedorComoProxyLazyNaoInicializado() {
        // RED — documenta o bug: findById() puro é a causa raiz, não usar mais em avancar().
        Colaborador vendedor = criarVendedor("1");
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null);
        lead.moverParaEmContato(vendedor);
        Lead salvo = leadRepository.save(lead);
        entityManager.flush();
        entityManager.clear(); // simula o fim da transação anterior — próxima leitura é um objeto novo

        Lead recarregado = leadRepository.findById(salvo.getId()).orElseThrow();
        entityManager.clear(); // vendedor.nome só existiria se já tivesse sido tocado dentro da sessão

        assertThatThrownBy(() -> recarregado.getVendedor().getNome())
                .isInstanceOf(LazyInitializationException.class);
    }

    @Test
    void buscarPorIdComVendedorInicializaVendedorMesmoForaDaTransacaoOriginal() {
        Colaborador vendedor = criarVendedor("2");
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null);
        lead.moverParaEmContato(vendedor);
        Lead salvo = leadRepository.save(lead);
        entityManager.flush();
        entityManager.clear();

        Lead recarregado = leadRepository.buscarPorIdComVendedor(salvo.getId()).orElseThrow();
        entityManager.clear();

        assertThat(recarregado.getVendedor().getNome()).isEqualTo("Paula");
    }

    // M25 (Suposição 7) — teste contra banco real de propósito: a query mistura "produtoVenda IS
    // NULL" (leads fechados antes do M25 existir, ou pelo caminho legado via Plano — removido no
    // M28, nunca setava produtoVenda) com "<> :produtoExcluido" (caminho atual, fecharVenda()) —
    // um mock nunca provaria que o SQL gerado trata os dois casos certo (em especial NULL, que em
    // SQL puro "NULL <> X" é UNKNOWN, não TRUE).
    //
    // M28 — Lead.fechar(Plano) foi removido junto com Plano, então a entidade não tem mais nenhum
    // jeito de nascer com produtoVenda NULL num status FECHADO (fecharVenda() sempre seta esse
    // campo). Pra continuar cobrindo a query que ainda precisa tratar dado histórico com essa
    // forma, simula a linha legada via fecharVenda() normal + reflection só pra apagar
    // produtoVenda depois — não existe mais um caminho de entidade legítimo que produza esse
    // estado, então reflection aqui documenta a exceção em vez de reintroduzir Lead.fechar(Plano).
    @Test
    void countByStatusAndDataFechamentoBetweenExcluindoProdutoIncluiLegadoEExcluiSoOProdutoIndicado() {
        Colaborador vendedor = criarVendedor("3");
        Instant antes = Instant.now().minusSeconds(60);
        Instant depois = Instant.now().plusSeconds(60);

        Lead legado = new Lead("Legado", "legado@restaurante.com", null, null);
        legado.moverParaEmContato(vendedor);
        legado.moverParaProposta();
        legado.fecharVenda(ProdutoVenda.MENTORIA_CONTINUA, OrigemVenda.DIRETA, new BigDecimal("1.00"),
                null, FormaPagamento.PIX);
        ReflectionTestUtils.setField(legado, "produtoVenda", null);
        leadRepository.save(legado);

        Lead mentoria = new Lead("Mentoria", "mentoria@restaurante.com", null, null);
        mentoria.moverParaEmContato(vendedor);
        mentoria.moverParaProposta();
        mentoria.fecharVenda(ProdutoVenda.MENTORIA_CONTINUA, OrigemVenda.DIRETA, new BigDecimal("26000.00"),
                new BigDecimal("6000.00"), FormaPagamento.PIX);
        leadRepository.save(mentoria);

        Lead ingresso = new Lead("Ingresso", "ingresso@restaurante.com", null, null);
        ingresso.moverParaEmContato(vendedor);
        ingresso.moverParaProposta();
        ingresso.fecharVenda(ProdutoVenda.INGRESSO_EVENTO, OrigemVenda.DIRETA, new BigDecimal("300.00"),
                new BigDecimal("300.00"), FormaPagamento.PIX);
        leadRepository.save(ingresso);

        entityManager.flush();
        entityManager.clear();

        long total = leadRepository.countByStatusAndDataFechamentoBetween(StatusLead.FECHADO, antes, depois);
        long semIngresso = leadRepository.countByStatusAndDataFechamentoBetweenExcluindoProduto(
                StatusLead.FECHADO, antes, depois, ProdutoVenda.INGRESSO_EVENTO);

        assertThat(total).isEqualTo(3);
        assertThat(semIngresso).isEqualTo(2);
    }

    // Gap 2 (raio-x, confirmado 19/07/2026) — contra banco real de propósito: só isso prova que a
    // migração V32 realmente trocou o CHECK constraint chk_lead_origem_venda pra aceitar PARCEIRO
    // (mesmo tipo de bug já achado ao vivo com chk_lead_status na V26).
    @Test
    void origemVendaParceiroSobreviveCheckConstraintForaDaTransacaoOriginal() {
        Colaborador vendedor = criarVendedor("4");
        Lead lead = new Lead("Parceiro", "parceiro@restaurante.com", null, null);
        lead.moverParaEmContato(vendedor);
        lead.moverParaProposta();
        lead.fecharVenda(ProdutoVenda.CONSULTORIA, OrigemVenda.PARCEIRO, new BigDecimal("5000.00"),
                new BigDecimal("5000.00"), FormaPagamento.PIX);
        Lead salvo = leadRepository.save(lead);
        entityManager.flush();
        entityManager.clear();

        Lead recarregado = leadRepository.findById(salvo.getId()).orElseThrow();
        entityManager.clear();

        assertThat(recarregado.getOrigemVenda()).isEqualTo(OrigemVenda.PARCEIRO);
    }

    // Gap 7 (raio-x + pesquisa da taxa real da Hotmart, confirmado 19/07/2026) — contra banco
    // real de propósito: só isso prova que taxaPlataformaRetida sobrevive o round-trip pgcrypto
    // (@ColumnTransformer) igual valorTotalVenda/valorPagoNoAto.
    @Test
    void taxaPlataformaRetidaSobrevivePgcryptoRoundTripForaDaTransacaoOriginal() {
        Colaborador vendedor = criarVendedor("5");
        Lead lead = new Lead("Hotmart", "hotmart@restaurante.com", null, null);
        lead.moverParaEmContato(vendedor);
        lead.moverParaProposta();
        lead.fecharVenda(ProdutoVenda.PRODUTO_DIGITAL, OrigemVenda.HOTMART, new BigDecimal("1000.00"),
                new BigDecimal("890.00"), FormaPagamento.HOTMART, new BigDecimal("110.00"));
        Lead salvo = leadRepository.save(lead);
        entityManager.flush();
        entityManager.clear();

        Lead recarregado = leadRepository.findById(salvo.getId()).orElseThrow();
        entityManager.clear();

        assertThat(recarregado.getTaxaPlataformaRetida()).isEqualByComparingTo("110.00");
    }
}
