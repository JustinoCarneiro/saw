package com.sawhub.hub.dashboardadmin;

import com.sawhub.hub.atividade.AtividadeLogService;
import com.sawhub.hub.comercial.ComercialDashboardService;
import com.sawhub.hub.comercial.ConciliacaoService;
import com.sawhub.hub.comercial.StatusLead;
import com.sawhub.hub.comercial.dto.ConciliacaoVendaResponse;
import com.sawhub.hub.comercial.dto.DashboardComercialResponse;
import com.sawhub.hub.common.VariacaoCalculator;
import com.sawhub.hub.conteudo.Conteudo;
import com.sawhub.hub.conteudo.ConteudoRepository;
import com.sawhub.hub.dashboardadmin.dto.DashboardAdminResponse;
import com.sawhub.hub.dashboardadmin.dto.DashboardAdminResponse.AtividadeRecente;
import com.sawhub.hub.dashboardadmin.dto.DashboardAdminResponse.CrescimentoMesItem;
import com.sawhub.hub.dashboardadmin.dto.DashboardAdminResponse.DistribuicaoTipoContratoItem;
import com.sawhub.hub.dashboardadmin.dto.DashboardAdminResponse.MentoriaHojeItem;
import com.sawhub.hub.evento.Evento;
import com.sawhub.hub.evento.EventoRepository;
import com.sawhub.hub.evento.StatusEvento;
import com.sawhub.hub.financeiro.RelatorioFinanceiroService;
import com.sawhub.hub.financeiro.dto.DashboardFaturamentoResponse;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.mentorado.StatusMentorado;
import com.sawhub.hub.mentorado.TipoContrato;
import com.sawhub.hub.mentoria.Mentoria;
import com.sawhub.hub.mentoria.MentoriaRepository;
import com.sawhub.hub.mentoria.StatusMentoria;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

/** H10.1–H10.3 — agregação pura de leitura, sem entidade própria (mesmo padrão de
 * {@code consolidated/} e do dashboard comercial). Ver Blueprint M16 no ROADMAP.md pras
 * Suposições sobre timestamps de criação como proxy de crescimento/atividade. */
@Service
public class DashboardAdminService {

    // Brasil-only (CLAUDE.md) — explícito em vez de depender do timezone padrão da JVM do
    // servidor (Coolify/VPS pode rodar em UTC).
    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private static final int MESES_CRESCIMENTO = 6;
    private static final int MAX_ATIVIDADES_RECENTES = 8;

    private final MentoradoRepository mentoradoRepository;
    private final MentoriaRepository mentoriaRepository;
    private final EventoRepository eventoRepository;
    private final ConteudoRepository conteudoRepository;
    private final RelatorioFinanceiroService relatorioFinanceiroService;
    private final AtividadeLogService atividadeLogService;
    private final ComercialDashboardService comercialDashboardService;
    private final ConciliacaoService conciliacaoService;

    public DashboardAdminService(MentoradoRepository mentoradoRepository, MentoriaRepository mentoriaRepository,
                                  EventoRepository eventoRepository, ConteudoRepository conteudoRepository,
                                  RelatorioFinanceiroService relatorioFinanceiroService,
                                  AtividadeLogService atividadeLogService,
                                  ComercialDashboardService comercialDashboardService,
                                  ConciliacaoService conciliacaoService) {
        this.mentoradoRepository = mentoradoRepository;
        this.mentoriaRepository = mentoriaRepository;
        this.eventoRepository = eventoRepository;
        this.conteudoRepository = conteudoRepository;
        this.relatorioFinanceiroService = relatorioFinanceiroService;
        this.atividadeLogService = atividadeLogService;
        this.comercialDashboardService = comercialDashboardService;
        this.conciliacaoService = conciliacaoService;
    }

