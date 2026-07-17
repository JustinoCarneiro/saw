package com.sawhub.hub.meta;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MetaRepository extends JpaRepository<Meta, UUID> {

    // status nulo = "Todas" (H3.2) — mesmo padrão de filtro opcional já usado em
    // MentoriaRepository.buscarPorStatus.
    @Query("SELECT m FROM Meta m WHERE m.mentorado.id = :mentoradoId "
            + "AND (:status IS NULL OR m.status = :status) "
            + "ORDER BY m.prazo ASC")
    List<Meta> buscarPorMentorado(@Param("mentoradoId") UUID mentoradoId, @Param("status") StatusMeta status);

    // Fase 5 — achado ao vivo: findAll() (usado por MetaCsvService.exportar() e pela listagem
    // admin nova) deixa `mentorado`/`mentorado.usuario` como proxy LAZY não inicializado; como
    // nenhum dos dois chamadores roda dentro de uma transação, acessar esses campos fora da
    // consulta sempre lançava LazyInitializationException (GET /admin/metas/export retornava 500
    // em produção, nunca coberto por E2E). Mesma classe de bug já corrigida em
    // EncaminhamentoRepository.buscarPorIdComMeta/buscarPorMentorado.
    @Query("SELECT m FROM Meta m LEFT JOIN FETCH m.mentorado men LEFT JOIN FETCH men.usuario ORDER BY m.prazo ASC")
    List<Meta> listarTodasComMentorado();
}
