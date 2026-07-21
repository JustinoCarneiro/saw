package com.sawhub.hub.aviso;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AvisoMentoradoRepository extends JpaRepository<AvisoMentorado, AvisoMentoradoId> {

    // Mesmo formato de ConteudoMentoradoRepository.buscarCatalogo (M11): LEFT JOIN pra que um
    // aviso sem linha ainda (mentorado nunca interagiu) apareça como "não lido" por padrão, sem
    // precisar criar a linha antecipadamente pra cada mentorado-aviso.
    @Query("""
        SELECT a, am
        FROM Aviso a
        LEFT JOIN AvisoMentorado am ON am.aviso = a AND am.mentorado.id = :mentoradoId
        WHERE (:categoria IS NULL OR a.categoria = :categoria)
        ORDER BY a.criadoEm DESC
    """)
    List<Object[]> buscarParaMentorado(
        @Param("mentoradoId") UUID mentoradoId,
        @Param("categoria") CategoriaAviso categoria
    );

    Optional<AvisoMentorado> findByMentoradoIdAndAvisoId(UUID mentoradoId, UUID avisoId);

    // FK aviso_mentorado.aviso_id não tem ON DELETE CASCADE — excluir um aviso lido por algum
    // mentorado sem isso quebraria com violação de FK.
    @Modifying
    @Query("DELETE FROM AvisoMentorado am WHERE am.aviso.id = :avisoId")
    void deleteAllByAvisoId(@Param("avisoId") UUID avisoId);
}
