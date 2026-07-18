package com.sawhub.hub.perfil;

import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.mentorado.Plano;
import com.sawhub.hub.perfil.dto.AssinaturaResponse;
import com.sawhub.hub.perfil.dto.AtualizarPerfilMentoradoRequest;
import com.sawhub.hub.perfil.dto.PerfilMentoradoResponse;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** H9.1 (perfil) e H9.3 (assinatura) — leitura/escrita direta do {@link Mentorado} já
 * resolvido, sem agregação de outros módulos (ver {@link PerfilJornadaService} pra H9.2, que
 * cruza dado de vários módulos). */
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

    // H9.3 — Suposição 5 do Blueprint M15: informativo (vê plano/vencimento/opções), troca de
    // plano de fato continua ação do Admin (AtualizarMentoradoRequest).
    public AssinaturaResponse assinatura(UUID usuarioId) {
        Mentorado mentorado = resolverMentorado(usuarioId);
        List<AssinaturaResponse.PlanoDisponivel> disponiveis = Arrays.stream(Plano.values())
                .map(p -> new AssinaturaResponse.PlanoDisponivel(p, p.ordinal() > mentorado.getPlano().ordinal()))
                .toList();
        return new AssinaturaResponse(mentorado.getPlano(), mentorado.getVencimentoPlano(), disponiveis);
    }

    private Mentorado resolverMentorado(UUID usuarioId) {
        return mentoradoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new IllegalStateException(
                        "Usuário autenticado (id=" + usuarioId + ") não tem Mentorado vinculado."));
    }
}
