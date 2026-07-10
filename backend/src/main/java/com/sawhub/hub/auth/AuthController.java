package com.sawhub.hub.auth;

import com.sawhub.hub.security.AppUserPrincipal;
import com.sawhub.hub.security.GoogleOAuthProperties;
import com.sawhub.hub.security.MeResponse;
import com.sawhub.hub.security.PasswordResetService;
import com.sawhub.hub.security.dto.EsqueciSenhaRequest;
import com.sawhub.hub.security.dto.RedefinirSenhaRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login e logout são tratados pelos filtros do Spring Security (ver SecurityConfig) — este
 * controller existe pro /me (monta a resposta a partir do principal da sessão) e, desde o M18,
 * pro fluxo de recuperação de senha (H1.4) — os dois únicos endpoints POST aqui que não exigem
 * sessão (ver SecurityConfig.permitAll + PasswordResetRateLimitFilter).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String MENSAGEM_ESQUECI_SENHA_GENERICA =
            "Se esse e-mail existir na nossa base, você vai receber um link de redefinição.";

    private final GoogleOAuthProperties googleOAuthProperties;
    private final PasswordResetService passwordResetService;

    public AuthController(GoogleOAuthProperties googleOAuthProperties, PasswordResetService passwordResetService) {
        this.googleOAuthProperties = googleOAuthProperties;
        this.passwordResetService = passwordResetService;
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

    /** H1.4 (M18) — sempre 200 com a mesma mensagem, exista ou não o e-mail (Suposição 4 do
     * Blueprint M18: nunca revela se a conta existe, mesmo princípio do login). */
    @PostMapping("/esqueci-senha")
    public MensagemResponse esqueciSenha(@Valid @RequestBody EsqueciSenhaRequest request) {
        passwordResetService.solicitar(request.email());
        return new MensagemResponse(MENSAGEM_ESQUECI_SENHA_GENERICA);
    }

    /** H1.4 (M18) — token inválido/expirado/já usado vira 400 genérico (IllegalArgumentException,
     * já mapeado em GlobalExceptionHandler), sem distinguir qual dos três casos aconteceu. */
    @PostMapping("/redefinir-senha")
    public MensagemResponse redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest request) {
        passwordResetService.redefinir(request.token(), request.novaSenha());
        return new MensagemResponse("Senha redefinida com sucesso.");
    }

    public record OAuth2ConfigResponse(boolean googleEnabled) {
    }

    public record MensagemResponse(String message) {
    }
}
