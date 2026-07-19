package com.sawhub.hub.comercial.dto;

import com.sawhub.hub.comercial.FormaPagamento;
import com.sawhub.hub.comercial.Lead;
import com.sawhub.hub.comercial.OrigemVenda;
import com.sawhub.hub.comercial.ProdutoVenda;
import com.sawhub.hub.comercial.StatusLead;
import com.sawhub.hub.mentorado.Plano;
import com.sawhub.hub.mentorado.TipoContrato;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// M25 (change request pós-MVP, 17/07/2026) — campos de venda aditivos (produtoVenda..
// formaPagamento), em paralelo a planoFechado/tipoContratoFechado (M23). tipoContratoFechado
// também entra aqui — gap achado nesta leva: já existia na entidade desde o M23, mas nunca tinha
// sido exposto no DTO.
public record LeadResponse(
        UUID id,
        String nome,
        String email,
        String telefone,
        String mensagem,
        Plano planoInteresse,
        StatusLead status,
        VendedorResumo vendedor,
        Plano planoFechado,
        TipoContrato tipoContratoFechado,
        String motivoPerdido,
        Instant dataFechamento,
        Instant criadoEm,
        ProdutoVenda produtoVenda,
        OrigemVenda origemVenda,
        BigDecimal valorTotalVenda,
        BigDecimal valorPagoNoAto,
        FormaPagamento formaPagamento,
        BigDecimal taxaPlataformaRetida
) {
    public static LeadResponse from(Lead l) {
        return new LeadResponse(l.getId(), l.getNome(), l.getEmail(), l.getTelefone(), l.getMensagem(),
                l.getPlanoInteresse(), l.getStatus(), VendedorResumo.from(l.getVendedor()), l.getPlanoFechado(),
                l.getTipoContratoFechado(), l.getMotivoPerdido(), l.getDataFechamento(), l.getCriadoEm(),
                l.getProdutoVenda(), l.getOrigemVenda(), l.getValorTotalVenda(), l.getValorPagoNoAto(),
                l.getFormaPagamento(), l.getTaxaPlataformaRetida());
    }
}
