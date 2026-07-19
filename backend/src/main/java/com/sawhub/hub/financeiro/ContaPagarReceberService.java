package com.sawhub.hub.financeiro;

import com.sawhub.hub.financeiro.dto.CriarContaRequest;
import com.sawhub.hub.financeiro.dto.LiquidarContaRequest;
import com.sawhub.hub.financeiro.dto.LiquidarParcialContaRequest;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** H14.4 — contas a pagar/receber. */
@Service
public class ContaPagarReceberService {

    // Sentinela pro filtro de período "desligado" (ver listar) — cobre qualquer dataVencimento
    // real folgadamente, ficando bem dentro do range de DATE do Postgres (evita repetir o
    // problema de parâmetro nulo sem tipo que ContaPagarReceberRepository.buscarComFiltro já
    // documenta).
    private static final LocalDate SEM_FILTRO_INICIO = LocalDate.of(1900, 1, 1);
    private static final LocalDate SEM_FILTRO_FIM = LocalDate.of(2999, 12, 31);

    private final ContaPagarReceberRepository contaRepository;
    private final LancamentoFinanceiroRepository lancamentoRepository;
    private final CategoriaFinanceiraRepository categoriaRepository;

    public ContaPagarReceberService(ContaPagarReceberRepository contaRepository,
                                     LancamentoFinanceiroRepository lancamentoRepository,
                                     CategoriaFinanceiraRepository categoriaRepository) {
        this.contaRepository = contaRepository;
        this.lancamentoRepository = lancamentoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional
    public ContaPagarReceber criar(CriarContaRequest request) {
        CategoriaFinanceira categoria = request.categoriaId() == null ? null
                : categoriaRepository.findById(request.categoriaId())
                        .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada."));
        ContaPagarReceber conta = new ContaPagarReceber(request.tipo(), request.descricao(), request.valor(),
                request.dataVencimento(), categoria);
        return contaRepository.save(conta);
    }

    public List<ContaPagarReceber> listar(TipoConta tipo, StatusConta status) {
        return listar(tipo, status, null, null);
    }

    /** Change request 17/07/2026 ("filtro mensal") — ano/mes juntos viram uma janela
     * [1º dia do mês, 1º dia do mês seguinte) sobre {@code dataVencimento}; qualquer um dos dois
     * nulo desliga o filtro de período (mantém 100% do comportamento anterior pra quem não passa
     * ano/mes, ver overload acima). */
    public List<ContaPagarReceber> listar(TipoConta tipo, StatusConta status, Integer ano, Integer mes) {
        LocalDate inicio = SEM_FILTRO_INICIO;
        LocalDate fim = SEM_FILTRO_FIM;
        if (ano != null && mes != null) {
            YearMonth periodo = YearMonth.of(ano, mes);
            inicio = periodo.atDay(1);
            fim = periodo.plusMonths(1).atDay(1);
        }
        return contaRepository.buscarComFiltro(tipo, status, inicio, fim);
    }

    /** Gera o Lançamento REALIZADO correspondente quando `criarLancamento=true` — exige que a
     * conta já tenha uma categoria definida na criação, senão não tem como classificar o
     * lançamento gerado (H14.4 + H14.1 andam juntos aqui). */
    @Transactional
    public ContaPagarReceber liquidar(UUID contaId, LiquidarContaRequest request) {
        ContaPagarReceber conta = contaRepository.findById(contaId)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada."));

        LancamentoFinanceiro lancamentoGerado = null;
        if (request.criarLancamento()) {
            if (conta.getCategoria() == null) {
                throw new IllegalStateException("Conta sem categoria não pode gerar lançamento automático.");
            }
            TipoLancamento tipoLancamento = conta.getTipo() == TipoConta.A_PAGAR
                    ? TipoLancamento.DESPESA : TipoLancamento.RECEITA;
            lancamentoGerado = lancamentoRepository.save(new LancamentoFinanceiro(tipoLancamento, conta.getCategoria(),
                    conta.getDescricao(), conta.getValor(), request.dataPagamento(), StatusLancamento.REALIZADO, null));
        }

        conta.liquidar(request.dataPagamento(), lancamentoGerado);
        return contaRepository.save(conta);
    }

    @Transactional
    public ContaPagarReceber liquidarParcial(UUID contaId, LiquidarParcialContaRequest request) {
        ContaPagarReceber conta = contaRepository.findById(contaId)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada."));
        conta.liquidarParcial(request.valorPago(), request.dataPagamento());
        return contaRepository.save(conta);
    }
}
