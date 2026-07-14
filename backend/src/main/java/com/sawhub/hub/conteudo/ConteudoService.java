package com.sawhub.hub.conteudo;

import com.sawhub.hub.conteudo.dto.AtualizarConteudoRequest;
import com.sawhub.hub.conteudo.dto.CriarConteudoRequest;
import com.sawhub.hub.mentorado.Plano;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** H11.3 — curadoria da biblioteca de conteúdos. */
@Service
public class ConteudoService {

    private final ConteudoRepository conteudoRepository;

    public ConteudoService(ConteudoRepository conteudoRepository) {
        this.conteudoRepository = conteudoRepository;
    }

    @Transactional
    public Conteudo criar(CriarConteudoRequest request) {
        Conteudo conteudo = new Conteudo(request.titulo(), request.tipo(), request.url(), request.planoMinimo());
        conteudo.definirDuracaoMinutos(request.duracaoMinutos());
        return conteudoRepository.save(conteudo);
    }

    public List<Conteudo> listar(TipoConteudo tipo, Plano planoMinimo, Boolean publicado) {
        return conteudoRepository.buscarComFiltro(tipo, planoMinimo, publicado);
    }

    @Transactional
    public Conteudo atualizar(UUID id, AtualizarConteudoRequest request) {
        Conteudo conteudo = buscar(id);
        conteudo.atualizar(request.titulo(), request.tipo(), request.url(), request.planoMinimo());
        conteudo.definirDuracaoMinutos(request.duracaoMinutos());
        return conteudoRepository.save(conteudo);
    }

    @Transactional
    public Conteudo publicar(UUID id) {
        Conteudo conteudo = buscar(id);
        conteudo.publicar();
        return conteudoRepository.save(conteudo);
    }

    @Transactional
    public Conteudo despublicar(UUID id) {
        Conteudo conteudo = buscar(id);
        conteudo.despublicar();
        return conteudoRepository.save(conteudo);
    }

    private Conteudo buscar(UUID id) {
        return conteudoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conteúdo não encontrado."));
    }
}
