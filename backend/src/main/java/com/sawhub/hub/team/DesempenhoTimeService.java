package com.sawhub.hub.team;

import com.sawhub.hub.comercial.LeadRepository;
import com.sawhub.hub.comercial.MetaComercial;
import com.sawhub.hub.comercial.MetaComercialRepository;
import com.sawhub.hub.comercial.StatusLead;
import com.sawhub.hub.mentoria.MentoriaRepository;
import com.sawhub.hub.mentoria.StatusMentoria;
import com.sawhub.hub.team.dto.DesempenhoColaboradorResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** H15.7 — desempenho do time (mentorias realizadas por todo mundo + meta x realizado de
 * fechamentos pra quem já tem MetaComercial no período), mesmo padrão de período de
 * RankingComercialService. Sem meta de mentorias: ver Suposição 3 no Blueprint (ROADMAP.md). */
@Service
public class DesempenhoTimeService {

    private final ColaboradorRepository colaboradorRepository;
    private final MentoriaRepository mentoriaRepository;
    private final MetaComercialRepository metaComercialRepository;
    private final LeadRepository leadRepository;

    public DesempenhoTimeService(ColaboradorRepository colaboradorRepository, MentoriaRepository mentoriaRepository,
                                  MetaComercialRepository metaComercialRepository, LeadRepository leadRepository) {
        this.colaboradorRepository = colaboradorRepository;
        this.mentoriaRepository = mentoriaRepository;
        this.metaComercialRepository = metaComercialRepository;
        this.leadRepository = leadRepository;
    }

    public List<DesempenhoColaboradorResponse> desempenho(int ano, int mes) {
        YearMonth periodo = YearMonth.of(ano, mes);
        Instant inicio = periodo.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant fim = periodo.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        Map<UUID, MetaComercial> metasPorVendedor = metaComercialRepository
                .buscarComVendedorPorPeriodo(ano, mes).stream()
                .collect(Collectors.toMap(m -> m.getVendedor().getId(), Function.identity()));

        return colaboradorRepository.findAllByOrderByNomeAsc().stream()
                .map(colaborador -> {
                    long mentoriasRealizadas = mentoriaRepository.buscarPorMentor(colaborador).stream()
                            .filter(m -> m.getStatus() == StatusMentoria.REALIZADA)
                            .filter(m -> !m.getDataHora().isBefore(inicio) && m.getDataHora().isBefore(fim))
                            .count();

                    MetaComercial meta = metasPorVendedor.get(colaborador.getId());
                    if (meta == null) {
                        return DesempenhoColaboradorResponse.semMeta(colaborador, mentoriasRealizadas);
                    }

                    long fechamentosRealizados = leadRepository.countByVendedorIdAndStatusAndDataFechamentoBetween(
                            colaborador.getId(), StatusLead.FECHADO, inicio, fim);
                    double pctAtingido = meta.getMetaFechamentos() == 0 ? 0.0
                            : BigDecimal.valueOf(fechamentosRealizados)
                                    .divide(BigDecimal.valueOf(meta.getMetaFechamentos()), 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100))
                                    .doubleValue();
                    return DesempenhoColaboradorResponse.comMeta(colaborador, mentoriasRealizadas,
                            meta.getMetaFechamentos(), fechamentosRealizados, pctAtingido);
                })
                .toList();
    }
}
