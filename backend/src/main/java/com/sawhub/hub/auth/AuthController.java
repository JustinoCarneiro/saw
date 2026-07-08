package com.sawhub.hub.auth;

import com.sawhub.hub.security.AppUserPrincipal;
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

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal AppUserPrincipal principal) {
        return MeResponse.from(principal);
    }
}
