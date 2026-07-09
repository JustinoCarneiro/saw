package com.sawhub.hub.meta;

import com.sawhub.hub.meta.dto.AtualizarMetaRequest;
import com.sawhub.hub.meta.dto.CriarMetaRequest;
import com.sawhub.hub.meta.dto.MetaResponse;
import com.sawhub.hub.meta.dto.ResumoMetasResponse;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** H3.1–H3.3 (M09) — self-service do mentorado (ver ROADMAP.md M09: nenhuma história cobre
 * curadoria do Admin). Toda operação resolve o {@link Mentorado} a partir do usuário autenticado
 * — create/editar/transição nunca aceitam um id de mentorado vindo de request. */
@Service
public class MetaService {

    private final MetaRepository metaRepository;
    private final MentoradoRepository mentoradoRepository;

    public MetaService(MetaRepository metaRepository, MentoradoRepository mentoradoRepository) {
        this.metaRepository = metaRepository;
        this.mentoradoRepository = mentoradoRepository;
    }

    public Meta criar(UUID usuarioId, CriarMetaRequest request) {
        Mentorado mentorado = mentoradoDoUsuario(usuarioId);
        Meta meta = new Meta(mentorado, request.titulo(), request.descricao(), request.prazo());
        return metaRepository.save(meta);
    }

    public List<Meta> listar(UUID usuarioId, StatusMeta status) {
        Mentorado mentorado = mentoradoDoUsuario(usuarioId);
        return metaRepository.buscarPorMentorado(mentorado.getId(), status);
    }

    public ResumoMetasResponse resumo(UUID usuarioId) {
        Mentorado mentorado = mentoradoDoUsuario(usuarioId);
        LocalDate hoje = LocalDate.now();
        List<MetaResponse> todas = metaRepository.buscarPorMentorado(mentorado.getId(), null).stream()
                .map(m -> MetaResponse.from(m, hoje))
                .toList();

        int conclusaoMedia = todas.isEmpty() ? 0
                : (int) Math.round(todas.stream().mapToInt(MetaResponse::progressoPct).average().orElse(0));
        long concluidas = todas.stream().filter(m -> m.status() == StatusMeta.CONCLUIDA).count();
        long noPrazo = todas.stream().filter(m -> m.subStatus() == SubStatusMeta.NO_PRAZO).count();
        long atrasadas = todas.stream().filter(m -> m.subStatus() == SubStatusMeta.ATRASADA).count();

        return new ResumoMetasResponse(conclusaoMedia, concluidas, noPrazo, atrasadas);
    }

    public Meta atualizar(UUID usuarioId, UUID metaId, AtualizarMetaRequest request) {
        Meta meta = buscarDoUsuario(usuarioId, metaId);
        meta.editar(request.titulo(), request.descricao(), request.prazo(), request.progressoPct());
        return metaRepository.save(meta);
    }

    public Meta avancarStatus(UUID usuarioId, UUID metaId, StatusMeta novoStatus) {
        Meta meta = buscarDoUsuario(usuarioId, metaId);
        switch (novoStatus) {
            case CONCLUIDA -> meta.concluir();
            case PAUSADA -> meta.pausar();
            case ATIVA -> meta.reativar();
        }
        return metaRepository.save(meta);
    }

    private Mentorado mentoradoDoUsuario(UUID usuarioId) {
        return mentoradoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new IllegalStateException(
                        "Usuário autenticado (id=" + usuarioId + ") não tem Mentorado vinculado."));
    }

    // 404 genérico pra "não existe" e "existe mas não é sua" (isolamento por tenant, mesmo
    // princípio do H1.1) — não confirma nem nega que a meta existe pra outro mentorado.
    private Meta buscarDoUsuario(UUID usuarioId, UUID metaId) {
        Mentorado mentorado = mentoradoDoUsuario(usuarioId);
        return metaRepository.findById(metaId)
                .filter(m -> m.getMentorado().getId().equals(mentorado.getId()))
                .orElseThrow(() -> new NoSuchElementException("Meta não encontrada."));
    }
}
