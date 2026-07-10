package com.sawhub.hub.mentorado;

import com.sawhub.hub.aviso.AvisoMentoradoService;
import com.sawhub.hub.conteudo.ConteudoRepository;
import com.sawhub.hub.conteudo.TipoConteudo;
import com.sawhub.hub.mentorado.dto.DashboardMentoradoResponse;
import com.sawhub.hub.mentorado.dto.DashboardMentoradoResponse.Compromisso;
import com.sawhub.hub.mentorado.dto.DashboardMentoradoResponse.DicaDestaque;
import com.sawhub.hub.mentoria.MentoriaRepository;
import com.sawhub.hub.mentoria.StatusMentoria;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** H2.1–H2.3 (M08) — agrega dado que já existe (nenhuma entidade nova, ver Blueprint M08 no
 * ROADMAP.md). "meta semanal" (E3) ainda não tem entidade própria ligada aqui — volta null nesta
 * leva, não é bug. "avisos" (E16) fechado no M17 — reaproveita {@link AvisoMentoradoService},
 * sem duplicar a agregação de plano/leitura. */
@Service
public class MentoradoDashboardService {

    private static final int MAX_AVISOS_DASHBOARD = 3;

    private final MentoradoRepository mentoradoRepository;
    private final EncaminhamentoRepository encaminhamentoRepository;
    private final MentoriaRepository mentoriaRepository;
    private final ConteudoRepository conteudoRepository;
    private final AvisoMentoradoService avisoMentoradoService;

    public MentoradoDashboardService(MentoradoRepository mentoradoRepository,
                                      EncaminhamentoRepository encaminhamentoRepository,
                                      MentoriaRepository mentoriaRepository,
                                      ConteudoRepository conteudoRepository,
                                      AvisoMentoradoService avisoMentoradoService) {
        this.mentoradoRepository = mentoradoRepository;
        this.encaminhamentoRepository = encaminhamentoRepository;
        this.mentoriaRepository = mentoriaRepository;
        this.conteudoRepository = conteudoRepository;
        this.avisoMentoradoService = avisoMentoradoService;
    }

    public DashboardMentoradoResponse dashboard(UUID usuarioId) {
        Mentorado mentorado = mentoradoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new IllegalStateException(
                        "Usuário autenticado (id=" + usuarioId + ") não tem Mentorado vinculado."));

        List<Encaminhamento> encaminhamentos = encaminhamentoRepository.findByMentoradoIdOrderByCriadoEmDesc(mentorado.getId());
        long tarefasAbertas = encaminhamentos.stream().filter(e -> !e.isConcluido()).count();
        long pesoTotal = encaminhamentos.stream().mapToLong(Encaminhamento::getPeso).sum();
        long pesoConcluido = encaminhamentos.stream()
                .filter(Encaminhamento::isConcluido)
                .mapToLong(Encaminhamento::getPeso)
                .sum();
        int evolucaoGeralPct = ProgressoCalculator.pctPeso(pesoConcluido, pesoTotal);

        List<Compromisso> compromissos = compromissosFuturos(mentorado);
        Compromisso proximaReuniao = compromissos.isEmpty() ? null : compromissos.get(0);

        return new DashboardMentoradoResponse(
                mentorado.getNome(),
                evolucaoGeralPct,
                tarefasAbertas,
                null, // meta semanal — E3 não construído nesta leva (ver Blueprint M08)
                proximaReuniao,
                compromissos,
                dicaDestaque(mentorado),
                avisoMentoradoService.listar(usuarioId, null, null).stream().limit(MAX_AVISOS_DASHBOARD).toList()
        );
    }

    private List<Compromisso> compromissosFuturos(Mentorado mentorado) {
        Instant agora = Instant.now();
        return mentoriaRepository.buscarPorMentorado(mentorado).stream()
                .filter(m -> m.getDataHora().isAfter(agora))
                .filter(m -> m.getStatus() == StatusMentoria.AGENDADA || m.getStatus() == StatusMentoria.CONFIRMADA)
                .sorted((a, b) -> a.getDataHora().compareTo(b.getDataHora()))
                .map(Compromisso::from)
                .toList();
    }

    private DicaDestaque dicaDestaque(Mentorado mentorado) {
        // buscarComFiltro já devolve ordenado DESC por criadoEm — primeiro elegível é o mais
        // recente dentro do plano do mentorado. Comparação de plano centralizada em
        // Plano.atendePlanoMinimo() (achado do revisor-seguranca no M11: esta lógica tinha sido
        // duplicada independentemente em ConteudoMentoradoService, invalidando a nota antiga de
        // "único ponto do backend").
        return conteudoRepository.buscarComFiltro(TipoConteudo.VIDEO, null, true).stream()
                .filter(c -> mentorado.getPlano().atendePlanoMinimo(c.getPlanoMinimo()))
                .findFirst()
                .map(DicaDestaque::from)
                .orElse(null);
    }
}
