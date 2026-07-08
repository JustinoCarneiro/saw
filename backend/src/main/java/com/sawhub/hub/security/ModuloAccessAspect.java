package com.sawhub.hub.security;

import com.sawhub.hub.team.AreaModuloMatrix;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Intercepta qualquer método (controller ou serviço) anotado com {@link RequiresModulo} e barra
 * a chamada se a área do colaborador logado não tiver aquele módulo liberado. A exceção lançada
 * é pega automaticamente pelo Spring Security (ExceptionTranslationFilter) e vira 403 — nenhuma
 * configuração extra necessária, funciona tanto em controllers quanto em métodos de serviço
 * chamados por fora de uma requisição HTTP direta (ex.: um job agendado).
 */
@Aspect
@Component
public class ModuloAccessAspect {

    @Before("@annotation(requiresModulo) || @within(requiresModulo)")
    public void checarModulo(RequiresModulo requiresModulo) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication != null && authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            throw new AccessDeniedException("Não autenticado.");
        }
        if (principal.getArea() == null || !AreaModuloMatrix.isAllowed(principal.getArea(), requiresModulo.value())) {
            throw new AccessDeniedException("Sua área não tem acesso a este módulo.");
        }
    }
}
