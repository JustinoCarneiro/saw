package com.sawhub.hub.auth;

import com.sawhub.hub.security.AppUserPrincipal;
import com.sawhub.hub.security.GoogleOAuthProperties;
import com.sawhub.hub.security.MeResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login e logout são tratados pelos filtros do Spring Security (ver SecurityConfig) — este
 * controller só existe para o /me, que precisa de lógica de aplicação (montar a resposta a
 * partir do principal da sessão).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final GoogleOAuthProperties googleOAuthProperties;

    public AuthController(GoogleOAuthProperties googleOAuthProperties) {
        this.googleOAuthProperties = googleOAuthProperties;
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal AppUserPrincipal principal) {
        return MeResponse.from(principal);
    }

    /** Público (M07) — pro frontend só mostrar "Entrar com Google" quando o backend realmente
     * tem credencial configurada, evitando um botão morto em dev/demo sem app OAuth registrado. */
    @GetMapping("/oauth2-config")
    public OAuth2ConfigResponse oauth2Config() {
        return new OAuth2ConfigResponse(googleOAuthProperties.isEnabled());
    }

    public record OAuth2ConfigResponse(boolean googleEnabled) {
    }
}
