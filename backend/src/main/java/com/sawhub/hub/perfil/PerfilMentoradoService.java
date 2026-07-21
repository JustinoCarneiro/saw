package com.sawhub.hub.perfil;

import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.perfil.dto.AtualizarPerfilMentoradoRequest;
import com.sawhub.hub.perfil.dto.PerfilMentoradoResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** H9.1 (perfil) — leitura/escrita direta do {@link Mentorado} já resolvido, sem agregação de
 * outros módulos (ver {@link PerfilJornadaService} pra H9.2, que cruza dado de vários módulos). */
@Service
@Transactional(readOnly = true)
public class PerfilMentoradoService {

    private final MentoradoRepository mentoradoRepository;

    public PerfilMentoradoService(MentoradoRepository mentoradoRepository) {
        this.mentoradoRepository = mentoradoRepository;
    }

    public PerfilMentoradoResponse buscar(UUID usuarioId) {
        return PerfilMentoradoResponse.from(resolverMentorado(usuarioId));
    }

    @Transactional
    public PerfilMentoradoResponse atualizar(UUID usuarioId, AtualizarPerfilMentoradoRequest request) {
        Mentorado mentorado = resolverMentorado(usuarioId);
        mentorado.atualizarPerfil(request.telefone(), request.bio(), request.fotoUrl());
        return PerfilMentoradoResponse.from(mentorado);
    }

    private Mentorado resolverMentorado(UUID usuarioId) {
        return mentoradoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new IllegalStateException(
                        "Usuário autenticado (id=" + usuarioId + ") não tem Mentorado vinculado."));
    }
}
