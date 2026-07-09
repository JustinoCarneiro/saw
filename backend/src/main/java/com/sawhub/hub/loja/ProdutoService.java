package com.sawhub.hub.loja;

import com.sawhub.hub.loja.dto.AtualizarProdutoRequest;
import com.sawhub.hub.loja.dto.CriarProdutoRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** H8.1 — curadoria do catálogo. Mesmo padrão de ConteudoService (M06/M11): "não encontrado" é
 * IllegalArgumentException (400), convenção admin-only já estabelecida (diferente do 404
 * mentee-facing de ConteudoMentoradoService/EventoMentoradoService). */
@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    public ProdutoService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    @Transactional
    public Produto criar(CriarProdutoRequest request) {
        Produto produto = new Produto(request.titulo(), request.descricao(), request.categoria(), request.preco(),
                request.precoOriginal(), request.avaliacaoMedia(), request.destaque(), request.arquivoUrl(),
                request.imagemUrl());
        return produtoRepository.save(produto);
    }

    public List<Produto> listar(CategoriaProduto categoria, Boolean publicado, Boolean destaque, String busca) {
        return produtoRepository.buscarComFiltro(categoria, publicado, destaque, busca);
    }

    @Transactional
    public Produto atualizar(UUID id, AtualizarProdutoRequest request) {
        Produto produto = buscar(id);
        produto.atualizar(request.titulo(), request.descricao(), request.categoria(), request.preco(),
                request.precoOriginal(), request.avaliacaoMedia(), request.destaque(), request.arquivoUrl(),
                request.imagemUrl());
        return produtoRepository.save(produto);
    }

    @Transactional
    public Produto publicar(UUID id) {
        Produto produto = buscar(id);
        produto.publicar();
        return produtoRepository.save(produto);
    }

    @Transactional
    public Produto despublicar(UUID id) {
        Produto produto = buscar(id);
        produto.despublicar();
        return produtoRepository.save(produto);
    }

    private Produto buscar(UUID id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado."));
    }
}
