package com.sawhub.hub.financeiro;

import com.sawhub.hub.financeiro.dto.CaixaMensalResponse;
import com.sawhub.hub.financeiro.dto.PosicaoCaixaMensalResponse;
import com.sawhub.hub.financeiro.dto.RegistrarPosicaoCaixaRequest;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** "Caixa do mês: Inicial, saldo por banco, Final" (change request pós-MVP, E14, reunião
 * 17/07/2026) — entrada manual do Admin, mesmo critério da planilha real (ver
 * {@link PosicaoCaixaMensal}), não derivada de {@link LancamentoFinanceiro}. */
@Service
public class CaixaMensalService {

    private final ContaBancariaRepository contaBancariaRepository;
    private final PosicaoCaixaMensalRepository posicaoRepository;

    public CaixaMensalService(ContaBancariaRepository contaBancariaRepository,
                               PosicaoCaixaMensalRepository posicaoRepository) {
        this.contaBancariaRepository = contaBancariaRepository;
        this.posicaoRepository = posicaoRepository;
    }

    /** Upsert por (conta, ano, mês) — registrar de novo o mesmo período só corrige o valor já
     * digitado, não duplica linha (mesma natureza de entrada manual da planilha real). */
    @Transactional
    public PosicaoCaixaMensal registrarPosicao(RegistrarPosicaoCaixaRequest request) {
        ContaBancaria conta = contaBancariaRepository.findById(request.contaBancariaId())
                .orElseThrow(() -> new IllegalArgumentException("Conta bancária não encontrada."));

        PosicaoCaixaMensal posicao = posicaoRepository
                .findByContaBancariaIdAndAnoAndMes(request.contaBancariaId(), request.ano(), request.mes())
                .orElse(null);
        if (posicao == null) {
            return posicaoRepository.save(new PosicaoCaixaMensal(conta, request.ano(), request.mes(),
                    request.saldoInicial(), request.saldoFinal()));
        }
        posicao.atualizar(request.saldoInicial(), request.saldoFinal());
        return posicaoRepository.save(posicao);
    }

    public CaixaMensalResponse caixaDoMes(int ano, int mes) {
        List<PosicaoCaixaMensal> posicoes = posicaoRepository.buscarPorAnoMesComConta(ano, mes);
        List<PosicaoCaixaMensalResponse> contas = posicoes.stream()
                .map(PosicaoCaixaMensalResponse::from)
                .toList();
        BigDecimal totalInicial = posicoes.stream().map(PosicaoCaixaMensal::getSaldoInicial)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFinal = posicoes.stream().map(PosicaoCaixaMensal::getSaldoFinal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CaixaMensalResponse(ano, mes, contas, totalInicial, totalFinal);
    }
}
