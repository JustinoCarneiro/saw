package com.sawhub.hub.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sawhub.hub.team.Area;
import com.sawhub.hub.team.Colaborador;
import com.sawhub.hub.team.ColaboradorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Achado H1 da revisão de segurança: este é o único lugar que cria a primeira conta
 * ADMIN do sistema a partir de env vars sem default (bootstrap-fundador-senha falha o
 * boot se ausente). O teste fixa o comportamento idempotente — nunca recriar o Fundador
 * se a tabela usuario já tiver alguém.
 */
@ExtendWith(MockitoExtension.class)
class FundadorBootstrapTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ColaboradorRepository colaboradorRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private FundadorBootstrap bootstrap() {
        FundadorBootstrap bootstrap = new FundadorBootstrap(usuarioRepository, colaboradorRepository, passwordEncoder);
        ReflectionTestUtils.setField(bootstrap, "fundadorNome", "Matheus Brayan");
        ReflectionTestUtils.setField(bootstrap, "fundadorEmail", "matheus@sawhub.com.br");
        ReflectionTestUtils.setField(bootstrap, "fundadorSenha", "senha-vinda-da-env");
        return bootstrap;
    }

    @Test
    void naoFazNadaSeJaExistirQualquerUsuario() {
        when(usuarioRepository.count()).thenReturn(1L);

        bootstrap().run(null);

        verify(usuarioRepository, never()).save(any());
        verify(colaboradorRepository, never()).save(any());
    }

    @Test
    void criaFundadorComAreaFundadorESenhaCodificadaQuandoTabelaVazia() {
        when(usuarioRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("senha-vinda-da-env")).thenReturn("hash-codificado");
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        bootstrap().run(null);

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        assertThat(usuarioCaptor.getValue().getEmail()).isEqualTo("matheus@sawhub.com.br");
        assertThat(usuarioCaptor.getValue().getPasswordHash()).isEqualTo("hash-codificado");
        assertThat(usuarioCaptor.getValue().getPerfil()).isEqualTo(Perfil.ADMIN);

        ArgumentCaptor<Colaborador> colaboradorCaptor = ArgumentCaptor.forClass(Colaborador.class);
        verify(colaboradorRepository).save(colaboradorCaptor.capture());
        assertThat(colaboradorCaptor.getValue().getNome()).isEqualTo("Matheus Brayan");
        assertThat(colaboradorCaptor.getValue().getArea()).isEqualTo(Area.FUNDADOR);
    }
}
