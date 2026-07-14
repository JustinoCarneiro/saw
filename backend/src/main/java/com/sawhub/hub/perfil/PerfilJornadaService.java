package com.sawhub.hub.perfil;

import com.sawhub.hub.conteudo.ConteudoMentoradoService;
import com.sawhub.hub.conteudo.TipoConteudo;
import com.sawhub.hub.conteudo.dto.ConteudoMentoradoResponse;
import com.sawhub.hub.evento.InscricaoEventoRepository;
import com.sawhub.hub.evento.StatusInscricao;
import com.sawhub.hub.mentorado.EncaminhamentoRepository;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.mentorado.StatusTarefa;
import com.sawhub.hub.mentoria.MentoriaRepository;
import com.sawhub.hub.mentoria.StatusMentoria;
import com.sawhub.hub.meta.MetaRepository;
import com.sawhub.hub.meta.StatusMeta;
import com.sawhub.hub.perfil.dto.JornadaResponse;
import com.sawhub.hub.perfil.dto.JornadaResponse.Conquista;
import com.sawhub.hub.perfil.dto.JornadaResponse.Stats;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** H9.2 — XP/nível 100% derivados por leitura (ver Suposições 2/3 do Blueprint M15 no
 * ROADMAP.md): nenhum side-effect é adicionado aos módulos já fechados (M09-M13) que são a
 * fonte desses contadores. Data de desbloqueio de conquista É persistida (ConquistaDesbloqueada,
 * V18) — ver {@link #sincronizarConquistas} pra como "desde sempre" vs. data real é decidido. */
@Service
public class PerfilJornadaService {

    private final MentoradoRepository mentoradoRepository;
    private final ConteudoMentoradoService conteudoMentoradoService;
    private final InscricaoEventoRepository inscricaoEventoRepository;
    private final MentoriaRepository mentoriaRepository;
    private final MetaRepository metaRepository;
    private final EncaminhamentoRepository encaminhamentoRepository;
    private final ConquistaDesbloqueadaRepository conquistaDesbloqueadaRepository;

    public PerfilJornadaService(MentoradoRepository mentoradoRepository,
                                 ConteudoMentoradoService conteudoMentoradoService,
                                 InscricaoEventoRepository inscricaoEventoRepository,
                                 MentoriaRepository mentoriaRepository,
                                 MetaRepository metaRepository,
                                 EncaminhamentoRepository encaminhamentoRepository,
                                 ConquistaDesbloqueadaRepository conquistaDesbloqueadaRepository) {
        this.mentoradoRepository = mentoradoRepository;
        this.conteudoMentoradoService = conteudoMentoradoService;
        this.inscricaoEventoRepository = inscricaoEventoRepository;
        this.mentoriaRepository = mentoriaRepository;
        this.metaRepository = metaRepository;
        this.encaminhamentoRepository = encaminhamentoRepository;
        this.conquistaDesbloqueadaRepository = conquistaDesbloqueadaRepository;
    }

    @Transactional
    public JornadaResponse jornada(UUID usuarioId) {
        Mentorado mentorado = mentoradoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new IllegalStateException(
                        "Usuário autenticado (id=" + usuarioId + ") não tem Mentorado vinculado."));

        List<ConteudoMentoradoResponse> conteudos = conteudoMentoradoService.buscarCatalogo(usuarioId, null, null);
        long materiaisAcessados = conteudos.stream()
                .filter(c -> c.tipo() != TipoConteudo.VIDEO && (c.assistido() || c.favorito()))
                .count();
        long dicasAssistidas = conteudos.stream()
                .filter(c -> c.tipo() == TipoConteudo.VIDEO && c.assistido())
                .count();

        long eventosParticipados = inscricaoEventoRepository.findByMentoradoId(mentorado.getId()).stream()
                .filter(i -> i.getStatus() == StatusInscricao.PARTICIPOU)
                .count();

        long mentoriasRealizadas = mentoriaRepository.buscarPorMentorado(mentorado).stream()
                .filter(m -> m.getStatus() == StatusMentoria.REALIZADA)
                .count();

        long metasConcluidas = metaRepository.buscarPorMentorado(mentorado.getId(), null).stream()
                .filter(m -> m.getStatus() == StatusMeta.CONCLUIDA)
                .count();

        long tarefasConcluidas = encaminhamentoRepository.buscarPorMentorado(mentorado.getId(), null, null).stream()
                .filter(e -> e.getStatus() == StatusTarefa.CONCLUIDA)
                .count();

        int xp = calcularXp(materiaisAcessados, dicasAssistidas, eventosParticipados, mentoriasRealizadas,
                metasConcluidas, tarefasConcluidas);

        NivelJornada nivelAtual = NivelJornada.paraXp(xp);
        NivelJornada proximo = nivelAtual.proximo();
        Integer xpProximoNivel = proximo == null ? null : proximo.getXpMinimo();
        int progressoPct = calcularProgressoPct(xp, nivelAtual, proximo);

        Stats stats = new Stats(materiaisAcessados, dicasAssistidas, eventosParticipados, mentoriasRealizadas);
        List<Conquista> conquistas = sincronizarConquistas(mentorado, materiaisAcessados, dicasAssistidas,
                eventosParticipados, mentoriasRealizadas, metasConcluidas, tarefasConcluidas);

        return new JornadaResponse(nivelAtual, xp, xpProximoNivel, progressoPct, stats, conquistas);
    }

    // Fórmula documentada no Blueprint M15 (ROADMAP.md) — pesos ajustáveis, não é regra de
    // negócio validada com o cliente, é uma primeira leva pra demonstrar o conceito de jornada.
    private static int calcularXp(long materiaisAcessados, long dicasAssistidas, long eventosParticipados,
                                   long mentoriasRealizadas, long metasConcluidas, long tarefasConcluidas) {
        return (int) (materiaisAcessados * 10 + dicasAssistidas * 15 + eventosParticipados * 150
                + mentoriasRealizadas * 200 + metasConcluidas * 100 + tarefasConcluidas * 15);
    }

    private static int calcularProgressoPct(int xp, NivelJornada atual, NivelJornada proximo) {
        if (proximo == null) {
            return 100;
        }
        int faixa = proximo.getXpMinimo() - atual.getXpMinimo();
        int progresso = xp - atual.getXpMinimo();
        int pct = faixa <= 0 ? 100 : (progresso * 100) / faixa;
        return Math.max(0, Math.min(100, pct));
    }

    // H9.2 — "desde sempre" vs. data real: se essa é a PRIMEIRA VEZ que computamos a jornada
    // deste mentorado desde a V18 (conquistasObservadasEm nulo), qualquer conquista já verdadeira
    // agora é backfillada com desbloqueadaEm=null ("já era antes de rastrearmos", sem data
    // fabricada) — e o marco fica setado, então da PRÓXIMA vez em diante, uma conquista nova
    // ganha timestamp real. Sem essa marca, a primeira leitura pós-deploy de QUALQUER mentorado
    // gravaria hoje como data de algo que já era verdade há meses.
    private List<Conquista> sincronizarConquistas(Mentorado mentorado, long materiaisAcessados,
                                                    long dicasAssistidas, long eventosParticipados,
                                                    long mentoriasRealizadas, long metasConcluidas,
                                                    long tarefasConcluidas) {
        boolean ferramentasEmDia = mentorado.getFerramentasTotal() != null && mentorado.getFerramentasTotal() > 0
                && mentorado.getFerramentasConcluidas() != null
                && mentorado.getFerramentasConcluidas().intValue() == mentorado.getFerramentasTotal().intValue();
        boolean emCrescimento = mentorado.getCrescimentoFaturamentoPct() != null
                && mentorado.getCrescimentoFaturamentoPct().compareTo(BigDecimal.ZERO) > 0;

        record Condicao(String codigo, String titulo, String descricao, boolean desbloqueada) {
        }
        List<Condicao> condicoes = List.of(
                new Condicao("PRIMEIRO_EVENTO", "Primeiro Evento", "Participou de um evento da SAW.",
                        eventosParticipados >= 1),
                new Condicao("MENTORIA_REALIZADA", "Mentoria Realizada", "Participou de uma mentoria.",
                        mentoriasRealizadas >= 1),
                new Condicao("MARATONISTA", "Maratonista", "Acessou 10 ou mais materiais.",
                        materiaisAcessados >= 10),
                new Condicao("SEMPRE_LIGADO", "Sempre Ligado", "Assistiu 5 ou mais dicas do Brayan.",
                        dicasAssistidas >= 5),
                new Condicao("META_BATIDA", "Meta Batida", "Concluiu ao menos uma meta estratégica.",
                        metasConcluidas >= 1),
                new Condicao("PRODUTIVO", "Produtivo", "Concluiu 10 ou mais tarefas.",
                        tarefasConcluidas >= 10),
                new Condicao("EM_CRESCIMENTO", "Em Crescimento", "Faturamento em crescimento no período.",
                        emCrescimento),
                new Condicao("FERRAMENTAS_EM_DIA", "Ferramentas em Dia", "Concluiu todas as ferramentas obrigatórias.",
                        ferramentasEmDia)
        );

        boolean primeiraObservacao = mentorado.getConquistasObservadasEm() == null;
        Map<String, Instant> datasPorCodigo = new HashMap<>();
        for (ConquistaDesbloqueada existente : conquistaDesbloqueadaRepository.findByMentoradoId(mentorado.getId())) {
            datasPorCodigo.put(existente.getCodigo(), existente.getDesbloqueadaEm());
        }

        for (Condicao condicao : condicoes) {
            if (condicao.desbloqueada() && !datasPorCodigo.containsKey(condicao.codigo())) {
                Instant data = primeiraObservacao ? null : Instant.now();
                conquistaDesbloqueadaRepository.save(new ConquistaDesbloqueada(mentorado.getId(), condicao.codigo(), data));
                datasPorCodigo.put(condicao.codigo(), data);
            }
        }

        if (primeiraObservacao) {
            mentorado.marcarConquistasObservadas();
            mentoradoRepository.save(mentorado);
        }

        return condicoes.stream()
                .map(c -> new Conquista(c.codigo(), c.titulo(), c.descricao(), c.desbloqueada(), datasPorCodigo.get(c.codigo())))
                .toList();
    }
}
