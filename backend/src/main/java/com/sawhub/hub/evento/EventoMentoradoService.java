package com.sawhub.hub.evento;

import com.sawhub.hub.evento.dto.EventoMentoradoResponse;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.mentorado.TipoContrato;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** H7.1-H7.2 (M13) — leitura + inscrição mentee-facing sobre a mesma entidade {@link Evento} do
 * Admin (M06). Mesmo padrão de isolamento do M08-M12: o id do Mentorado nunca vem de parâmetro,
 * só do usuário autenticado.
 *
 * <p>M28 (change request, 21/07/2026) — "controle de vagas em evento por mentorado da Contínua":
 * ganhou um segundo caminho de entrada, {@link #inscreverAdmin}/{@link #cancelarAdmin}, chamado
 * pelo time interno (achado da investigação: a área do mentorado está pausada hoje —
 * {@code AREA_MENTORADO_PAUSADA} —, então o self-service abaixo é inalcançável na prática; sem um
 * caminho admin, a cota nunca teria como ser exercitada de verdade). Os dois caminhos convergem no
 * mesmo núcleo ({@link #inscreverNucleo}/{@link #cancelarNucleo}) — a cota vale pra ambos, não é
 * uma regra "só quando é o admin que inscreve". */
@Service
public class EventoMentoradoService {

    // M28 — "3 grátis/ano" (ano de CONTRATO, janela rolante a partir de dataFechamentoContrato,
    // não ano civil — decisão confirmada com o Marcos, ver ROADMAP.md).
    private static final int COTA_EVENTOS_GRATIS_CONTINUA = 3;

    private final EventoRepository eventoRepository;
    private final InscricaoEventoRepository inscricaoEventoRepository;
    private final MentoradoRepository mentoradoRepository;

    public EventoMentoradoService(EventoRepository eventoRepository, InscricaoEventoRepository inscricaoEventoRepository,
                                   MentoradoRepository mentoradoRepository) {
        this.eventoRepository = eventoRepository;
        this.inscricaoEventoRepository = inscricaoEventoRepository;
        this.mentoradoRepository = mentoradoRepository;
    }

    // spec.md H7.1: só eventos PROGRAMADO/AO_VIVO (ver EventoRepository.buscarPorStatusIn) —
    // diferente do M12, este épico não pede histórico de eventos passados.
    public List<EventoMentoradoResponse> listar(UUID usuarioId, TipoEvento tipo, String tema) {
        Mentorado mentorado = resolverMentorado(usuarioId);
        List<Evento> eventos = eventoRepository.buscarPorStatusIn(
                List.of(StatusEvento.PROGRAMADO, StatusEvento.AO_VIVO), tipo);
        if (tema != null && !tema.isBlank()) {
            eventos = eventos.stream()
                    .filter(e -> e.getTema() != null && e.getTema().toLowerCase().contains(tema.toLowerCase()))
                    .toList();
        }

        Set<UUID> inscritos = inscricaoEventoRepository.findByMentoradoId(mentorado.getId()).stream()
                .filter(i -> i.getStatus() != StatusInscricao.CANCELADA)
                .map(i -> i.getEvento().getId())
                .collect(Collectors.toSet());

        return eventos.stream().map(e -> EventoMentoradoResponse.from(e, inscritos.contains(e.getId()))).toList();
    }

    @Transactional
    public EventoMentoradoResponse inscrever(UUID usuarioId, UUID eventoId) {
        return inscreverNucleo(resolverMentorado(usuarioId), eventoId);
    }

    @Transactional
    public void cancelar(UUID usuarioId, UUID eventoId) {
        cancelarNucleo(resolverMentorado(usuarioId), eventoId);
    }

    // M28 — mesmo núcleo do self-service acima, mentoradoId vem de path param (ação do time
    // interno em nome do mentorado), não do usuário autenticado.
    @Transactional
    public EventoMentoradoResponse inscreverAdmin(UUID mentoradoId, UUID eventoId) {
        return inscreverNucleo(buscarMentorado(mentoradoId), eventoId);
    }

    @Transactional
    public void cancelarAdmin(UUID mentoradoId, UUID eventoId) {
        cancelarNucleo(buscarMentorado(mentoradoId), eventoId);
    }

    /** M28 — histórico de inscrições deste mentorado (qualquer status), pra tela admin mostrar
     * o que já foi usado da cota + permitir cancelar. Dataset pequeno por mentorado (mesmo
     * raciocínio de não precisar de uma query paginada nova — ver findByMentoradoId). */
    public List<InscricaoEvento> listarInscricoesAdmin(UUID mentoradoId) {
        buscarMentorado(mentoradoId); // 404 cedo se o mentorado não existe, mesmo padrão do resto do módulo
        return inscricaoEventoRepository.buscarPorMentoradoComEvento(mentoradoId);
    }

    /** M28 — resumo da cota pra exibição (card "X/3 usados neste ciclo"). Cálculo do ciclo
     * centralizado aqui e reaproveitado por {@link #exigirCotaDisponivel} — nunca duplicar essa
     * matemática de janela rolante no frontend (risco real de divergir do que o backend realmente
     * aplica na hora de bloquear). */
    public CotaEventosInfo consultarCotaAdmin(UUID mentoradoId) {
        Mentorado mentorado = buscarMentorado(mentoradoId);
        if (mentorado.getTipoContrato() != TipoContrato.MENTORIA_CONTINUA || mentorado.getDataFechamentoContrato() == null) {
            return new CotaEventosInfo(false, 0, COTA_EVENTOS_GRATIS_CONTINUA, null, null);
        }
        Ciclo ciclo = calcularCicloAtual(mentorado.getDataFechamentoContrato());
        long usadas = contarUsadasNoCiclo(mentorado, ciclo);
        return new CotaEventosInfo(true, (int) usadas, COTA_EVENTOS_GRATIS_CONTINUA, ciclo.inicio(), ciclo.fim());
    }

    public record CotaEventosInfo(boolean aplicavel, int usadas, int limite, LocalDate inicioCiclo, LocalDate fimCiclo) {
    }

    private EventoMentoradoResponse inscreverNucleo(Mentorado mentorado, UUID eventoId) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new NoSuchElementException("Evento não encontrado."));
        if (evento.getStatus() != StatusEvento.PROGRAMADO && evento.getStatus() != StatusEvento.AO_VIVO) {
            throw new IllegalStateException("Este evento não está aceitando inscrições.");
        }

        InscricaoEvento inscricao = inscricaoEventoRepository
                .findByMentoradoIdAndEventoId(mentorado.getId(), eventoId).orElse(null);
        // Idempotente: reinscrever num evento em que já está INSCRITA não é erro, nem ocupa uma
        // segunda vaga (duplo clique/retry de rede não deve punir o mentorado).
        if (inscricao != null && inscricao.getStatus() == StatusInscricao.INSCRITA) {
            return EventoMentoradoResponse.from(evento, true);
        }

        // M28 — só consome cota quando a inscrição vai de fato ocupar uma vaga NOVA (linha acima
        // já filtrou o caso idempotente). Só se aplica a Mentoria Contínua — Individual/Consultoria
        // não têm essa cota.
        if (mentorado.getTipoContrato() == TipoContrato.MENTORIA_CONTINUA) {
            exigirCotaDisponivel(mentorado);
        }

        // ocuparVaga() muta Evento (protegido por @Version) na mesma transação que salva a
        // InscricaoEvento — duas inscrições concorrentes na última vaga fazem a 2ª save() do
        // Evento estourar 409 (ObjectOptimisticLockingFailureException), não silenciosamente
        // ultrapassar a capacidade. Ver ROADMAP.md M13.
        evento.ocuparVaga();
        eventoRepository.save(evento);

        if (inscricao == null) {
            inscricao = new InscricaoEvento(mentorado, evento);
        } else {
            inscricao.reinscrever();
        }
        inscricaoEventoRepository.save(inscricao);

        return EventoMentoradoResponse.from(evento, true);
    }

    private void cancelarNucleo(Mentorado mentorado, UUID eventoId) {
        InscricaoEvento inscricao = inscricaoEventoRepository
                .findByMentoradoIdAndEventoId(mentorado.getId(), eventoId)
                .filter(i -> i.getStatus() == StatusInscricao.INSCRITA)
                .orElseThrow(() -> new NoSuchElementException("Inscrição não encontrada."));

        inscricao.cancelar();
        inscricaoEventoRepository.save(inscricao);

        // M28 — cancelamento devolve a cota (a próxima checagem de exigirCotaDisponivel não conta
        // mais essa linha, já que ela filtra status != CANCELADA), mesma simetria de liberarVaga().
        Evento evento = inscricao.getEvento();
        evento.liberarVaga();
        eventoRepository.save(evento);
    }

    // M28 — "3 grátis/ano de CONTRATO" (não ano civil, decisão confirmada com o Marcos): janela
    // rolante de 12 meses a partir de dataFechamentoContrato, achando o ciclo atual (o k-ésimo
    // aniversário que já passou) e contando só inscrições ATIVAS de eventos DENTRO desse ciclo.
    // Sem dataFechamentoContrato (Mentoria Contínua criada sem dado de contrato completo ainda)
    // não há como ancorar a janela — não bloqueia por dado incompleto que não é culpa do mentorado.
    private void exigirCotaDisponivel(Mentorado mentorado) {
        LocalDate dataFechamento = mentorado.getDataFechamentoContrato();
        if (dataFechamento == null) {
            return;
        }
        Ciclo ciclo = calcularCicloAtual(dataFechamento);
        long usadas = contarUsadasNoCiclo(mentorado, ciclo);
        if (usadas >= COTA_EVENTOS_GRATIS_CONTINUA) {
            throw new IllegalStateException(
                    "Cota de " + COTA_EVENTOS_GRATIS_CONTINUA + " eventos grátis deste ciclo de contrato já foi usada.");
        }
    }

    private record Ciclo(LocalDate inicio, LocalDate fim) {
    }

    private static Ciclo calcularCicloAtual(LocalDate dataFechamentoContrato) {
        LocalDate hoje = LocalDate.now();
        long ciclosDecorridos = Math.max(0, ChronoUnit.YEARS.between(dataFechamentoContrato, hoje));
        LocalDate inicioCiclo = dataFechamentoContrato.plusYears(ciclosDecorridos);
        return new Ciclo(inicioCiclo, inicioCiclo.plusYears(1));
    }

    private long contarUsadasNoCiclo(Mentorado mentorado, Ciclo ciclo) {
        return inscricaoEventoRepository.buscarPorMentoradoComEvento(mentorado.getId()).stream()
                .filter(i -> i.getStatus() != StatusInscricao.CANCELADA)
                .filter(i -> {
                    LocalDate dataEvento = i.getEvento().getDataHora().atZone(ZoneOffset.UTC).toLocalDate();
                    return !dataEvento.isBefore(ciclo.inicio()) && dataEvento.isBefore(ciclo.fim());
                })
                .count();
    }

    private Mentorado resolverMentorado(UUID usuarioId) {
        return mentoradoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new IllegalStateException("Mentorado não encontrado para o usuário autenticado."));
    }

    private Mentorado buscarMentorado(UUID mentoradoId) {
        return mentoradoRepository.findById(mentoradoId)
                .orElseThrow(() -> new IllegalArgumentException("Mentorado não encontrado."));
    }
}
