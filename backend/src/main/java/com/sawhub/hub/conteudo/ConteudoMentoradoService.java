package com.sawhub.hub.conteudo;

import com.sawhub.hub.conteudo.dto.AtualizarConteudoMentoradoRequest;
import com.sawhub.hub.conteudo.dto.ConteudoMentoradoResponse;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.mentorado.Plano;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
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
        List<Plano> planosPermitidos = planosPermitidos(mentorado.getPlano());
        List<Object[]> rows = conteudoMentoradoRepository.buscarCatalogo(mentorado.getId(), planosPermitidos, tipo, favorito);
        return rows.stream().map(this::mapToResponse).toList();
    }

    public List<ConteudoMentoradoResponse> buscarDicas(UUID usuarioId) {
        Mentorado mentorado = resolverMentorado(usuarioId);
        List<Plano> planosPermitidos = planosPermitidos(mentorado.getPlano());
        List<Object[]> rows = conteudoMentoradoRepository.buscarDicas(mentorado.getId(), planosPermitidos);
        return rows.stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public ConteudoMentoradoResponse atualizarStatus(UUID usuarioId, UUID conteudoId, AtualizarConteudoMentoradoRequest request) {
        Mentorado mentorado = resolverMentorado(usuarioId);
        Conteudo conteudo = conteudoRepository.findById(conteudoId)
                .filter(Conteudo::isPublicado)
                .orElseThrow(() -> new NoSuchElementException("Conteúdo não encontrado."));

        if (!mentorado.getPlano().atendePlanoMinimo(conteudo.getPlanoMinimo())) {
            throw new AccessDeniedException("Seu plano não permite acesso a este conteúdo.");
        }

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

    private Mentorado resolverMentorado(UUID usuarioId) {
        return mentoradoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new IllegalStateException("Mentorado não encontrado para o usuário autenticado."));
    }

    // Lista completa (não só um boolean) porque buscarCatalogo/buscarDicas passam pra JPQL como
    // parâmetro de "IN" — comparação em si centralizada em Plano.atendePlanoMinimo() (achado do
    // revisor-seguranca no M11: evita uma segunda cópia independente do mesmo cálculo).
    private List<Plano> planosPermitidos(Plano planoAtual) {
        return Arrays.stream(Plano.values())
                .filter(planoAtual::atendePlanoMinimo)
                .toList();
    }

    private ConteudoMentoradoResponse mapToResponse(Object[] row) {
        Conteudo c = (Conteudo) row[0];
        ConteudoMentorado cm = (ConteudoMentorado) row[1];
        return ConteudoMentoradoResponse.from(c, cm);
    }
}
