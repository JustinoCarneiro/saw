package com.sawhub.hub.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.session.data.redis.config.ConfigureRedisAction;

@Configuration
public class RedisSessionConfig {

    /**
     * Sessão serializada em JSON (não serialização binária do Java) — inspecionável via
     * redis-cli/RedisInsight e imune a drift de serialVersionUID entre deploys.
     *
     * SecurityJackson2Modules registra os mixins que o próprio Spring Security precisa pra
     * (de)serializar suas classes internas guardadas na sessão (SecurityContextImpl,
     * UsernamePasswordAuthenticationToken, SimpleGrantedAuthority) — sem isso o Jackson não
     * consegue reconstruí-las (não têm construtor padrão).
     */
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModules(SecurityJackson2Modules.getModules(getClass().getClassLoader()));
        // Achado M3 da revisão de segurança: sem isto, o GenericJackson2JsonRedisSerializer usa
        // o PolymorphicTypeValidator "laissez-faire" (aceita reconstruir QUALQUER classe do
        // classpath a partir do campo "@class") — quem escrever no Redis (ex.: porta exposta sem
        // auth) poderia forjar um valor de sessão apontando pra uma classe gadget. Isso troca por
        // uma allowlist restrita às classes que o Spring Security realmente precisa.
        SecurityJackson2Modules.enableDefaultTyping(mapper);
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    /**
     * Redis gerenciado (Hostinger/Coolify em produção) normalmente não permite CONFIG SET —
     * evita o Spring Session tentar reconfigurar notificações de keyspace no boot.
     */
    @Bean
    public ConfigureRedisAction configureRedisAction() {
        return ConfigureRedisAction.NO_OP;
    }
}
