package com.sawhub.hub.mentoria;

import com.sawhub.hub.mentoria.dto.AtaResponse;
import com.sawhub.hub.mentoria.dto.AtualizarMateriaisMentoriaRequest;
import com.sawhub.hub.mentoria.dto.AtualizarStatusMentoriaRequest;
import com.sawhub.hub.mentoria.dto.CriarMentoriaRequest;
import com.sawhub.hub.mentoria.dto.MentoriaResponse;
import com.sawhub.hub.mentoria.dto.MentorResumoResponse;
import com.sawhub.hub.mentoria.dto.PresencaResumoRow;
import com.sawhub.hub.mentoria.dto.RegistrarPresencasRequest;
import com.sawhub.hub.security.RequiresModulo;
import com.sawhub.hub.team.Area;
import com.sawhub.hub.team.Colaborador;
import com.sawhub.hub.team.ColaboradorRepository;
import com.sawhub.hub.team.Modulo;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** H11.2 (criação/agenda) — o mesmo dado alimenta a tela do mentorado (H5.1/H5.2/H5.3) via
 * {@link MentoriaMentoradoController}, read-only e tenant-scoped, ver ROADMAP.md M12. */
@RestController
@RequestMapping("/api/v1/admin/mentorias")
@RequiresModulo(Modulo.MENTORADOS)
public class MentoriaController {

    private final MentoriaService mentoriaService;
    private final AtaService ataService;
    private final ColaboradorRepository colaboradorRepository;
    private final PresencaMentoriaRepository presencaMentoriaRepository;

    public MentoriaController(MentoriaService mentoriaService, AtaService ataService,
                               ColaboradorRepository colaboradorRepository,
                               PresencaMentoriaRepository presencaMentoriaRepository) {
        this.mentoriaService = mentoriaService;
        this.ataService = ataService;
        this.colaboradorRepository = colaboradorRepository;
        this.presencaMentoriaRepository = presencaMentoriaRepository;
    }

    /** Só leitura, pro seletor de mentor no formulário de criação (H11.2) — mesmo raciocínio do
     * `/comercial/vendedores` do M05: TeamController é gated por Modulo.TIME (só Fundador), então
     * uma Gestão de Performance não-Fundador não conseguiria listar colaboradores por ali. */
    @GetMapping("/mentores")
    public List<MentorResumoResponse> mentores() {
        return colaboradorRepository.findAllByAreaIn(List.of(Area.GESTAO_PERFORMANCE, Area.ADMIN)).stream()
                .sorted(Comparator.comparing(Colaborador::getNome))
                .map(MentorResumoResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MentoriaResponse criar(@Valid @RequestBody CriarMentoriaRequest request) {
        return MentoriaResponse.from(mentoriaService.criar(request));
    }

    // M28 — tipo/mentoradoId novos (ver MentoriaService#listar): a lista central (MentoriasAgendaPage)
    // passa a pedir tipo=GRUPO por padrão; a página do mentorado (MentoradoDetalhePage) pede
    // mentoradoId pra ver o histórico individual/consultoria dele, sem tipo (todos os tipos).
    @GetMapping
    public List<MentoriaResponse> listar(@RequestParam(required = false) StatusMentoria status,
                                          @RequestParam(required = false) TipoMentoria tipo,
                                          @RequestParam(required = false) UUID mentoradoId,
                                          @RequestParam(required = false) Instant de,
                                          @RequestParam(required = false) Instant ate) {
        return mentoriaService.listar(status, tipo, mentoradoId, de, ate).stream().map(MentoriaResponse::from).toList();
    }

    @GetMapping("/{id}")
    public MentoriaResponse buscar(@PathVariable UUID id) {
        Mentoria mentoria = mentoriaService.buscar(id);
        return MentoriaResponse.from(mentoria, presencasPorMentorado(id));
    }

    @PatchMapping("/{id}/status")
    public MentoriaResponse atualizarStatus(@PathVariable UUID id, @Valid @RequestBody AtualizarStatusMentoriaRequest request) {
        return MentoriaResponse.from(mentoriaService.avancarStatus(id, request.novoStatus()));
    }

    /** M12 — pré-requisito de H5.2 (ver ROADMAP.md): sem isto o mentorado nunca teria materiais
     * recomendados pra ver. Substitui a lista inteira (idempotente, não incremental). */
    @PatchMapping("/{id}/materiais")
    public MentoriaResponse atualizarMateriais(@PathVariable UUID id, @Valid @RequestBody AtualizarMateriaisMentoriaRequest request) {
        return MentoriaResponse.from(mentoriaService.atualizarMateriais(id, request.conteudoIds()));
    }

    /** CLAUDE.md: "Realizada (gera ata)" — transição + criação da ata numa operação só,
     * por isso vive em {@link AtaService}, não em {@link MentoriaService#avancarStatus}. */
    @PostMapping("/{id}/realizar")
    public AtaResponse realizar(@PathVariable UUID id) {
        Ata ata = ataService.realizarMentoria(id);
        return AtaResponse.from(ata, ataService.listarSugestoes(id));
    }

    /** E17/M27 — presença por mentorado, só mentoria GRUPO (ver MentoriaService#registrarPresencas). */
    @PatchMapping("/{id}/presencas")
    public MentoriaResponse registrarPresencas(@PathVariable UUID id, @Valid @RequestBody RegistrarPresencasRequest request) {
        Mentoria mentoria = mentoriaService.registrarPresencas(id, request);
        return MentoriaResponse.from(mentoria, presencasPorMentorado(id));
    }

    private Map<UUID, Boolean> presencasPorMentorado(UUID mentoriaId) {
        return presencaMentoriaRepository.buscarResumoPorMentoria(mentoriaId).stream()
                .collect(Collectors.toMap(PresencaResumoRow::mentoradoId, PresencaResumoRow::presente));
    }
}
