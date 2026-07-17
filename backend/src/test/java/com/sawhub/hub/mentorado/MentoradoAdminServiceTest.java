package com.sawhub.hub.mentorado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.sawhub.hub.comercial.Lead;
import com.sawhub.hub.comercial.LeadRepository;
import com.sawhub.hub.mentorado.dto.AtualizarMentoradoRequest;
import com.sawhub.hub.security.Usuario;
import com.sawhub.hub.security.UsuarioRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/** H11.1 — RED primeiro: MentoradoAdminService ainda não existe neste ponto do ciclo. */
@ExtendWith(MockitoExtension.class)
class MentoradoAdminServiceTest {

    @Mock
    private MentoradoRepository mentoradoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private LeadRepository leadRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private MentoradoAdminService service() {
        return new MentoradoAdminService(mentoradoRepository, usuarioRepository, leadRepository, passwordEncoder);
    }

    private static Lead leadFechado(Plano planoFechado) {
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null, null);
        Usuario vendedorUsuario = null;
        lead.moverParaEmContato(new com.sawhub.hub.team.Colaborador(vendedorUsuario, "Paula",
                com.sawhub.hub.team.Area.COMERCIAL));
        lead.moverParaProposta();
        lead.fechar(planoFechado);
        return lead;
    }

    @Test
    void atualizarMudaNomeNegocioEPlano() {
        UUID id = UUID.randomUUID();
        Mentorado mentorado = new Mentorado(null, "Antigo", "Restaurante Antigo", Plano.GRATUITO,
                java.math.BigDecimal.ZERO, 0, 0);
        when(mentoradoRepository.findById(id)).thenReturn(Optional.of(mentorado));
        when(mentoradoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new AtualizarMentoradoRequest("Novo Nome", "Novo Negócio", Plano.ESSENCIAL,
                java.time.LocalDate.of(2026, 12, 1), null, null, null, null);
        Mentorado atualizado = service().atualizar(id, request);

        assertThat(atualizado.getNome()).isEqualTo("Novo Nome");
        assertThat(atualizado.getPlano()).isEqualTo(Plano.ESSENCIAL);
        assertThat(atualizado.getVencimentoPlano()).isEqualTo(java.time.LocalDate.of(2026, 12, 1));
    }

    @Test
    void atualizarTambemGravaContatoBioAreasEFoto() {
        UUID id = UUID.randomUUID();
        Mentorado mentorado = new Mentorado(null, "Antigo", "Restaurante Antigo", Plano.GRATUITO,
                java.math.BigDecimal.ZERO, 0, 0);
        when(mentoradoRepository.findById(id)).thenReturn(Optional.of(mentorado));
        when(mentoradoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new AtualizarMentoradoRequest("Novo Nome", "Novo Negócio", Plano.ESSENCIAL, null,
                "11999998888", "Bio preenchida pelo Admin", java.util.List.of("Delivery", "Marketing"),
                "https://exemplo.com/foto.jpg");
        Mentorado atualizado = service().atualizar(id, request);

        assertThat(atualizado.getTelefone()).isEqualTo("11999998888");
        assertThat(atualizado.getBio()).isEqualTo("Bio preenchida pelo Admin");
        assertThat(atualizado.getAreasInteresse()).isEqualTo("Delivery, Marketing");
        assertThat(atualizado.getFotoUrl()).isEqualTo("https://exemplo.com/foto.jpg");
    }

    @Test
    void desativarMudaStatus() {
        UUID id = UUID.randomUUID();
        Mentorado mentorado = new Mentorado(null, "Nome", null, Plano.GRATUITO, java.math.BigDecimal.ZERO, 0, 0);
        when(mentoradoRepository.findById(id)).thenReturn(Optional.of(mentorado));
        when(mentoradoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Mentorado desativado = service().desativar(id);

        assertThat(desativado.getStatus()).isEqualTo(StatusMentorado.INATIVO);
    }

    @Test
    void criarAPartirDeLeadFechadoFuncionaEVinculaOLead() {
        UUID leadId = UUID.randomUUID();
        Lead lead = leadFechado(Plano.ESSENCIAL);
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(usuarioRepository.findByEmail(lead.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mentoradoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resultado = service().criarAPartirDeLead(leadId);

        assertThat(resultado.mentorado().getNome()).isEqualTo("Maria Souza");
        assertThat(resultado.mentorado().getPlano()).isEqualTo(Plano.ESSENCIAL);
        assertThat(resultado.senhaTemporaria()).isNotBlank();
        assertThat(lead.getMentorado()).isEqualTo(resultado.mentorado());
    }

    @Test
    void criarAPartirDeLeadNaoFechadoLancaErro() {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null, null);
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));

        assertThatThrownBy(() -> service().criarAPartirDeLead(leadId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Fechado");
    }

    @Test
    void criarAPartirDeLeadJaVinculadoLancaErro() {
        UUID leadId = UUID.randomUUID();
        Lead lead = leadFechado(Plano.BASICO);
        Mentorado jaVinculado = new Mentorado(null, "X", null, Plano.BASICO, java.math.BigDecimal.ZERO, 0, 0);
        ReflectionTestUtils.setField(jaVinculado, "id", UUID.randomUUID());
        lead.vincularMentorado(jaVinculado);
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));

        assertThatThrownBy(() -> service().criarAPartirDeLead(leadId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("vinculado");
    }

    @Test
    void criarAPartirDeLeadComEmailJaCadastradoLancaErro() {
        UUID leadId = UUID.randomUUID();
        Lead lead = leadFechado(Plano.BASICO);
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(usuarioRepository.findByEmail(lead.getEmail()))
                .thenReturn(Optional.of(new Usuario(lead.getEmail(), "hash", com.sawhub.hub.security.Perfil.MENTORADO)));

        assertThatThrownBy(() -> service().criarAPartirDeLead(leadId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Já existe uma conta");
    }
}
