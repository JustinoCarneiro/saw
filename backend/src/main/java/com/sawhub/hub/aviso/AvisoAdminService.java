package com.sawhub.hub.aviso;

import com.sawhub.hub.aviso.dto.CriarAvisoRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** H12.2 — "criar" já é "publicar" (ver Suposição 3 do Blueprint M17); sem edição/exclusão
 * nesta leva (Suposição 4). */
@Service
public class AvisoAdminService {

    private final AvisoRepository avisoRepository;

    public AvisoAdminService(AvisoRepository avisoRepository) {
        this.avisoRepository = avisoRepository;
    }

    @Transactional
    public Aviso criar(CriarAvisoRequest request) {
        Aviso aviso = new Aviso(request.titulo(), request.descricao(), request.categoria(), request.planoMinimo());
        return avisoRepository.save(aviso);
    }

    public List<Aviso> listar() {
        return avisoRepository.findAllByOrderByCriadoEmDesc();
    }
}
