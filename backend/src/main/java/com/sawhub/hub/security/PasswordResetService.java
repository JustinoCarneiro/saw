package com.sawhub.hub.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** H1.4 (M18) — ver Suposições do Blueprint M18 no ROADMAP.md: token de alta entropia, hash
 * armazenado (nunca o valor bruto), uso único, validade de 30 minutos, resposta sempre genérica
 * (nunca revela se o e-mail existe — mesmo princípio do login/H1.1). */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final Duration VALIDADE = Duration.ofMinutes(30);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public PasswordResetService(UsuarioRepository usuarioRepository, PasswordResetTokenRepository tokenRepository,
                                 PasswordEncoder passwordEncoder, EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    // Nunca lança exceção nem retorna informação sobre se o e-mail existe — chamador (controller)
    // sempre responde 200 com a mesma mensagem, exista ou não a conta (Suposição 4 do Blueprint).
    // Achado do revisor-seguranca: sem o try/catch, uma falha de envio (SMTP fora do ar, caixa
    // rejeitando, etc. — quando houver credencial real configurada) propagava como exceção não
    // tratada e virava 500 só pra e-mails que EXISTEM, um oráculo de enumeração de contas
    // (e-mail inexistente sempre 200, e-mail existente com falha de envio vira 500).
    @Transactional
    public void solicitar(String email) {
        usuarioRepository.findByEmail(email).ifPresent(usuario -> {
            String tokenBruto = gerarTokenBruto();
            PasswordResetToken token = new PasswordResetToken(usuario, hash(tokenBruto), Instant.now().plus(VALIDADE));
            tokenRepository.save(token);
            try {
                emailService.enviarLinkRedefinicaoSenha(usuario.getEmail(), tokenBruto);
            } catch (RuntimeException e) {
                log.error("Falha ao enviar e-mail de redefinição de senha — token já foi gerado e continua "
                        + "válido, mas o usuário pode não ter recebido o link.", e);
            }
        });
    }

    @Transactional
    public void redefinir(String tokenBruto, String novaSenha) {
        PasswordResetToken token = tokenRepository.findByTokenHash(hash(tokenBruto))
                .filter(PasswordResetToken::isValido)
                .orElseThrow(() -> new IllegalArgumentException("Link inválido ou expirado."));

        Usuario usuario = token.getUsuario();
        usuario.atualizarSenha(passwordEncoder.encode(novaSenha));
        token.marcarUsado();

        // Suposição 3 do Blueprint: invalida os outros tokens pendentes do mesmo usuário.
        tokenRepository.findByUsuarioIdAndUsadoEmIsNull(usuario.getId()).forEach(PasswordResetToken::marcarUsado);
    }

    private static String gerarTokenBruto() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // SHA-256 (não BCrypt): o token já tem entropia alta o bastante que o custo computacional do
    // BCrypt não soma segurança real aqui, diferente de senha escolhida por humano (Suposição 2).
    private static String hash(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível na JVM.", e);
        }
    }
}
