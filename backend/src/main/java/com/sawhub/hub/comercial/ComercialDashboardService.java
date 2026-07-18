package com.sawhub.hub.comercial;

import com.sawhub.hub.comercial.dto.DashboardComercialResponse;
import com.sawhub.hub.comercial.dto.FunilItem;
import com.sawhub.hub.comercial.dto.VendaIngressoResumo;
import com.sawhub.hub.common.VariacaoCalculator;
import com.sawhub.hub.evento.Evento;
import com.sawhub.hub.evento.EventoRepository;
import com.sawhub.hub.evento.StatusEvento;
import com.sawhub.hub.financeiro.OrigemReceita;
import com.sawhub.hub.financeiro.RelatorioFinanceiroService;
import com.sawhub.hub.financeiro.dto.DashboardFaturamentoResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** H13.1 — dashboard comercial. MRR e vendas da loja não são dado próprio: lêem direto do
 * Financeiro (mesmo padrão de {@code consolidated/} sobre {@code mentorado/}), não duplicam a
 * agregação que {@link RelatorioFinanceiroService} já faz. */
@Service
public class ComercialDashboardService {

    private final LeadRepository leadRepository;
    private final RelatorioFinanceiroService relatorioFinanceiroService;
    private final EventoRepository eventoRepository;
    private final VendaIngressoRepository vendaIngressoRepository;

    public ComercialDashboardService(LeadRepository leadRepository, RelatorioFinanceiroService relatorioFinanceiroService,
                                      EventoRepository eventoRepository, VendaIngressoRepository vendaIngressoRepository) {
        this.leadRepository = leadRepository;
        this.relatorioFinanceiroService = relatorioFinanceiroService;
        this.eventoRepository = eventoRepository;
        this.vendaIngressoRepository = vendaIngressoRepository;
    }

    public DashboardComercialResponse dashboard(int ano, int mes) {
        YearMonth periodo = YearMonth.of(ano, mes);
        Instant inicio = periodo.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant fim = periodo.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        long fechados = leadRepository.countByStatusAndDataFechamentoBetween(StatusLead.FECHADO, inicio, fim);
        long perdidos = leadRepository.countByStatusAndDataFechamentoBetween(StatusLead.PERDIDO, inicio, fim);
        double taxaConversaoPct = (fechados + perdidos) == 0 ? 0.0
                : BigDecimal.valueOf(fechados)
                        .divide(BigDecimal.valueOf(fechados + perdidos), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();

        // M25 (Suposição 7) — "novos mentorados no mês" exclui venda de ingresso de evento
        // (contabilizada à parte em vendaIngressos, ver abaixo); a taxa de conversão acima
        // continua contando qualquer FECHADO, ingresso incluso — é conversão de verdade, só não
        // é um "novo mentorado".
        long novosMentoradosNoMes = leadRepository.countByStatusAndDataFechamentoBetweenExcluindoProduto(
                StatusLead.FECHADO, inicio, fim, ProdutoVenda.INGRESSO_EVENTO);

        List<FunilItem> funil = Arrays.stream(StatusLead.values())
                .map(status -> new FunilItem(status, leadRepository.countByStatus(status)))
                .toList();

        DashboardFaturamentoResponse atual = relatorioFinanceiroService.dashboardFaturamento(ano, mes);
        YearMonth anterior = periodo.minusMonths(1);
        DashboardFaturamentoResponse doMesAnterior = relatorioFinanceiroService.dashboardFaturamento(
                anterior.getYear(), anterior.getMonthValue());

        BigDecimal vendasLoja = vendasPorOrigem(atual, OrigemReceita.LOJA);
        double variacaoMrrPct = VariacaoCalculator.pct(doMesAnterior.mrr(), atual.mrr());

        List<VendaIngressoResumo> vendaIngressos = eventoRepository
                .buscarPorStatusEDataHoraBetween(StatusEvento.REALIZADO, inicio, fim).stream()
                .map(this::resumoVendaIngresso)
                .toList();

        return new DashboardComercialResponse(novosMentoradosNoMes, taxaConversaoPct, atual.mrr(), vendasLoja,
                variacaoMrrPct, funil, vendaIngressos);
    }

    // Uma linha por ingresso (VendaIngresso), mas o valor de venda vive no Lead (o total da venda,
    // não por ticket) — deduplica por Lead antes de somar, senão uma venda de 2 ingressos do
    // mesmo Lead contaria o valorTotalVenda em dobro.
    private VendaIngressoResumo resumoVendaIngresso(Evento evento) {
        List<VendaIngresso> vendas = vendaIngressoRepository.buscarPorEventoIdComLead(evento.getId());
        Map<UUID, Lead> leadsUnicos = new LinkedHashMap<>();
        for (VendaIngresso venda : vendas) {
            leadsUnicos.put(venda.getLead().getId(), venda.getLead());
        }
        BigDecimal valorLiquido = leadsUnicos.values().stream()
                .map(Lead::getValorTotalVenda)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new VendaIngressoResumo(evento.getId(), evento.getTitulo(), vendas.size(), evento.getVagas(), valorLiquido);
    }

    private static BigDecimal vendasPorOrigem(DashboardFaturamentoResponse faturamento, OrigemReceita origem) {
        return faturamento.composicao().stream()
                .filter(c -> c.origem() == origem)
                .map(c -> c.valor())
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

}