    public DashboardAdminResponse resumo(int ano, int mes) {
        YearMonth periodo = YearMonth.of(ano, mes);
        YearMonth anterior = periodo.minusMonths(1);

        List<Mentorado> mentorados = mentoradoRepository.buscarComFiltro(null, null);
        List<Mentoria> mentorias = mentoriaRepository.buscarPorStatus(null);
        List<Evento> eventos = eventoRepository.buscarComFiltro(null, null);

        long mentoradosAtivos = mentorados.stream().filter(m -> m.getStatus() == StatusMentorado.ATIVO).count();
        // Suposição 2 (Blueprint M16): sem histórico de ativar/desativar, "ativos no mês anterior"
        // é aproximado pelo total cadastrado até o fim daquele mês.
        long cadastradosAteMesAnterior = contarCadastradosAte(mentorados, anterior.atEndOfMonth());
        double variacaoMentoradosAtivosPct = VariacaoCalculator.pct(cadastradosAteMesAnterior, mentoradosAtivos);

        long mentoriasRealizadas = contarMentoriasPorStatusEMes(mentorias, StatusMentoria.REALIZADA, periodo);
        long mentoriasRealizadasAnterior = contarMentoriasPorStatusEMes(mentorias, StatusMentoria.REALIZADA, anterior);
        double variacaoMentoriasPct = VariacaoCalculator.pct(mentoriasRealizadasAnterior, mentoriasRealizadas);

        long eventosRealizados = contarEventosPorStatusEMes(eventos, StatusEvento.REALIZADO, periodo);
        long eventosRealizadosAnterior = contarEventosPorStatusEMes(eventos, StatusEvento.REALIZADO, anterior);
        double variacaoEventosPct = VariacaoCalculator.pct(eventosRealizadosAnterior, eventosRealizados);

        DashboardFaturamentoResponse faturamentoAtual = relatorioFinanceiroService.dashboardFaturamento(ano, mes);
        DashboardFaturamentoResponse faturamentoAnterior = relatorioFinanceiroService.dashboardFaturamento(
                anterior.getYear(), anterior.getMonthValue());
        double variacaoReceitaPct = VariacaoCalculator.pct(
                faturamentoAnterior.faturamentoMensal(), faturamentoAtual.faturamentoMensal());

        // Pedido do Marcos (22/07/2026) — "resumo" clicável de Comercial/Conciliação, mesmo padrão
        // de composição já usado em ComercialDashboardService (não duplica a agregação, só lê).
        DashboardComercialResponse comercialAtual = comercialDashboardService.dashboard(ano, mes);
        long leadsEmAberto = comercialAtual.funil().stream()
                .filter(f -> f.status() != StatusLead.FECHADO && f.status() != StatusLead.PERDIDO)
                .mapToLong(f -> f.quantidade())
                .sum();
        long vendasEmAtraso = conciliacaoService.listar().stream()
                .filter(ConciliacaoVendaResponse::emAtraso)
                .count();

        return new DashboardAdminResponse(
                mentoradosAtivos, variacaoMentoradosAtivosPct,
                mentoriasRealizadas, variacaoMentoriasPct,
                eventosRealizados, variacaoEventosPct,
                faturamentoAtual.faturamentoMensal(), variacaoReceitaPct,
                crescimentoUltimosMeses(mentorados, periodo),
                distribuicaoPorTipoContrato(mentorados),
                atividadesRecentes(mentorados, eventos),
                mentoriasDeHoje(mentorias),
                faturamentoAtual.resultadoDre(), faturamentoAtual.saldoCaixaAtual(),
                faturamentoAtual.lancamentosPendentes(), faturamentoAtual.lancamentosVencidos(),
                leadsEmAberto, comercialAtual.taxaConversaoPct(), vendasEmAtraso);
    }

    private static long contarCadastradosAte(List<Mentorado> mentorados, LocalDate fimDoMes) {
        Instant corte = fimDoMes.plusDays(1).atStartOfDay(ZONA).toInstant();
        return mentorados.stream().filter(m -> m.getCriadoEm().isBefore(corte)).count();
    }

    private static long contarMentoriasPorStatusEMes(List<Mentoria> mentorias, StatusMentoria status, YearMonth mes) {
        return mentorias.stream()
                .filter(m -> m.getStatus() == status)
                .filter(m -> noMes(m.getDataHora(), mes))
                .count();
    }

    private static long contarEventosPorStatusEMes(List<Evento> eventos, StatusEvento status, YearMonth mes) {
        return eventos.stream()
                .filter(e -> e.getStatus() == status)
                .filter(e -> noMes(e.getDataHora(), mes))
                .count();
    }

    private static boolean noMes(Instant instante, YearMonth mes) {
        YearMonth doInstante = YearMonth.from(instante.atZone(ZONA));
        return doInstante.equals(mes);
    }

    private static List<CrescimentoMesItem> crescimentoUltimosMeses(List<Mentorado> mentorados, YearMonth periodo) {
        return java.util.stream.IntStream.rangeClosed(0, MESES_CRESCIMENTO - 1)
                .mapToObj(i -> periodo.minusMonths((long) MESES_CRESCIMENTO - 1 - i))
                .map(mes -> new CrescimentoMesItem(mes.toString(), contarCadastradosAte(mentorados, mes.atEndOfMonth())))
                .toList();
    }

