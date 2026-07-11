package com.sawhub.hub.security;

import com.sawhub.hub.team.Area;
import com.sawhub.hub.team.Colaborador;
import com.sawhub.hub.team.ColaboradorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * A rota de criação de colaborador é @RequiresModulo(TIME), só Fundador acessa — sem isso,
 * ninguém consegue criar a primeira conta pela API. Roda só se a tabela usuario estiver vazia;
 * não commitamos hash de senha real em migration. @Order(1): precisa rodar antes do
 * DemoDataSeeder, que assume o Fundador já existe.
 */
@Component
@Order(1)
public class FundadorBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FundadorBootstrap.class);

    private final UsuarioRepository usuarioRepository;
    private final ColaboradorRepository colaboradorRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${sawhub.bootstrap.fundador-nome}")
    private String fundadorNome;

    @Value("${sawhub.bootstrap.fundador-email}")
    private String fundadorEmail;

    @Value("${sawhub.bootstrap.fundador-senha}")
    private String fundadorSenha;

    public FundadorBootstrap(UsuarioRepository usuarioRepository, ColaboradorRepository colaboradorRepository,
                              PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.colaboradorRepository = colaboradorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarioRepository.count() > 0) {
            return;
        }
        Usuario usuario = usuarioRepository.save(
                new Usuario(fundadorEmail, passwordEncoder.encode(fundadorSenha), Perfil.ADMIN));
        colaboradorRepository.save(new Colaborador(usuario, fundadorNome, Area.FUNDADOR));
        log.warn("Fundador inicial criado: {} — troque a senha padrão assim que possível.", fundadorEmail);
    }
}
