package com.sawhub.hub.meta;

import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.meta.dto.AtualizarMetaRequest;
import com.sawhub.hub.meta.dto.CriarMetaAdminRequest;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fase 5 (H3.4, 22/07/2026, pedido do Marcos) — mesmo gap achado e corrigido em
 * EncaminhamentoAdminService: a tela Admin de Metas só tinha listagem + import/export CSV, sem
 * forma de criar ou editar uma meta direto, nem avançar status (Concluir/Pausar/Reativar).
 * Diferente de Encaminhamento, Meta nunca nasce de um fluxo automático (não vem de ata) — então
 * aqui também precisa de criação, não só edição. Sem escopo por usuário/posse, diferente de
 * MetaService (self-service do mentorado): Admin opera sobre qualquer mentorado. */
@Service
public class MetaAdminService {

    private final MetaRepository metaRepository;
    private final MentoradoRepository mentoradoRepository;

    public MetaAdminService(MetaRepository metaRepository, MentoradoRepository mentoradoRepository) {
        this.metaRepository = metaRepository;
        this.mentoradoRepository = mentoradoRepository;
    }

    @Transactional
    public Meta criar(CriarMetaAdminRequest request) {
        Mentorado mentorado = mentoradoRepository.findById(request.mentoradoId())
                .orElseThrow(() -> new NoSuchElementException("Mentorado não encontrado."));
        Meta meta = new Meta(mentorado, request.titulo(), request.descricao(), request.prazo());
        return metaRepository.save(meta);
    }

    @Transactional
    public Meta atualizar(UUID id, AtualizarMetaRequest request) {
        Meta meta = buscar(id);
        meta.editar(request.titulo(), request.descricao(), request.prazo(), request.progressoPct());
        return metaRepository.save(meta);
    }

    @Transactional
    public Meta avancarStatus(UUID id, StatusMeta novoStatus) {
        Meta meta = buscar(id);
        switch (novoStatus) {
            case CONCLUIDA -> meta.concluir();
            case PAUSADA -> meta.pausar();
            case ATIVA -> meta.reativar();
        }
        return metaRepository.save(meta);
    }

    private Meta buscar(UUID id) {
        return metaRepository.buscarPorIdComMentorado(id)
                .orElseThrow(() -> new NoSuchElementException("Meta não encontrada."));
    }
}
