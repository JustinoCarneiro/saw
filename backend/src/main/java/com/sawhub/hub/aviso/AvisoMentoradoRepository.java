package com.sawhub.hub.aviso;

import com.sawhub.hub.mentorado.Plano;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
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
        WHERE a.planoMinimo IN :planos
        AND (:categoria IS NULL OR a.categoria = :categoria)
        ORDER BY a.criadoEm DESC
    """)
    List<Object[]> buscarParaMentorado(
        @Param("mentoradoId") UUID mentoradoId,
        @Param("planos") List<Plano> planos,
        @Param("categoria") CategoriaAviso categoria
    );

    Optional<AvisoMentorado> findByMentoradoIdAndAvisoId(UUID mentoradoId, UUID avisoId);
}
