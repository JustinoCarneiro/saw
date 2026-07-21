package com.sawhub.hub.aviso;

import com.sawhub.hub.aviso.dto.CriarAvisoRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** H12.2 — "criar" já é "publicar" (ver Suposição 3 do Blueprint M17). */
@Service
public class AvisoAdminService {

    private final AvisoRepository avisoRepository;
    private final AvisoMentoradoRepository avisoMentoradoRepository;

    public AvisoAdminService(AvisoRepository avisoRepository, AvisoMentoradoRepository avisoMentoradoRepository) {
        this.avisoRepository = avisoRepository;
        this.avisoMentoradoRepository = avisoMentoradoRepository;
    }

    @Transactional
    public Aviso criar(CriarAvisoRequest request) {
        Aviso aviso = new Aviso(request.titulo(), request.descricao(), request.categoria());
        return avisoRepository.save(aviso);
    }

    public List<Aviso> listar() {
        return avisoRepository.findAllByOrderByCriadoEmDesc();
    }

    @Transactional
    public Aviso atualizar(UUID id, CriarAvisoRequest request) {
        Aviso aviso = avisoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Aviso não encontrado: " + id));
        aviso.atualizar(request.titulo(), request.descricao(), request.categoria());
        return aviso;
    }

    @Transactional
    public void excluir(UUID id) {
        if (!avisoRepository.existsById(id)) {
            throw new IllegalArgumentException("Aviso não encontrado: " + id);
        }
        // Apaga primeiro os registros de leitura (sem ON DELETE CASCADE na FK) — senão a
        // exclusão do aviso quebra com violação de FK assim que algum mentorado já leu/tem
        // linha em aviso_mentorado.
        avisoMentoradoRepository.deleteAllByAvisoId(id);
        avisoRepository.deleteById(id);
    }
}
