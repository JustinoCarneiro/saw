package com.sawhub.hub.security;

import com.sawhub.hub.team.Modulo;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca um controller/método que exige acesso a um módulo específico (matriz de área do E15).
 * Enforced pelo {@link ModuloAccessAspect}. Complementa (não substitui) o gate grosseiro de
 * perfil (ROLE_ADMIN/ROLE_MENTORADO) já feito no filter chain.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresModulo {
    Modulo value();
}