    // Inclui um bucket extra pra tipoContrato == null ("Não informado") — nem todo mentorado tem
    // um tipo de contrato de mentoria (ver DistribuicaoTipoContratoItem), então esconder esse
    // caso faria a soma dos percentuais não bater 100%.
    private static List<DistribuicaoTipoContratoItem> distribuicaoPorTipoContrato(List<Mentorado> mentorados) {
        List<Mentorado> ativos = mentorados.stream().filter(m -> m.getStatus() == StatusMentorado.ATIVO).toList();
        long total = ativos.size();
        List<DistribuicaoTipoContratoItem> itens = Arrays.stream(TipoContrato.values())
                .map(tipo -> {
                    long qtd = ativos.stream().filter(m -> m.getTipoContrato() == tipo).count();
                    double pct = total == 0 ? 0.0 : (qtd * 10000L / total) / 100.0;
                    return new DistribuicaoTipoContratoItem(tipo, qtd, pct);
                })
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        long semInfo = ativos.stream().filter(m -> m.getTipoContrato() == null).count();
        double pctSemInfo = total == 0 ? 0.0 : (semInfo * 10000L / total) / 100.0;
        itens.add(new DistribuicaoTipoContratoItem(null, semInfo, pctSemInfo));
        return itens;
    }

    // H10 — cadastro/evento/conteúdo derivam do criadoEm da própria entidade (têm timestamp real
    // de sobra); marcos de TRANSIÇÃO de status (mentoria cancelada/realizada, evento cancelado,
    // pedido pago/reembolsado, lead fechado/perdido) vêm do AtividadeLog, escrito nos pontos de
    // transição que contam como marco (ver AtividadeLogService e Achado 1 desta leva — antes
    // dessas transições não tinham NENHUM timestamp pra ordenar/exibir).
    private List<AtividadeRecente> atividadesRecentes(List<Mentorado> mentorados, List<Evento> eventos) {
        List<Conteudo> conteudosPublicados = conteudoRepository.buscarComFiltro(null, true);

        Stream<AtividadeRecente> deMentorados = mentorados.stream()
                .map(m -> new AtividadeRecente("MENTORADO_CADASTRADO", "Novo mentorado: " + m.getNome(), m.getCriadoEm()));
        Stream<AtividadeRecente> deEventos = eventos.stream()
                .map(e -> new AtividadeRecente("EVENTO_CRIADO", "Novo evento: " + e.getTitulo(), e.getCriadoEm()));
        Stream<AtividadeRecente> deConteudos = conteudosPublicados.stream()
                .map(c -> new AtividadeRecente("CONTEUDO_PUBLICADO", "Conteúdo publicado: " + c.getTitulo(), c.getCriadoEm()));
        Stream<AtividadeRecente> deLog = atividadeLogService.listarRecentes().stream()
                .map(a -> new AtividadeRecente(a.getTipo(), a.getDescricao(), a.getCriadoEm()));

        return Stream.of(deMentorados, deEventos, deConteudos, deLog)
                .flatMap(s -> s)
                .sorted(Comparator.comparing(AtividadeRecente::quando).reversed())
                .limit(MAX_ATIVIDADES_RECENTES)
                .toList();
    }

    private static List<MentoriaHojeItem> mentoriasDeHoje(List<Mentoria> mentorias) {
        LocalDate hoje = LocalDate.now(ZONA);
        DateTimeFormatter horaFormato = DateTimeFormatter.ofPattern("HH:mm");
        return mentorias.stream()
                .filter(m -> m.getDataHora().atZone(ZONA).toLocalDate().equals(hoje))
                .filter(m -> m.getStatus() == StatusMentoria.AGENDADA || m.getStatus() == StatusMentoria.CONFIRMADA)
                .sorted(Comparator.comparing(Mentoria::getDataHora))
                .map(m -> new MentoriaHojeItem(
                        m.getTipo(),
                        m.getMentor() != null ? m.getMentor().getNome() : null,
                        m.getMentorados().stream().map(Mentorado::getNome).sorted().reduce((a, b) -> a + ", " + b).orElse(""),
                        horaFormato.withZone(ZONA).format(m.getDataHora()),
                        m.getStatus()))
                .toList();
    }
}
