package com.sawhub.hub.mentorado.dto;

import com.sawhub.hub.common.AreasInteresseUtil;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.Plano;
import com.sawhub.hub.mentorado.StatusMentorado;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// H11.1 (Fase 5) — telefone/bio/areasInteresse/fotoUrl passam a ser também editáveis pelo Admin
// (antes só autoedição do mentorado, H9.1) — o Admin precisa conseguir completar o cadastro sem
// depender do mentorado logar primeiro. Autoedição continua existindo em paralelo.
public record MentoradoResponse(
        UUID id,
        String nome,
        String email,
        String negocio,
        Plano plano,
        LocalDate vencimentoPlano,
        StatusMentorado status,
        String telefone,
        String bio,
        List<String> areasInteresse,
        String fotoUrl,
        Instant criadoEm
) {
    public static MentoradoResponse from(Mentorado m) {
        return new MentoradoResponse(m.getId(), m.getNome(), m.getUsuario().getEmail(), m.getNegocio(),
                m.getPlano(), m.getVencimentoPlano(), m.getStatus(), m.getTelefone(), m.getBio(),
                AreasInteresseUtil.parse(m.getAreasInteresse()), m.getFotoUrl(), m.getCriadoEm());
    }
}
