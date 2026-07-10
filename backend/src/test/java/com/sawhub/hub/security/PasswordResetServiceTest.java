package com.sawhub.hub.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/** H1.4 — RED primeiro: PasswordResetService ainda não existia neste ponto do ciclo. */
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;

    private PasswordResetService service() {
        return new PasswordResetService(usuarioRepository, tokenRepository, passwordEncoder, emailService);
    }

    private static Usuario usuario(UUID id, String email) {
        Usuario u = new Usuario(email, "hash-antigo", Perfil.MENTORADO);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private static PasswordResetToken token(Usuario usuario, Instant expiraEm, Instant usadoEm) {
        PasswordResetToken t = new PasswordResetToken(usuario, "hash-qualquer", expiraEm);
        ReflectionTestUtils.setField(t, "id", UUID.randomUUID());
        if (usadoEm != null) {
            ReflectionTestUtils.setField(t, "usadoEm", usadoEm);
        }
        return t;
    }

    @Test
    void solicitarComEmailExistenteCriaTokenEEnviaEmail() {
        Usuario usuario = usuario(UUID.randomUUID(), "ana@x.com");
        when(usuarioRepository.findByEmail("ana@x.com")).thenReturn(Optional.of(usuario));

        service().solicitar("ana@x.com");

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(captor.capture());
        PasswordResetToken salvo = captor.getValue();
        assertThat(salvo.getUsuario()).isEqualTo(usuario);
        assertThat(salvo.isValido()).isTrue();
        assertThat(salvo.getExpiraEm()).isAfter(Instant.now().plusSeconds(1740)); // ~29min
        assertThat(salvo.getExpiraEm()).isBefore(Instant.now().plusSeconds(1860)); // ~31min

        verify(emailService).enviarLinkRedefinicaoSenha(eq("ana@x.com"), anyString());
    }

    // Achado do revisor-seguranca (M18): sem o try/catch, uma falha de envio (SMTP fora do ar,
    // caixa rejeitando) propagava como exceção não tratada, virando 500 só pra e-mails que
    // EXISTEM — oráculo de enumeração de contas (e-mail inexistente sempre "sucesso silencioso",
    // e-mail existente com falha de envio quebrava diferente). O token continua salvo mesmo se o
    // envio falhar.
    @Test
    void solicitarNaoPropagaExcecaoQuandoEnvioDeEmailFalha() {
        Usuario usuario = usuario(UUID.randomUUID(), "ana@x.com");
        when(usuarioRepository.findByEmail("ana@x.com")).thenReturn(Optional.of(usuario));
        org.mockito.Mockito.doThrow(new RuntimeException("SMTP fora do ar"))
                .when(emailService).enviarLinkRedefinicaoSenha(any(), any());

        service().solicitar("ana@x.com");

        verify(tokenRepository).save(any());
    }

    @Test
    void solicitarComEmailInexistenteNaoFalhaENaoEnviaEmail() {
        when(usuarioRepository.findByEmail("fantasma@x.com")).thenReturn(Optional.empty());

        service().solicitar("fantasma@x.com");

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).enviarLinkRedefinicaoSenha(any(), any());
    }

    @Test
    void redefinirComTokenValidoAtualizaSenhaEMarcaTokenUsado() {
        Usuario usuario = usuario(UUID.randomUUID(), "ana@x.com");
        PasswordResetToken token = token(usuario, Instant.now().plusSeconds(600), null);
        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));
        when(tokenRepository.findByUsuarioIdAndUsadoEmIsNull(usuario.getId())).thenReturn(List.of());
        when(passwordEncoder.encode("novaSenha123")).thenReturn("hash-novo");

        service().redefinir("token-bruto-qualquer", "novaSenha123");

        assertThat(usuario.getPasswordHash()).isEqualTo("hash-novo");
        assertThat(token.getUsadoEm()).isNotNull();
    }

    @Test
    void redefinirInvalidaOutrosTokensPendentesDoMesmoUsuario() {
        Usuario usuario = usuario(UUID.randomUUID(), "ana@x.com");
        PasswordResetToken tokenUsado = token(usuario, Instant.now().plusSeconds(600), null);
        PasswordResetToken outroPendente = token(usuario, Instant.now().plusSeconds(600), null);
        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.of(tokenUsado));
        when(tokenRepository.findByUsuarioIdAndUsadoEmIsNull(usuario.getId())).thenReturn(List.of(outroPendente));
        when(passwordEncoder.encode(anyString())).thenReturn("hash-novo");

        service().redefinir("token-bruto", "novaSenha123");

        assertThat(outroPendente.getUsadoEm()).isNotNull();
    }

    @Test
    void redefinirComTokenInexistenteLancaIllegalArgumentException() {
        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().redefinir("token-invalido", "novaSenha123"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void redefinirComTokenExpiradoLancaIllegalArgumentException() {
        Usuario usuario = usuario(UUID.randomUUID(), "ana@x.com");
        PasswordResetToken expirado = token(usuario, Instant.now().minusSeconds(60), null);
        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expirado));

        assertThatThrownBy(() -> service().redefinir("token-expirado", "novaSenha123"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(tokenRepository, never()).save(any());
    }

    @Test
    void redefinirComTokenJaUsadoLancaIllegalArgumentException() {
        Usuario usuario = usuario(UUID.randomUUID(), "ana@x.com");
        PasswordResetToken jaUsado = token(usuario, Instant.now().plusSeconds(600), Instant.now().minusSeconds(60));
        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.of(jaUsado));

        assertThatThrownBy(() -> service().redefinir("token-ja-usado", "novaSenha123"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
