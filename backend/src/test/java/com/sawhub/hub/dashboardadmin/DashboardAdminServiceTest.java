package com.sawhub.hub.dashboardadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sawhub.hub.conteudo.ConteudoRepository;
import com.sawhub.hub.dashboardadmin.dto.DashboardAdminResponse;
import com.sawhub.hub.evento.Evento;
import com.sawhub.hub.evento.EventoRepository;
import com.sawhub.hub.evento.TipoEvento;
import com.sawhub.hub.financeiro.RelatorioFinanceiroService;
import com.sawhub.hub.financeiro.dto.DashboardFaturamentoResponse;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.mentorado.Plano;
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

    private DashboardAdminService service() {
        return new DashboardAdminService(mentoradoRepository, mentoriaRepository, eventoRepository,
                conteudoRepository, relatorioFinanceiroService);
    }

    private static Mentorado mentoradoEm(String nome, Plano plano, Instant criadoEm, boolean ativo) {
        Usuario usuario = new Usuario(nome.toLowerCase().replace(" ", "") + "@x.com", "hash", Perfil.MENTORADO);
        Mentorado m = new Mentorado(usuario, nome, "Negócio", plano, BigDecimal.ZERO, 0, 0);
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
        when(mentoradoRepository.buscarComFiltro(null, null, null)).thenReturn(List.of());
        when(mentoriaRepository.buscarPorStatus(null)).thenReturn(List.of());
        when(eventoRepository.buscarComFiltro(null, null)).thenReturn(List.of());
        when(conteudoRepository.buscarComFiltro(null, null, true)).thenReturn(List.of());
        var faturamento = new DashboardFaturamentoResponse(BigDecimal.ZERO, BigDecimal.ZERO, 0.0, List.of());
        // historicoReceita (M23) chama dashboardFaturamento pra cada um dos 6 meses da janela
        // deslizante, não só atual/anterior — este loop cobre os dois (i=4 e i=5) também.
        for (int i = 0; i < 6; i++) {
            YearMonth mes = atual.minusMonths(5L - i);
            when(relatorioFinanceiroService.dashboardFaturamento(mes.getYear(), mes.getMonthValue())).thenReturn(faturamento);
        }
    }

    @Test
    void mentoradosAtivosContaSoAtivosEIgnoraInativos() {
        YearMonth atual = YearMonth.of(2026, 7);
        YearMonth anterior = atual.minusMonths(1);
        mockarVazio(atual, anterior);

        Mentorado ativo1 = mentoradoEm("Ana", Plano.ESSENCIAL, emMes(anterior, 1), true);
        Mentorado ativo2 = mentoradoEm("Rafael", Plano.BASICO, emMes(anterior, 1), true);
        Mentorado inativo = mentoradoEm("Carlos", Plano.BASICO, emMes(anterior, 1), false);
        when(mentoradoRepository.buscarComFiltro(null, null, null)).thenReturn(List.of(ativo1, ativo2, inativo));

        DashboardAdminResponse resposta = service().resumo(atual.getYear(), atual.getMonthValue());

        assertThat(resposta.mentoradosAtivos()).isEqualTo(2);
    }

    @Test
    void distribuicaoPorPlanoSomaCemPorCentoEIgnoraInativos() {
        YearMonth atual = YearMonth.of(2026, 7);
        YearMonth anterior = atual.minusMonths(1);
        mockarVazio(atual, anterior);

        List<Mentorado> mentorados = List.of(
                mentoradoEm("A", Plano.ESSENCIAL, emMes(anterior, 1), true),
                mentoradoEm("B", Plano.ESSENCIAL, emMes(anterior, 1), true),
                mentoradoEm("C", Plano.BASICO, emMes(anterior, 1), true),
                mentoradoEm("D", Plano.PROFISSIONAL, emMes(anterior, 1), false)); // inativo, fora do cálculo
        when(mentoradoRepository.buscarComFiltro(null, null, null)).thenReturn(mentorados);

        DashboardAdminResponse resposta = service().resumo(atual.getYear(), atual.getMonthValue());

        // Tolerância pequena: divisão inteira truncada (2/3 + 1/3) não fecha em exatos 100.0 —
        // mesmo artefato de arredondamento de qualquer gráfico de distribuição percentual.
        double somaPct = resposta.distribuicaoPlano().stream().mapToDouble(DashboardAdminResponse.DistribuicaoPlanoItem::pct).sum();
        assertThat(somaPct).isCloseTo(100.0, org.assertj.core.data.Offset.offset(0.1));
        var essencial = resposta.distribuicaoPlano().stream()
                .filter(i -> i.plano() == Plano.ESSENCIAL).findFirst().orElseThrow();
        assertThat(essencial.quantidade()).isEqualTo(2);
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
        Mentorado mentorado = mentoradoEm("Ana", Plano.ESSENCIAL, emMes(anterior, 1), true);

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
        Mentorado mentorado = mentoradoEm("Ana", Plano.ESSENCIAL, emMes(anterior, 1), true);
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
    void historicoMentoriasEEventosTemSeisMesesEContaPorMesDeRealizacao() {
        YearMonth atual = YearMonth.of(2026, 7);
        YearMonth anterior = atual.minusMonths(1);
        mockarVazio(atual, anterior);

        Colaborador mentor = new Colaborador(null, "Brayan", Area.GESTAO_PERFORMANCE);
        Mentorado mentorado = mentoradoEm("Ana", Plano.ESSENCIAL, emMes(anterior, 1), true);

        Mentoria realizadaEsteMes = new Mentoria(TipoMentoria.INDIVIDUAL, mentor, Set.of(mentorado), emMes(atual, 10), 60, null, null);
        realizadaEsteMes.confirmar();
        realizadaEsteMes.realizar();
        Mentoria realizadaMesPassado = new Mentoria(TipoMentoria.INDIVIDUAL, mentor, Set.of(mentorado), emMes(anterior, 10), 60, null, null);
        realizadaMesPassado.confirmar();
        realizadaMesPassado.realizar();
        when(mentoriaRepository.buscarPorStatus(null)).thenReturn(List.of(realizadaEsteMes, realizadaMesPassado));

        Evento realizadoMesPassado = new Evento("Workshop", TipoEvento.AO_VIVO, "Tema", emMes(anterior, 5), null, null, 50);
        realizadoMesPassado.iniciar();
        realizadoMesPassado.finalizar();
        when(eventoRepository.buscarComFiltro(null, null)).thenReturn(List.of(realizadoMesPassado));

        DashboardAdminResponse resposta = service().resumo(atual.getYear(), atual.getMonthValue());

        assertThat(resposta.historicoMentoriasRealizadas()).hasSize(6);
        assertThat(resposta.historicoMentoriasRealizadas().get(5).mes()).isEqualTo("2026-07");
        assertThat(resposta.historicoMentoriasRealizadas().get(5).valor()).isEqualTo(1.0);
        assertThat(resposta.historicoMentoriasRealizadas().get(4).mes()).isEqualTo("2026-06");
        assertThat(resposta.historicoMentoriasRealizadas().get(4).valor()).isEqualTo(1.0);

        assertThat(resposta.historicoEventosRealizados()).hasSize(6);
        assertThat(resposta.historicoEventosRealizados().get(4).valor()).isEqualTo(1.0);
        assertThat(resposta.historicoEventosRealizados().get(5).valor()).isEqualTo(0.0);
    }

    @Test
    void historicoReceitaTemSeisMesesUsandoOFaturamentoDeCadaMes() {
        YearMonth atual = YearMonth.of(2026, 7);
        YearMonth anterior = atual.minusMonths(1);
        mockarVazio(atual, anterior);

        var faturamentoDeJulho = new DashboardFaturamentoResponse(new BigDecimal("5000.00"), BigDecimal.ZERO, 0.0, List.of());
        when(relatorioFinanceiroService.dashboardFaturamento(atual.getYear(), atual.getMonthValue())).thenReturn(faturamentoDeJulho);

        DashboardAdminResponse resposta = service().resumo(atual.getYear(), atual.getMonthValue());

        assertThat(resposta.historicoReceitaMes()).hasSize(6);
        assertThat(resposta.historicoReceitaMes().get(5).mes()).isEqualTo("2026-07");
        assertThat(resposta.historicoReceitaMes().get(5).valor()).isEqualTo(5000.0);
    }

    @Test
    void atividadesRecentesOrdenaPorDataDescELimitaOito() {
        YearMonth atual = YearMonth.of(2026, 7);
        YearMonth anterior = atual.minusMonths(1);
        mockarVazio(atual, anterior);

        List<Mentorado> mentorados = List.of(
                mentoradoEm("Antigo", Plano.BASICO, emMes(anterior, 1), true),
                mentoradoEm("Recente", Plano.BASICO, emMes(atual, 5), true));
        when(mentoradoRepository.buscarComFiltro(null, null, null)).thenReturn(mentorados);

        DashboardAdminResponse resposta = service().resumo(atual.getYear(), atual.getMonthValue());

        assertThat(resposta.atividadesRecentes().get(0).descricao()).contains("Recente");
        assertThat(resposta.atividadesRecentes()).hasSizeLessThanOrEqualTo(8);
    }
}
