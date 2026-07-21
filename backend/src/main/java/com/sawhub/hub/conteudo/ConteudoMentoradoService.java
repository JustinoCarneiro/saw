package com.sawhub.hub.conteudo;

import com.sawhub.hub.conteudo.dto.AtualizarConteudoMentoradoRequest;
import com.sawhub.hub.conteudo.dto.ConteudoMentoradoResponse;
import com.sawhub.hub.conteudo.dto.IndicadoresConsumoResponse;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ConteudoMentoradoService {

    private final ConteudoMentoradoRepository conteudoMentoradoRepository;
    private final ConteudoRepository conteudoRepository;
    private final MentoradoRepository mentoradoRepository;

    public ConteudoMentoradoService(ConteudoMentoradoRepository conteudoMentoradoRepository,
                                    ConteudoRepository conteudoRepository,
                                    MentoradoRepository mentoradoRepository) {
        this.conteudoMentoradoRepository = conteudoMentoradoRepository;
        this.conteudoRepository = conteudoRepository;
        this.mentoradoRepository = mentoradoRepository;
    }

    // Nome do parâmetro é usuarioId (não mentoradoId) de propósito — é o id do Usuario
    // autenticado (AppUserPrincipal.getUsuarioId()), resolvido pra Mentorado por dentro via
    // resolverMentorado(). Nomear como "mentoradoId" convidaria um call site futuro a passar
    // mentorado.getId() direto, pulando a resolução a partir do usuário autenticado.
    public List<ConteudoMentoradoResponse> buscarCatalogo(UUID usuarioId, TipoConteudo tipo, Boolean favorito) {
        Mentorado mentorado = resolverMentorado(usuarioId);
        List<Object[]> rows = conteudoMentoradoRepository.buscarCatalogo(mentorado.getId(), tipo, favorito);
        return rows.stream().map(this::mapToResponse).toList();
    }

    public List<ConteudoMentoradoResponse> buscarDicas(UUID usuarioId) {
        Mentorado mentorado = resolverMentorado(usuarioId);
        List<Object[]> rows = conteudoMentoradoRepository.buscarDicas(mentorado.getId());
        return rows.stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public ConteudoMentoradoResponse atualizarStatus(UUID usuarioId, UUID conteudoId, AtualizarConteudoMentoradoRequest request) {
        Mentorado mentorado = resolverMentorado(usuarioId);
        Conteudo conteudo = conteudoRepository.findById(conteudoId)
                .filter(Conteudo::isPublicado)
                .orElseThrow(() -> new NoSuchElementException("Conteúdo não encontrado."));

        ConteudoMentoradoId id = new ConteudoMentoradoId(mentorado.getId(), conteudo.getId());
        ConteudoMentorado cm = conteudoMentoradoRepository.findById(id).orElse(null);

        if (cm == null) {
            cm = new ConteudoMentorado(mentorado, conteudo);
        }

        if (request.favorito() != null) {
            cm.setFavorito(request.favorito());
        }
        if (request.assistido() != null) {
            cm.setAssistido(request.assistido());
        }

        conteudoMentoradoRepository.save(cm);
        return ConteudoMentoradoResponse.from(conteudo, cm);
    }

    // H6.3 — "consumo conta nos meus indicadores (dias assistidos, minutos, favoritas)"
    // (docs/spec.md).
    public IndicadoresConsumoResponse indicadoresConsumo(UUID usuarioId) {
        Mentorado mentorado = resolverMentorado(usuarioId);
        List<ConteudoMentorado> assistidos = conteudoMentoradoRepository.findByMentoradoIdAndAssistidoTrue(mentorado.getId());

        Set<LocalDate> diasComConsumo = assistidos.stream()
                .map(ConteudoMentorado::getDataConsumo)
                .filter(Objects::nonNull)
                .map(instant -> instant.atZone(ZoneId.of("America/Sao_Paulo")).toLocalDate())
                .collect(Collectors.toSet());

        // Duração DECLARADA (Conteudo.duracaoMinutos), não tempo real de exibição — conteúdos
        // sem duração cadastrada (a maioria, opcional) simplesmente não somam nada.
        int minutosAssistidos = assistidos.stream()
                .map(ConteudoMentorado::getConteudo)
                .map(Conteudo::getDuracaoMinutos)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        long favoritas = conteudoMentoradoRepository.countByMentoradoIdAndFavoritoTrue(mentorado.getId());
        return new IndicadoresConsumoResponse(diasComConsumo.size(), favoritas, minutosAssistidos);
    }

    private Mentorado resolverMentorado(UUID usuarioId) {
        return mentoradoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new IllegalStateException("Mentorado não encontrado para o usuário autenticado."));
    }

    private ConteudoMentoradoResponse mapToResponse(Object[] row) {
        Conteudo c = (Conteudo) row[0];
        ConteudoMentorado cm = (ConteudoMentorado) row[1];
        return ConteudoMentoradoResponse.from(c, cm);
    }
}
