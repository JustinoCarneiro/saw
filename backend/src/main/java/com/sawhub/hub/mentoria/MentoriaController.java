package com.sawhub.hub.mentoria;

import com.sawhub.hub.mentoria.dto.AtaResponse;
import com.sawhub.hub.mentoria.dto.AtualizarMateriaisMentoriaRequest;
import com.sawhub.hub.mentoria.dto.AtualizarStatusMentoriaRequest;
import com.sawhub.hub.mentoria.dto.CriarMentoriaRequest;
import com.sawhub.hub.mentoria.dto.MentoriaResponse;
import com.sawhub.hub.mentoria.dto.MentorResumoResponse;
import com.sawhub.hub.security.RequiresModulo;
import com.sawhub.hub.team.Area;
import com.sawhub.hub.team.Colaborador;
import com.sawhub.hub.team.ColaboradorRepository;
import com.sawhub.hub.team.Modulo;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
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

    public MentoriaController(MentoriaService mentoriaService, AtaService ataService,
                               ColaboradorRepository colaboradorRepository) {
        this.mentoriaService = mentoriaService;
        this.ataService = ataService;
        this.colaboradorRepository = colaboradorRepository;
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

    @GetMapping
    public List<MentoriaResponse> listar(@RequestParam(required = false) StatusMentoria status,
                                          @RequestParam(required = false) Instant de,
                                          @RequestParam(required = false) Instant ate) {
        return mentoriaService.listar(status, de, ate).stream().map(MentoriaResponse::from).toList();
    }

    @GetMapping("/{id}")
    public MentoriaResponse buscar(@PathVariable UUID id) {
        return MentoriaResponse.from(mentoriaService.buscar(id));
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
}
