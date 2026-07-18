package com.sawhub.hub.comercial;

import com.sawhub.hub.comercial.dto.RankingItem;
import com.sawhub.hub.comercial.dto.VendedorResumo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/** H13.3 — meta x realizado e ranking do time comercial no período. */
@Service
public class RankingComercialService {

    private final MetaComercialRepository metaComercialRepository;
    private final LeadRepository leadRepository;

    public RankingComercialService(MetaComercialRepository metaComercialRepository, LeadRepository leadRepository) {
        this.metaComercialRepository = metaComercialRepository;
        this.leadRepository = leadRepository;
    }

    public List<RankingItem> ranking(int ano, int mes) {
        YearMonth periodo = YearMonth.of(ano, mes);
        Instant inicio = periodo.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant fim = periodo.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        return metaComercialRepository.buscarComVendedorPorPeriodo(ano, mes).stream()
                .map(meta -> {
                    // M25 (Suposição 7) — realizado exclui venda de ingresso de evento: comissão
                    // de fechamento de mentoria/consultoria é uma coisa diferente de vender
                    // ingresso, não faz sentido pesar igual na meta do vendedor.
                    long realizado = leadRepository.countByVendedorIdAndStatusAndDataFechamentoBetweenExcluindoProduto(
                            meta.getVendedor().getId(), StatusLead.FECHADO, inicio, fim, ProdutoVenda.INGRESSO_EVENTO);
                    double pctAtingido = meta.getMetaFechamentos() == 0 ? 0.0
                            : BigDecimal.valueOf(realizado)
                                    .divide(BigDecimal.valueOf(meta.getMetaFechamentos()), 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100))
                                    .doubleValue();
                    return new RankingItem(VendedorResumo.from(meta.getVendedor()), meta.getMetaFechamentos(), realizado, pctAtingido);
                })
                .sorted(Comparator.comparingLong(RankingItem::realizado).reversed())
                .toList();
    }
}
