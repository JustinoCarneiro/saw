package com.sawhub.hub.aviso;

import com.sawhub.hub.aviso.dto.AvisoMentoradoResponse;
import com.sawhub.hub.aviso.dto.ResumoAvisosResponse;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.mentorado.Plano;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** H12.1 — leitura/estado de avisos do mentorado logado. Mesmo padrão de isolamento por tenant
 * do resto do projeto: {@link Mentorado} sempre resolvido a partir do usuário autenticado. */
@Service
@Transactional(readOnly = true)
public class AvisoMentoradoService {

    private final AvisoMentoradoRepository avisoMentoradoRepository;
    private final AvisoRepository avisoRepository;
    private final MentoradoRepository mentoradoRepository;

    public AvisoMentoradoService(AvisoMentoradoRepository avisoMentoradoRepository, AvisoRepository avisoRepository,
                                  MentoradoRepository mentoradoRepository) {
        this.avisoMentoradoRepository = avisoMentoradoRepository;
        this.avisoRepository = avisoRepository;
        this.mentoradoRepository = mentoradoRepository;
    }

    public List<AvisoMentoradoResponse> listar(UUID usuarioId, CategoriaAviso categoria, Boolean apenasNaoLidos) {
        Mentorado mentorado = resolverMentorado(usuarioId);
        List<Plano> planosPermitidos = planosPermitidos(mentorado.getPlano());
        List<AvisoMentoradoResponse> avisos = avisoMentoradoRepository
                .buscarParaMentorado(mentorado.getId(), planosPermitidos, categoria).stream()
                .map(this::mapToResponse)
                .toList();
        if (Boolean.TRUE.equals(apenasNaoLidos)) {
            return avisos.stream().filter(a -> !a.lido()).toList();
        }
        return avisos;
    }

    public ResumoAvisosResponse resumo(UUID usuarioId) {
        long naoLidos = listar(usuarioId, null, true).size();
        return new ResumoAvisosResponse(naoLidos);
    }

    @Transactional
    public void marcarLido(UUID usuarioId, UUID avisoId) {
        Mentorado mentorado = resolverMentorado(usuarioId);
        Aviso aviso = avisoRepository.findById(avisoId)
                .orElseThrow(() -> new NoSuchElementException("Aviso não encontrado."));

        AvisoMentorado am = avisoMentoradoRepository.findByMentoradoIdAndAvisoId(mentorado.getId(), aviso.getId())
                .orElseGet(() -> new AvisoMentorado(mentorado, aviso));
        am.marcarLido();
        avisoMentoradoRepository.save(am);
    }

    @Transactional
    public void marcarTodosLidos(UUID usuarioId) {
        Mentorado mentorado = resolverMentorado(usuarioId);
        List<Plano> planosPermitidos = planosPermitidos(mentorado.getPlano());
        List<Object[]> rows = avisoMentoradoRepository.buscarParaMentorado(mentorado.getId(), planosPermitidos, null);

        for (Object[] row : rows) {
            Aviso aviso = (Aviso) row[0];
            AvisoMentorado am = (AvisoMentorado) row[1];
            if (am == null) {
                am = new AvisoMentorado(mentorado, aviso);
            } else if (am.isLido()) {
                continue;
            }
            am.marcarLido();
            avisoMentoradoRepository.save(am);
        }
    }

    private AvisoMentoradoResponse mapToResponse(Object[] row) {
        Aviso a = (Aviso) row[0];
        AvisoMentorado am = (AvisoMentorado) row[1];
        return AvisoMentoradoResponse.from(a, am);
    }

    // Mesma centralização de Plano.atendePlanoMinimo() já usada em ConteudoMentoradoService (M11).
    private List<Plano> planosPermitidos(Plano planoAtual) {
        return Arrays.stream(Plano.values())
                .filter(planoAtual::atendePlanoMinimo)
                .toList();
    }

    private Mentorado resolverMentorado(UUID usuarioId) {
        return mentoradoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new IllegalStateException(
                        "Usuário autenticado (id=" + usuarioId + ") não tem Mentorado vinculado."));
    }
}
