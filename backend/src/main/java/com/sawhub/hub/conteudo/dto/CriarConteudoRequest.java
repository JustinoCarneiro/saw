package com.sawhub.hub.conteudo.dto;

import com.sawhub.hub.conteudo.TipoConteudo;
import com.sawhub.hub.mentorado.Plano;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CriarConteudoRequest(
        @NotBlank @Size(max = 255) String titulo,
        @NotNull TipoConteudo tipo,
        // Achado do revisor-seguranca no M11: esta url passou a ser renderizada como link
        // clicável/window.open() direto pro mentorado (MateriaisPage) — sem restringir o esquema,
        // um Admin comprometido podia gravar "javascript:..." e obter execução no contexto
        // autenticado do mentorado que clicasse.
        @NotBlank @Size(max = 500) @Pattern(regexp = "^https?://.+") String url,
        Plano planoMinimo
) {
}
