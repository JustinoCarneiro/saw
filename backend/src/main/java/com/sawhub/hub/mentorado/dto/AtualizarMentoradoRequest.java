package com.sawhub.hub.mentorado.dto;

import com.sawhub.hub.mentorado.Plano;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

// vencimentoPlano nullable (M15/E9, H9.3): setado pelo Admin junto do plano, opcional — nem todo
// plano/mentorado tem data de vencimento definida ainda nesta leva.
// telefone/bio/areasInteresse/fotoUrl (Fase 5, H11.1): mesmos limites/validação de
// AtualizarPerfilMentoradoRequest (autoedição, H9.1) — Admin e o próprio mentorado escrevem nos
// mesmos campos, então as regras precisam ser as mesmas dos dois lados.
public record AtualizarMentoradoRequest(
        @NotBlank @Size(max = 255) String nome,
        @Size(max = 255) String negocio,
        @NotNull Plano plano,
        LocalDate vencimentoPlano,
        @Size(max = 30) String telefone,
        @Size(max = 500) String bio,
        @Size(max = 10) List<@NotBlank @Size(max = 50) String> areasInteresse,
        @Size(max = 500) @Pattern(regexp = "^https?://.+") String fotoUrl
) {
}
