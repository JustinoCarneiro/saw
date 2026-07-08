package com.sawhub.hub.financeiro;

import com.sawhub.hub.financeiro.dto.CriarContaRequest;
import com.sawhub.hub.financeiro.dto.LiquidarContaRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** H14.4 — contas a pagar/receber. */
@Service
public class ContaPagarReceberService {

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
        if (tipo != null) {
            return contaRepository.findByTipoOrderByDataVencimentoAsc(tipo).stream()
                    .filter(c -> status == null || c.getStatus() == status)
                    .toList();
        }
        if (status != null) {
            return contaRepository.findByStatusOrderByDataVencimentoAsc(status);
        }
        return contaRepository.findAllByOrderByDataVencimentoAsc();
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
}
