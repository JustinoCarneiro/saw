package com.sawhub.hub.dashboardadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sawhub.hub.atividade.AtividadeLog;
import com.sawhub.hub.atividade.AtividadeLogService;
import com.sawhub.hub.comercial.ComercialDashboardService;
import com.sawhub.hub.comercial.ConciliacaoService;
import com.sawhub.hub.comercial.StatusLead;
import com.sawhub.hub.comercial.dto.ConciliacaoVendaResponse;
import com.sawhub.hub.comercial.dto.DashboardComercialResponse;
import com.sawhub.hub.comercial.dto.FunilItem;
import com.sawhub.hub.conteudo.ConteudoRepository;
import com.sawhub.hub.dashboardadmin.dto.DashboardAdminResponse;
import com.sawhub.hub.evento.Evento;
import com.sawhub.hub.evento.EventoRepository;
import com.sawhub.hub.evento.TipoEvento;
import com.sawhub.hub.financeiro.RelatorioFinanceiroService;
import com.sawhub.hub.financeiro.dto.DashboardFaturamentoResponse;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.mentorado.TipoContrato;
import com.sawhub.hub.mentoria.Mentoria;
import com.sawhub.hub.mentoria.MentoriaRepository;
import com.sawhub.hub.mentoria.StatusMentoria;
import com.sawhub.hub.mentoria.TipoMentoria;
import com.sawhub.hub.security.Perfil;
import com.sawhub.hub.security.Usuario;
import com.sawhub.hub.team.Area;
import com.sawhub.hub.team.Colaborador;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** H10.1–H10.3 — RED primeiro: DashboardAdminService ainda não existia neste ponto do ciclo. */
@ExtendWith(MockitoExtension.class)
class DashboardAdminServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");

    @Mock
    private MentoradoRepository mentoradoRepository;
    @Mock
    private MentoriaRepository mentoriaRepository;
    @Mock
    private EventoRepository eventoRepository;
    @Mock
    private ConteudoRepository conteudoRepository;
    @Mock
    private RelatorioFinanceiroService relatorioFinanceiroService;
    @Mock
    private AtividadeLogService atividadeLogService;
    @Mock
    private ComercialDashboardService comercialDashboardService;
    @Mock
    private ConciliacaoService conciliacaoService;

    private DashboardAdminService service() {
        return new DashboardAdminService(mentoradoRepository, mentoriaRepository, eventoRepository,
                conteudoRepository, relatorioFinanceiroService, atividadeLogService,
                comercialDashboardService, conciliacaoService);
    }

    private static Mentorado mentoradoEm(String nome, Instant criadoEm, boolean ativo) {
        Usuario usuario = new Usuario(nome.toLowerCase().replace(" ", "") + "@x.com", "hash", Perfil.MENTORADO);
        Mentorado m = new Mentorado(usuario, nome, "Negócio", BigDecimal.ZERO, 0, 0);
        if (!ativo) {
            m.desativar();
        }
        ReflectionTestUtils.setField(m, "criadoEm", criadoEm);
        return m;
    }

    private static Instant emMes(YearMonth mes, int dia) {
        return mes.atDay(dia).atStartOfDay(ZONA).toInstant();
    }

    private void mockarVazio(YearMonth atual, YearMonth anterior) {
        when(mentoradoRepository.buscarComFiltro(null, null)).thenReturn(List.of());
        when(mentoriaRepository.buscarPorStatus(null)).thenReturn(List.of());
        when(eventoRepository.buscarComFiltro(null, null)).thenReturn(List.of());
        when(conteudoRepository.buscarComFiltro(null, true)).thenReturn(List.of());
        when(atividadeLogService.listarRecentes()).thenReturn(List.of());
        var faturamento = new DashboardFaturamentoResponse(BigDecimal.ZERO, BigDecimal.ZERO, 0.0, List.of(),
                BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L, List.of());
        when(relatorioFinanceiroService.dashboardFaturamento(atual.getYear(), atual.getMonthValue())).thenReturn(faturamento);
        when(relatorioFinanceiroService.dashboardFaturamento(anterior.getYear(), anterior.getMonthValue())).thenReturn(faturamento);
        var comercial = new DashboardComercialResponse(0L, 0.0, BigDecimal.ZERO, BigDecimal.ZERO, 0.0,
                List.of(), List.of(), List.of());
        when(comercialDashboardService.dashboard(atual.getYear(), atual.getMonthValue())).thenReturn(comercial);
        when(conciliacaoService.listar()).thenReturn(List.of());
    }

    @Test
    void mentoradosAtivosContaSoAtivosEIgnoraInativos() {
        YearMonth atual = YearMonth.of(2026, 7);
        YearMonth anterior = atual.minusMonths(1);
        mockarVazio(atual, anterior);

        Mentorado ativo1 = mentoradoEm("Ana", emMes(anterior, 1), true);
        Mentorado ativo2 = mentoradoEm("Rafael", emMes(anterior, 1), true);
        Mentorado inativo = mentoradoEm("Carlos", emMes(anterior, 1), false);
        when(mentoradoRepository.buscarComFiltro(null, null)).thenReturn(List.of(ativo1, ativo2, inativo));

        DashboardAdminResponse resposta = service().resumo(atual.getYear(), atual.getMonthValue());

        assertThat(resposta.mentoradosAtivos()).isEqualTo(2);
    }

    @Test
    void distribuicaoPorTipoContratoSomaCemPorCentoEIgnoraInativos() {
        YearMonth atual = YearMonth.of(2026, 7);
        YearMonth anterior = atual.minusMonths(1);
        mockarVazio(atual, anterior);

        Mentorado a = mentoradoEm("A", emMes(anterior, 1), true);
        Mentorado b = mentoradoEm("B", emMes(anterior, 1), true);
        Mentorado c = mentoradoEm("C", emMes(anterior, 1), true);
        Mentorado d = mentoradoEm("D", emMes(anterior, 1), false); // inativo, fora do cálculo
        ReflectionTestUtils.setField(a, "tipoContrato", TipoContrato.MENTORIA_CONTINUA);
        ReflectionTestUtils.setField(b, "tipoContrato", TipoContrato.MENTORIA_CONTINUA);
        ReflectionTestUtils.setField(c, "tipoContrato", TipoContrato.CONSULTORIA);
        // d fica sem tipoContrato de propósito — mas está inativo, então nem entraria no bucket
        // "Não informado" (ver assertThat(semInformado) abaixo).
        List<Mentorado> mentorados = List.of(a, b, c, d);
        when(mentoradoRepository.buscarComFiltro(null, null)).thenReturn(mentorados);

        DashboardAdminResponse resposta = service().resumo(atual.getYear(), atual.getMonthValue());

        // Tolerância pequena: divisão inteira truncada não fecha em exatos 100.0 — mesmo
        // artefato de arredondamento de qualquer gráfico de distribuição percentual.
        double somaPct = resposta.distribuicaoTipoContrato().stream()
                .mapToDouble(DashboardAdminResponse.DistribuicaoTipoContratoItem::pct).sum();
        assertThat(somaPct).isCloseTo(100.0, org.assertj.core.data.Offset.offset(0.1));
        var continua = resposta.distribuicaoTipoContrato().stream()
                .filter(i -> i.tipoContrato() == TipoContrato.MENTORIA_CONTINUA).findFirst().orElseThrow();
        assertThat(continua.quantidade()).isEqualTo(2);
        var semInformado = resposta.distribuicaoTipoContrato().stream()
                .filter(i -> i.tipoContrato() == null).findFirst().orElseThrow();
        assertThat(semInformado.quantidade()).isEqualTo(0);
    }

    @Test
    void crescimentoMentoradosTemSeisMesesEmOrdemCronologica() {
        YearMonth atual = YearMonth.of(2026, 7);
        YearMonth anterior = atual.minusMonths(1);
        mockarVazio(atual, anterior);

        DashboardAdminResponse resposta = service().resumo(atual.getYear(), atual.getMonthValue());

        assertThat(resposta.crescimentoMentorados()).hasSize(6);
        assertThat(resposta.crescimentoMentorados().get(0).mes()).isEqualTo("2026-02");
        assertThat(resposta.crescimentoMentorados().get(5).mes()).isEqualTo("2026-07");
    }

    @Test
    void mentoriasEEventosRealizadosContamSoOMesCorrente() {
        YearMonth atual = YearMonth.of(2026, 7);
        YearMonth anterior = atual.minusMonths(1);
        mockarVazio(atual, anterior);

        Colaborador mentor = new Colaborador(null, "Brayan", Area.GESTAO_PERFORMANCE);
        Mentorado mentorado = mentoradoEm("Ana", emMes(anterior, 1), true);

        Mentoria realizadaEsteMes = new Mentoria(TipoMentoria.INDIVIDUAL, mentor, Set.of(mentorado), emMes(atual, 10), 60, null, null);
        realizadaEsteMes.confirmar();
        realizadaEsteMes.realizar();
        Mentoria realizadaMesPassado = new Mentoria(TipoMentoria.INDIVIDUAL, mentor, Set.of(mentorado), emMes(anterior, 10), 60, null, null);
        realizadaMesPassado.confirmar();
        realizadaMesPassado.realizar();
        Mentoria agendadaEsteMes = new Mentoria(TipoMentoria.INDIVIDUAL, mentor, Set.of(mentorado), emMes(atual, 15), 60, null, null);
        when(mentoriaRepository.buscarPorStatus(null)).thenReturn(List.of(realizadaEsteMes, realizadaMesPassado, agendadaEsteMes));

        Evento realizadoEsteMes = new Evento("Workshop", TipoEvento.AO_VIVO, "Tema", emMes(atual, 5), null, null, 50);
        realizadoEsteMes.iniciar();
        realizadoEsteMes.finalizar();
        when(eventoRepository.buscarComFiltro(null, null)).thenReturn(List.of(realizadoEsteMes));

        DashboardAdminResponse resposta = service().resumo(atual.getYear(), atual.getMonthValue());

        assertThat(resposta.mentoriasRealizadas()).isEqualTo(1);
        assertThat(resposta.eventosRealizados()).isEqualTo(1);
    }

    @Test
    void mentoriasDeHojeFiltraPorDataEStatusAgendadaOuConfirmada() {
        YearMonth atual = YearMonth.of(2026, 7);
        YearMonth anterior = atual.minusMonths(1);
        mockarVazio(atual, anterior);

        Colaborador mentor = new Colaborador(null, "Brayan", Area.GESTAO_PERFORMANCE);
        Mentorado mentorado = mentoradoEm("Ana", emMes(anterior, 1), true);
        Instant hojeAs10 = LocalDate.now(ZONA).atTime(10, 0).atZone(ZONA).toInstant();
        Instant amanha = LocalDate.now(ZONA).plusDays(1).atTime(10, 0).atZone(ZONA).toInstant();

        Mentoria hojeConfirmada = new Mentoria(TipoMentoria.INDIVIDUAL, mentor, Set.of(mentorado), hojeAs10, 60, null, null);
        hojeConfirmada.confirmar();
        Mentoria hojeCancelada = new Mentoria(TipoMentoria.INDIVIDUAL, mentor, Set.of(mentorado), hojeAs10, 60, null, null);
        hojeCancelada.cancelar();
        Mentoria amanhaConfirmada = new Mentoria(TipoMentoria.INDIVIDUAL, mentor, Set.of(mentorado), amanha, 60, null, null);
        amanhaConfirmada.confirmar();
        when(mentoriaRepository.buscarPorStatus(null)).thenReturn(List.of(hojeConfirmada, hojeCancelada, amanhaConfirmada));

        DashboardAdminResponse resposta = service().resumo(atual.getYear(), atual.getMonthValue());

        assertThat(resposta.mentoriasHoje()).hasSize(1);
        assertThat(resposta.mentoriasHoje().get(0).status()).isEqualTo(StatusMentoria.CONFIRMADA);
        assertThat(resposta.mentoriasHoje().get(0).mentoradoNomes()).isEqualTo("Ana");
    }

    @Test
    void atividadesRecentesOrdenaPorDataDescELimitaOito() {
        YearMonth atual = YearMonth.of(2026, 7);
        YearMonth anterior = atual.minusMonths(1);
        mockarVazio(atual, anterior);

        List<Mentorado> mentorados = List.of(
                mentoradoEm("Antigo", emMes(anterior, 1), true),
                mentoradoEm("Recente", emMes(atual, 5), true));
        when(mentoradoRepository.buscarComFiltro(null, null)).thenReturn(mentorados);

        DashboardAdminResponse resposta = service().resumo(atual.getYear(), atual.getMonthValue());

        assertThat(resposta.atividadesRecentes().get(0).descricao()).contains("Recente");
        assertThat(resposta.atividadesRecentes()).hasSizeLessThanOrEqualTo(8);
    }

    @Test
    void atividadesRecentesMisturaLogDeTransicaoDeStatusComOsTiposDeCriacao() {
        // H10 — cancelar/realizar/pagar/reembolsar/fechar/perder não têm criadoEm próprio (não
        // são criação de entidade), então entram via AtividadeLog, não pelos 3 streams originais.
        YearMonth atual = YearMonth.of(2026, 7);
        YearMonth anterior = atual.minusMonths(1);
        mockarVazio(atual, anterior);

        Mentorado mentorado = mentoradoEm("Ana", emMes(anterior, 1), true);
        when(mentoradoRepository.buscarComFiltro(null, null)).thenReturn(List.of(mentorado));

        AtividadeLog logRecente = new AtividadeLog("MENTORIA_CANCELADA", "Mentoria cancelada: Carlos Menezes");
        ReflectionTestUtils.setField(logRecente, "criadoEm", emMes(atual, 20));
        when(atividadeLogService.listarRecentes()).thenReturn(List.of(logRecente));

        DashboardAdminResponse resposta = service().resumo(atual.getYear(), atual.getMonthValue());

        assertThat(resposta.atividadesRecentes().get(0).tipo()).isEqualTo("MENTORIA_CANCELADA");
        assertThat(resposta.atividadesRecentes().get(0).descricao()).isEqualTo("Mentoria cancelada: Carlos Menezes");
    }

    // Pedido do Marcos (22/07/2026) — "o CEO abre só o Dashboard na maioria das vezes": resumo
    // clicável de Comercial/Financeiro/Caixa/Conciliação precisa refletir o que as próprias
    // telas mostram, não duplicar a agregação (só ler DashboardComercialResponse/
    // DashboardFaturamentoResponse/ConciliacaoService, mesmo padrão de composição já usado em
    // ComercialDashboardService pra ler o Financeiro).
    @Test
    void resumoTrazMetricasDeComercialFinanceiroECaixaClicaveisNoDashboard() {
        YearMonth atual = YearMonth.of(2026, 7);
        YearMonth anterior = atual.minusMonths(1);
        mockarVazio(atual, anterior);

        var faturamento = new DashboardFaturamentoResponse(BigDecimal.ZERO, BigDecimal.ZERO, 0.0, List.of(),
                new BigDecimal("5000.00"), new BigDecimal("12000.00"), 3L, 2L, List.of());
        when(relatorioFinanceiroService.dashboardFaturamento(atual.getYear(), atual.getMonthValue())).thenReturn(faturamento);

        var funil = List.of(
                new FunilItem(StatusLead.SOLICITACAO, 4L),
                new FunilItem(StatusLead.PROPOSTA, 2L),
                new FunilItem(StatusLead.FECHADO, 5L),
                new FunilItem(StatusLead.PERDIDO, 3L));
        var comercial = new DashboardComercialResponse(5L, 45.5, BigDecimal.ZERO, BigDecimal.ZERO, 0.0,
                funil, List.of(), List.of());
        when(comercialDashboardService.dashboard(atual.getYear(), atual.getMonthValue())).thenReturn(comercial);

        var vendaEmDia = new ConciliacaoVendaResponse(java.util.UUID.randomUUID(), "Cliente A",
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, 100.0, false, null, null);
        var vendaAtrasada = new ConciliacaoVendaResponse(java.util.UUID.randomUUID(), "Cliente B",
                BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, 0.0, true, 10, 1);
        when(conciliacaoService.listar()).thenReturn(List.of(vendaEmDia, vendaAtrasada));

        DashboardAdminResponse resposta = service().resumo(atual.getYear(), atual.getMonthValue());

        assertThat(resposta.resultadoDre()).isEqualByComparingTo("5000.00");
        assertThat(resposta.saldoCaixaAtual()).isEqualByComparingTo("12000.00");
        assertThat(resposta.lancamentosPendentes()).isEqualTo(3);
        assertThat(resposta.lancamentosVencidos()).isEqualTo(2);
        // Leads em aberto: soma tudo exceto FECHADO/PERDIDO (4 + 2 = 6).
        assertThat(resposta.leadsEmAberto()).isEqualTo(6);
        assertThat(resposta.taxaConversaoPct()).isEqualTo(45.5);
        assertThat(resposta.vendasEmAtraso()).isEqualTo(1);
    }
}
