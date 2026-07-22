package com.sawhub.hub.financeiro;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/** "Caixa do mês" + "Transferências Entre Contas" (change request pós-MVP, E14, reunião
 * 17/07/2026, ver V51). @DataJpaTest de propósito (sessão real do Hibernate) — mesmo raciocínio
 * de VendaIngressoRepositoryTest: as duas queries custom têm LEFT JOIN FETCH, um mock nunca
 * reproduziria LazyInitializationException se a FETCH sumisse numa mudança futura. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class CaixaBancarioRepositoryTest {

    @Autowired
    private ContaBancariaRepository contaBancariaRepository;
    @Autowired
    private PosicaoCaixaMensalRepository posicaoRepository;
    @Autowired
    private TransferenciaBancariaRepository transferenciaRepository;
    @Autowired
    private EntityManager entityManager;

    private ContaBancaria criarConta(String sufixo) {
        return contaBancariaRepository.save(new ContaBancaria("Conta teste " + sufixo + " " + UUID.randomUUID()));
    }

    @Test
    void contaBancariaComNomeDuplicadoViolaConstraintUnica() {
        String nome = "Conta única " + UUID.randomUUID();
        contaBancariaRepository.saveAndFlush(new ContaBancaria(nome));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> contaBancariaRepository.saveAndFlush(new ContaBancaria(nome)))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void buscarPorAnoMesComContaTrazSaldoInicializadoForaDaTransacao() {
        ContaBancaria conta = criarConta("A");
        posicaoRepository.save(new PosicaoCaixaMensal(conta, 2026, 7, new BigDecimal("1000.00"), new BigDecimal("1500.00")));
        entityManager.flush();
        entityManager.clear();

        List<PosicaoCaixaMensal> posicoes = posicaoRepository.buscarPorAnoMesComConta(2026, 7);

        assertThat(posicoes).anySatisfy(p -> {
            assertThat(p.getContaBancaria().getNome()).isEqualTo(conta.getNome()); // não estoura LazyInitializationException
            assertThat(p.getSaldoInicial()).isEqualByComparingTo("1000.00");
        });
    }

    @Test
    void posicaoCaixaMensalDuplicadaParaMesmaContaAnoMesViolaConstraintUnica() {
        ContaBancaria conta = criarConta("B");
        posicaoRepository.saveAndFlush(new PosicaoCaixaMensal(conta, 2026, 8, new BigDecimal("100"), new BigDecimal("200")));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> posicaoRepository.saveAndFlush(
                        new PosicaoCaixaMensal(conta, 2026, 8, new BigDecimal("300"), new BigDecimal("400"))))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void buscarPorPeriodoTrazContasInicializadasForaDaTransacao() {
        ContaBancaria origem = criarConta("C");
        ContaBancaria destino = criarConta("D");
        transferenciaRepository.save(new TransferenciaBancaria(origem, destino, new BigDecimal("500.00"),
                LocalDate.of(2026, 7, 15), "Empréstimo interno"));
        entityManager.flush();
        entityManager.clear();

        List<TransferenciaBancaria> transferencias =
                transferenciaRepository.buscarPorPeriodo(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertThat(transferencias).anySatisfy(t -> {
            assertThat(t.getContaOrigem().getNome()).isEqualTo(origem.getNome());
            assertThat(t.getContaDestino().getNome()).isEqualTo(destino.getNome());
        });
    }
}
