package com.sawhub.hub.financeiro.dto;

import com.sawhub.hub.financeiro.StatusLancamento;
import com.sawhub.hub.financeiro.TipoLancamento;
import com.sawhub.hub.mentorado.Plano;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** M26 — ganhou `dataVencimento` (opcional; ausente = lançamento direto, sem prazo) e `eventoId`
 * (opcional), absorvendo o antigo `CriarContaRequest`. `categoriaId` passou a ser obrigatório em
 * toda leva (nenhum construtor secundário nulável aqui: essa é a mudança de comportamento
 * pretendida do M26, "todas as vendas e valores precisam ser mapeados no DRE", não um detalhe de
 * compatibilidade a preservar). */
public record CriarLancamentoRequest(
        @NotNull TipoLancamento tipo,
        @NotNull UUID categoriaId,
        @NotBlank String descricao,
        @NotNull @DecimalMin(value = "0.01", message = "Valor deve ser positivo") BigDecimal valor,
        @NotNull LocalDate dataCompetencia,
        @NotNull StatusLancamento status,
        Plano planoReferencia,
        UUID eventoId,
        LocalDate dataVencimento
) {
    public CriarLancamentoRequest(TipoLancamento tipo, UUID categoriaId, String descricao, BigDecimal valor,
                                   LocalDate dataCompetencia, StatusLancamento status, Plano planoReferencia) {
        this(tipo, categoriaId, descricao, valor, dataCompetencia, status, planoReferencia, null, null);
    }
}
