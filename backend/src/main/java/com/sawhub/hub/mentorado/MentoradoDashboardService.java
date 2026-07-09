package com.sawhub.hub.mentorado;

import com.sawhub.hub.conteudo.ConteudoRepository;
import com.sawhub.hub.conteudo.TipoConteudo;
import com.sawhub.hub.mentorado.dto.DashboardMentoradoResponse;
import com.sawhub.hub.mentorado.dto.DashboardMentoradoResponse.Compromisso;
import com.sawhub.hub.mentorado.dto.DashboardMentoradoResponse.DicaDestaque;
import com.sawhub.hub.mentoria.Mentoria;
import com.sawhub.hub.mentoria.MentoriaRepository;
import com.sawhub.hub.mentoria.StatusMentoria;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** H2.1–H2.3 (M08) — agrega dado que já existe (nenhuma entidade nova, ver Blueprint M08 no
 * ROADMAP.md). "meta semanal" (E3) e "avisos" (E16) ainda não têm entidade nenhuma no sistema —
 * voltam null/vazio nesta leva, não é bug. */
@Service
public class MentoradoDashboardService {

    private final MentoradoRepository mentoradoRepository;
    private final EncaminhamentoRepository encaminhamentoRepository;
    private final MentoriaRepository mentoriaRepository;
    private final ConteudoRepository conteudoRepository;

    public MentoradoDashboardService(MentoradoRepository mentoradoRepository,
                                      EncaminhamentoRepository encaminhamentoRepository,
                                      MentoriaRepository mentoriaRepository,
                                      ConteudoRepository conteudoRepository) {
        this.mentoradoRepository = mentoradoRepository;
        this.encaminhamentoRepository = encaminhamentoRepository;
        this.mentoriaRepository = mentoriaRepository;
        this.conteudoRepository = conteudoRepository;
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
                List.of() // avisos — E16 não construído nesta leva (ver Blueprint M08)
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
        // recente dentro do plano do mentorado.
        // Nota do revisor-seguranca: único ponto do backend que compara Plano por ordinal() —
        // correto hoje (ordem declarada do enum já é a hierarquia de negócio), mas reordenar
        // Plano.java silenciosamente muda quem vê o quê. Manter GRATUITO→PROFISSIONAL nessa ordem.
        return conteudoRepository.buscarComFiltro(TipoConteudo.VIDEO, null, true).stream()
                .filter(c -> c.getPlanoMinimo().ordinal() <= mentorado.getPlano().ordinal())
                .findFirst()
                .map(DicaDestaque::from)
                .orElse(null);
    }
}
