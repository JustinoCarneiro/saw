package com.sawhub.hub.mentorado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sawhub.hub.comercial.FormaPagamento;
import com.sawhub.hub.comercial.Lead;
import com.sawhub.hub.comercial.LeadRepository;
import com.sawhub.hub.comercial.OrigemVenda;
import com.sawhub.hub.comercial.ProdutoVenda;
import com.sawhub.hub.mentorado.dto.AtualizarDadosContratoRequest;
import com.sawhub.hub.mentorado.dto.AtualizarDiagnosticoInicialRequest;
import com.sawhub.hub.mentorado.dto.AtualizarMentoradoRequest;
import com.sawhub.hub.mentorado.dto.CriarMentoradoDiretoRequest;
import com.sawhub.hub.mentorado.dto.ImportarMentoradoDiretoLinha;
import com.sawhub.hub.security.Usuario;
import com.sawhub.hub.security.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    private MentoradoDiagnosticoInicialRepository diagnosticoInicialRepository;
    @Mock
    private ContratoDocumentoStorageService contratoDocumentoStorageService;
    @Mock
    private PasswordEncoder passwordEncoder;

    private MentoradoAdminService service() {
        return new MentoradoAdminService(mentoradoRepository, usuarioRepository, leadRepository,
                diagnosticoInicialRepository, contratoDocumentoStorageService, passwordEncoder);
    }

    private static Lead leadFechado() {
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null);
        Usuario vendedorUsuario = null;
        lead.moverParaEmContato(new com.sawhub.hub.team.Colaborador(vendedorUsuario, "Paula",
                com.sawhub.hub.team.Area.COMERCIAL));
        lead.moverParaProposta();
        lead.fecharVenda(ProdutoVenda.MENTORIA_CONTINUA, OrigemVenda.DIRETA, new BigDecimal("18000.00"),
                null, FormaPagamento.PIX);
        return lead;
    }

    // M28 — buscarPorId novo, pra MentoradoDetalhePage abrir direto por URL sem depender de ter
    // vindo da lista.
    @Test
    void buscarPorIdDevolveOMentorado() {
        UUID id = UUID.randomUUID();
        Mentorado mentorado = new Mentorado(null, "Maria Souza", null, Plano.GRATUITO, BigDecimal.ZERO, 0, 0);
        when(mentoradoRepository.findById(id)).thenReturn(Optional.of(mentorado));

        assertThat(service().buscarPorId(id)).isEqualTo(mentorado);
    }

    @Test
    void buscarPorIdInexistenteLancaErro() {
        UUID id = UUID.randomUUID();
        when(mentoradoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().buscarPorId(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não encontrado");
    }

    @Test
    void atualizarMudaNomeNegocioEPlano() {
        UUID id = UUID.randomUUID();
        Mentorado mentorado = new Mentorado(null, "Antigo", "Restaurante Antigo", Plano.GRATUITO,
                java.math.BigDecimal.ZERO, 0, 0);
        when(mentoradoRepository.findById(id)).thenReturn(Optional.of(mentorado));
        when(mentoradoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new AtualizarMentoradoRequest("Novo Nome", "Novo Negócio", Plano.ESSENCIAL,
                java.time.LocalDate.of(2026, 12, 1), null, null, null);
        Mentorado atualizado = service().atualizar(id, request);

        assertThat(atualizado.getNome()).isEqualTo("Novo Nome");
        assertThat(atualizado.getPlano()).isEqualTo(Plano.ESSENCIAL);
        assertThat(atualizado.getVencimentoPlano()).isEqualTo(java.time.LocalDate.of(2026, 12, 1));
    }

    @Test
    void atualizarTambemGravaContatoBioEFoto() {
        UUID id = UUID.randomUUID();
        Mentorado mentorado = new Mentorado(null, "Antigo", "Restaurante Antigo", Plano.GRATUITO,
                java.math.BigDecimal.ZERO, 0, 0);
        when(mentoradoRepository.findById(id)).thenReturn(Optional.of(mentorado));
        when(mentoradoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new AtualizarMentoradoRequest("Novo Nome", "Novo Negócio", Plano.ESSENCIAL, null,
                "11999998888", "Bio preenchida pelo Admin", "https://exemplo.com/foto.jpg");
        Mentorado atualizado = service().atualizar(id, request);

        assertThat(atualizado.getTelefone()).isEqualTo("11999998888");
        assertThat(atualizado.getBio()).isEqualTo("Bio preenchida pelo Admin");
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
        Lead lead = leadFechado();
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(usuarioRepository.findByEmail(lead.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mentoradoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resultado = service().criarAPartirDeLead(leadId);

        assertThat(resultado.mentorado().getNome()).isEqualTo("Maria Souza");
        // M28 — Lead.planoFechado removido junto com Plano; todo mentorado criado a partir de
        // lead nasce com o mesmo default GRATUITO de criarDireto()/criarDiretoDeImportacao().
        assertThat(resultado.mentorado().getPlano()).isEqualTo(Plano.GRATUITO);
        assertThat(resultado.senhaTemporaria()).isNotBlank();
        assertThat(lead.getMentorado()).isEqualTo(resultado.mentorado());
    }

    // M25 — Suposição 6 do Blueprint: quando o Lead foi fechado via fecharVenda() (formulário
    // único de venda) com um produto de mentoria/consultoria, criarAPartirDeLead propaga
    // produtoVenda/valorTotalVenda/dataFechamento pro Mentorado (tipoContrato/valorContrato/
    // dataFechamentoContrato), evitando redigitar o mesmo dado duas vezes.
    @Test
    void criarAPartirDeLeadPropagaDadosDeVendaParaTipoContrato() {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null);
        lead.moverParaEmContato(new com.sawhub.hub.team.Colaborador(null, "Paula", com.sawhub.hub.team.Area.COMERCIAL));
        lead.moverParaProposta();
        lead.fecharVenda(ProdutoVenda.MENTORIA_CONTINUA, OrigemVenda.DIRETA, new BigDecimal("26000.00"),
                new BigDecimal("6000.00"), FormaPagamento.PIX);
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(usuarioRepository.findByEmail(lead.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mentoradoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resultado = service().criarAPartirDeLead(leadId);

        assertThat(resultado.mentorado().getTipoContrato()).isEqualTo(TipoContrato.MENTORIA_CONTINUA);
        assertThat(resultado.mentorado().getValorContrato()).isEqualByComparingTo("26000.00");
        assertThat(resultado.mentorado().getDataFechamentoContrato()).isEqualTo(LocalDate.now(java.time.ZoneOffset.UTC));
    }

    // Venda de ingresso/produto digital não é um contrato de mentoria — não deve preencher
    // tipoContrato (Suposição 6: "só se aplica quando produtoVenda é um dos 3 tipos de mentoria/
    // consultoria").
    @Test
    void criarAPartirDeLeadDeVendaDeIngressoNaoPreencheTipoContrato() {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null);
        lead.moverParaEmContato(new com.sawhub.hub.team.Colaborador(null, "Paula", com.sawhub.hub.team.Area.COMERCIAL));
        lead.moverParaProposta();
        lead.fecharVenda(ProdutoVenda.INGRESSO_EVENTO, OrigemVenda.DIRETA, new BigDecimal("300.00"),
                new BigDecimal("300.00"), FormaPagamento.PIX);
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(usuarioRepository.findByEmail(lead.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mentoradoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resultado = service().criarAPartirDeLead(leadId);

        assertThat(resultado.mentorado().getTipoContrato()).isNull();
        assertThat(resultado.mentorado().getValorContrato()).isNull();
    }

    @Test
    void criarAPartirDeLeadNaoFechadoLancaErro() {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null);
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));

        assertThatThrownBy(() -> service().criarAPartirDeLead(leadId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Fechado");
    }

    @Test
    void criarAPartirDeLeadJaVinculadoLancaErro() {
        UUID leadId = UUID.randomUUID();
        Lead lead = leadFechado();
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
        Lead lead = leadFechado();
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(usuarioRepository.findByEmail(lead.getEmail()))
                .thenReturn(Optional.of(new Usuario(lead.getEmail(), "hash", com.sawhub.hub.security.Perfil.MENTORADO)));

        assertThatThrownBy(() -> service().criarAPartirDeLead(leadId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Já existe uma conta");
    }

    // M23 — "criar mentorado direto" (pedido explícito do cliente): cria Lead já FECHADO,
    // Usuario+Mentorado, e vincula os dois, sem exigir um Lead pré-existente no funil.
    @Test
    void criarDiretoCriaLeadJaFechadoUsuarioEMentoradoVinculados() {
        when(usuarioRepository.findByEmail("dono@restaurante.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mentoradoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new CriarMentoradoDiretoRequest("dono@restaurante.com", "Maria Souza",
                "Menu Caseirinho", "11999998888", TipoContrato.MENTORIA_CONTINUA,
                new BigDecimal("26000.00"), LocalDate.of(2026, 7, 17));
        var resultado = service().criarDireto(request);

        assertThat(resultado.mentorado().getNome()).isEqualTo("Maria Souza");
        assertThat(resultado.mentorado().getNegocio()).isEqualTo("Menu Caseirinho");
        assertThat(resultado.mentorado().getTipoContrato()).isEqualTo(TipoContrato.MENTORIA_CONTINUA);
        assertThat(resultado.mentorado().getValorContrato()).isEqualByComparingTo("26000.00");
        assertThat(resultado.mentorado().getVencimentoContrato()).isEqualTo(LocalDate.of(2027, 7, 17));
        assertThat(resultado.senhaTemporaria()).isNotBlank();
    }

    @Test
    void criarDiretoComEmailJaCadastradoLancaErro() {
        when(usuarioRepository.findByEmail("dono@restaurante.com"))
                .thenReturn(Optional.of(new Usuario("dono@restaurante.com", "hash", com.sawhub.hub.security.Perfil.MENTORADO)));

        var request = new CriarMentoradoDiretoRequest("dono@restaurante.com", "Maria Souza",
                null, null, TipoContrato.CONSULTORIA, null, null);

        assertThatThrownBy(() -> service().criarDireto(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Já existe uma conta");
    }

    // M23 item 4 (bulk-CREATE import, 19/07/2026) — mesma lógica de criarDireto, mas com o
    // conjunto completo de dados que a migração real do Notion carrega.
    @Test
    void criarDiretoDeImportacaoCriaTudoComNomeFantasiaCnpjESocios() {
        when(usuarioRepository.findByEmail("dono@restaurante.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mentoradoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var linha = new ImportarMentoradoDiretoLinha("dono@restaurante.com", "Maria Souza", "Menu Caseirinho",
                "11999998888", TipoContrato.MENTORIA_CONTINUA, new BigDecimal("26000.00"),
                LocalDate.of(2026, 7, 17), "Menu Caseirinho Ltda", "42.521.899/0001-38",
                "Girlandia Aragão de Sousa; Jaene Oliveira de Araujo", null, null, null, null, null, null, null,
                null, null, null);
        var resultado = service().criarDiretoDeImportacao(linha);

        assertThat(resultado.mentorado().getNome()).isEqualTo("Maria Souza");
        assertThat(resultado.mentorado().getNomeFantasia()).isEqualTo("Menu Caseirinho Ltda");
        assertThat(resultado.mentorado().getCnpj()).isEqualTo("42.521.899/0001-38");
        assertThat(resultado.mentorado().getSocios()).contains("Girlandia", "Jaene");
        assertThat(resultado.mentorado().getTipoContrato()).isEqualTo(TipoContrato.MENTORIA_CONTINUA);
        assertThat(resultado.senhaTemporaria()).isNotBlank();
        verifyNoInteractions(diagnosticoInicialRepository);
    }

    @Test
    void criarDiretoDeImportacaoComDadosDeDiagnosticoTambemCriaDiagnosticoInicial() {
        when(usuarioRepository.findByEmail("dono@restaurante.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mentoradoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(diagnosticoInicialRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var linha = new ImportarMentoradoDiretoLinha("dono@restaurante.com", "Maria Souza", null, null,
                TipoContrato.CONSULTORIA, null, null, null, null, null,
                new BigDecimal("600000"), 6, true, 1, RespostaSimNao.SIM, "margem apertada", "5 a 10 minutos",
                EstadoImplementacao.EM_CONSTRUCAO, EstadoImplementacao.SIM, null);
        var resultado = service().criarDiretoDeImportacao(linha);

        ArgumentCaptor<MentoradoDiagnosticoInicial> captor = ArgumentCaptor.forClass(MentoradoDiagnosticoInicial.class);
        verify(diagnosticoInicialRepository).save(captor.capture());
        assertThat(captor.getValue().getMentorado()).isEqualTo(resultado.mentorado());
        assertThat(captor.getValue().getFaturamentoAnual()).isEqualByComparingTo("600000");
        assertThat(captor.getValue().getCulturaConstruida()).isEqualTo(EstadoImplementacao.EM_CONSTRUCAO);
    }

    @Test
    void criarDiretoDeImportacaoComEmailJaCadastradoLancaErro() {
        when(usuarioRepository.findByEmail("dono@restaurante.com"))
                .thenReturn(Optional.of(new Usuario("dono@restaurante.com", "hash", com.sawhub.hub.security.Perfil.MENTORADO)));

        var linha = new ImportarMentoradoDiretoLinha("dono@restaurante.com", "Maria Souza", null, null,
                TipoContrato.CONSULTORIA, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null);

        assertThatThrownBy(() -> service().criarDiretoDeImportacao(linha))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Já existe uma conta");
        verifyNoInteractions(leadRepository, mentoradoRepository);
    }

    // M28 (change request, 21/07/2026, "import único") — buscarPorEmail/existeContaComEmail
    // resolvem, por e-mail, se uma linha do CSV de import vai criar ou atualizar.

    @Test
    void buscarPorEmailDevolveMentoradoQuandoUsuarioEstaVinculadoAUmMentorado() {
        Usuario usuario = new Usuario("dono@restaurante.com", "hash", com.sawhub.hub.security.Perfil.MENTORADO);
        Mentorado mentorado = new Mentorado(usuario, "Maria Souza", null, Plano.GRATUITO, BigDecimal.ZERO, 0, 0);
        when(usuarioRepository.findByEmail("dono@restaurante.com")).thenReturn(Optional.of(usuario));
        when(mentoradoRepository.findByUsuario(usuario)).thenReturn(Optional.of(mentorado));

        assertThat(service().buscarPorEmail("dono@restaurante.com")).isEqualTo(mentorado);
    }

    @Test
    void buscarPorEmailDevolveNullQuandoUsuarioExisteMasNaoEhMentorado() {
        Usuario usuario = new Usuario("colaborador@sawhub.com.br", "hash", com.sawhub.hub.security.Perfil.ADMIN);
        when(usuarioRepository.findByEmail("colaborador@sawhub.com.br")).thenReturn(Optional.of(usuario));
        when(mentoradoRepository.findByUsuario(usuario)).thenReturn(Optional.empty());

        assertThat(service().buscarPorEmail("colaborador@sawhub.com.br")).isNull();
    }

    @Test
    void buscarPorEmailDevolveNullQuandoNaoHaContaComEsseEmail() {
        when(usuarioRepository.findByEmail("fantasma@x.com")).thenReturn(Optional.empty());

        assertThat(service().buscarPorEmail("fantasma@x.com")).isNull();
    }

    @Test
    void existeContaComEmailDistingueDeMentoradoNaoEncontrado() {
        when(usuarioRepository.findByEmail("colaborador@sawhub.com.br"))
                .thenReturn(Optional.of(new Usuario("colaborador@sawhub.com.br", "hash", com.sawhub.hub.security.Perfil.ADMIN)));
        when(usuarioRepository.findByEmail("fantasma@x.com")).thenReturn(Optional.empty());

        assertThat(service().existeContaComEmail("colaborador@sawhub.com.br")).isTrue();
        assertThat(service().existeContaComEmail("fantasma@x.com")).isFalse();
    }

    @Test
    void atualizarDeImportacaoAtualizaPerfilContratoEDiagnosticoSemMexerEmPlanoOuStatus() {
        UUID id = UUID.randomUUID();
        Usuario usuario = new Usuario("dono@restaurante.com", "hash", com.sawhub.hub.security.Perfil.MENTORADO);
        Mentorado mentorado = new Mentorado(usuario, "Nome Antigo", "Negócio Antigo", Plano.PROFISSIONAL,
                BigDecimal.ZERO, 0, 0);
        when(mentoradoRepository.findById(id)).thenReturn(Optional.of(mentorado));
        when(mentoradoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(diagnosticoInicialRepository.findByMentoradoId(id)).thenReturn(Optional.empty());
        when(diagnosticoInicialRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var linha = new ImportarMentoradoDiretoLinha("dono@restaurante.com", "Nome Novo", "Negócio Novo",
                "11999998888", TipoContrato.MENTORIA_CONTINUA, new BigDecimal("26000.00"),
                LocalDate.of(2026, 7, 17), "Menu Caseirinho Ltda", "42.521.899/0001-38", "Girlandia e Jaene",
                new BigDecimal("600000"), 6, true, 1, RespostaSimNao.SIM, "margem apertada", "5 a 10 minutos",
                EstadoImplementacao.EM_CONSTRUCAO, EstadoImplementacao.SIM, id);

        Mentorado atualizado = service().atualizarDeImportacao(id, linha);

        assertThat(atualizado.getNome()).isEqualTo("Nome Novo");
        assertThat(atualizado.getNegocio()).isEqualTo("Negócio Novo");
        assertThat(atualizado.getPlano()).isEqualTo(Plano.PROFISSIONAL);
        assertThat(atualizado.getNomeFantasia()).isEqualTo("Menu Caseirinho Ltda");
        assertThat(atualizado.getTipoContrato()).isEqualTo(TipoContrato.MENTORIA_CONTINUA);
        verify(diagnosticoInicialRepository).save(any());
    }

    @Test
    void atualizarDeImportacaoSemDadosDeDiagnosticoNaoTocaNoDiagnosticoRepository() {
        UUID id = UUID.randomUUID();
        Usuario usuario = new Usuario("dono@restaurante.com", "hash", com.sawhub.hub.security.Perfil.MENTORADO);
        Mentorado mentorado = new Mentorado(usuario, "Nome Antigo", null, Plano.GRATUITO, BigDecimal.ZERO, 0, 0);
        when(mentoradoRepository.findById(id)).thenReturn(Optional.of(mentorado));
        when(mentoradoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var linha = new ImportarMentoradoDiretoLinha("dono@restaurante.com", "Nome Novo", null, null,
                TipoContrato.CONSULTORIA, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, id);

        service().atualizarDeImportacao(id, linha);

        verifyNoInteractions(diagnosticoInicialRepository);
    }

    @Test
    void atualizarDadosContratoGravaTudoEDerivaVencimento() {
        UUID id = UUID.randomUUID();
        Mentorado mentorado = new Mentorado(null, "Maria Souza", null, Plano.GRATUITO, BigDecimal.ZERO, 0, 0);
        when(mentoradoRepository.findById(id)).thenReturn(Optional.of(mentorado));
        when(mentoradoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new AtualizarDadosContratoRequest("Menu Caseirinho", "42.521.899/0001-38",
                "Girlandia Aragão de Sousa; Jaene Oliveira de Araujo", TipoContrato.MENTORIA_CONTINUA,
                new BigDecimal("26000.00"), LocalDate.of(2025, 4, 18));
        Mentorado atualizado = service().atualizarDadosContrato(id, request);

        assertThat(atualizado.getNomeFantasia()).isEqualTo("Menu Caseirinho");
        assertThat(atualizado.getCnpj()).isEqualTo("42.521.899/0001-38");
        assertThat(atualizado.getSocios()).contains("Girlandia", "Jaene");
        assertThat(atualizado.getTipoContrato()).isEqualTo(TipoContrato.MENTORIA_CONTINUA);
        assertThat(atualizado.getVencimentoContrato()).isEqualTo(LocalDate.of(2026, 4, 18));
    }

    @Test
    void atualizarDiagnosticoInicialCriaQuandoNaoExiste() {
        UUID id = UUID.randomUUID();
        Mentorado mentorado = new Mentorado(null, "Maria Souza", null, Plano.GRATUITO, BigDecimal.ZERO, 0, 0);
        when(mentoradoRepository.findById(id)).thenReturn(Optional.of(mentorado));
        when(diagnosticoInicialRepository.findByMentoradoId(id)).thenReturn(Optional.empty());
        when(diagnosticoInicialRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new AtualizarDiagnosticoInicialRequest(new BigDecimal("600000"), 6, true, 1,
                RespostaSimNao.SIM, "margem apertada", "5 a 10 minutos",
                EstadoImplementacao.EM_CONSTRUCAO, EstadoImplementacao.EM_CONSTRUCAO);
        MentoradoDiagnosticoInicial diagnostico = service().atualizarDiagnosticoInicial(id, request);

        assertThat(diagnostico.getMentorado()).isEqualTo(mentorado);
        assertThat(diagnostico.getFaturamentoAnual()).isEqualByComparingTo("600000");
        assertThat(diagnostico.getCulturaConstruida()).isEqualTo(EstadoImplementacao.EM_CONSTRUCAO);
    }

    @Test
    void buscarDiagnosticoInicialRetornaNullQuandoAindaNaoPreenchido() {
        UUID id = UUID.randomUUID();
        when(diagnosticoInicialRepository.findByMentoradoId(id)).thenReturn(Optional.empty());

        assertThat(service().buscarDiagnosticoInicial(id)).isNull();
    }

    @Test
    void buscarDiagnosticoInicialRetornaOExistente() {
        UUID id = UUID.randomUUID();
        Mentorado mentorado = new Mentorado(null, "Maria Souza", null, Plano.GRATUITO, BigDecimal.ZERO, 0, 0);
        MentoradoDiagnosticoInicial existente = new MentoradoDiagnosticoInicial(mentorado);
        when(diagnosticoInicialRepository.findByMentoradoId(id)).thenReturn(Optional.of(existente));

        assertThat(service().buscarDiagnosticoInicial(id)).isSameAs(existente);
    }

    @Test
    void salvarDocumentoContratoGravaUrlNoMentorado() {
        UUID id = UUID.randomUUID();
        Mentorado mentorado = new Mentorado(null, "Maria Souza", null, Plano.GRATUITO, BigDecimal.ZERO, 0, 0);
        var arquivo = new org.springframework.mock.web.MockMultipartFile(
                "arquivo", "contrato.pdf", "application/pdf", "conteudo-fake".getBytes());
        when(mentoradoRepository.findById(id)).thenReturn(Optional.of(mentorado));
        when(contratoDocumentoStorageService.salvar(id, arquivo)).thenReturn("uuid-123.pdf");
        when(mentoradoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Mentorado atualizado = service().salvarDocumentoContrato(id, arquivo);

        assertThat(atualizado.getDocumentoContratoUrl()).isEqualTo("uuid-123.pdf");
    }

    @Test
    void resolverDocumentoContratoLancaErroSeAindaNaoTemDocumento() {
        UUID id = UUID.randomUUID();
        Mentorado mentorado = new Mentorado(null, "Maria Souza", null, Plano.GRATUITO, BigDecimal.ZERO, 0, 0);
        when(mentoradoRepository.findById(id)).thenReturn(Optional.of(mentorado));

        assertThatThrownBy(() -> service().resolverDocumentoContrato(id))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("documento");
    }

    @Test
    void atualizarDiagnosticoInicialAtualizaQuandoJaExiste() {
        UUID id = UUID.randomUUID();
        Mentorado mentorado = new Mentorado(null, "Maria Souza", null, Plano.GRATUITO, BigDecimal.ZERO, 0, 0);
        when(mentoradoRepository.findById(id)).thenReturn(Optional.of(mentorado));
        MentoradoDiagnosticoInicial existente = new MentoradoDiagnosticoInicial(mentorado);
        when(diagnosticoInicialRepository.findByMentoradoId(id)).thenReturn(Optional.of(existente));
        when(diagnosticoInicialRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new AtualizarDiagnosticoInicialRequest(new BigDecimal("700000"), 8, false, 2,
                RespostaSimNao.NAO, null, "10 a 15 minutos", EstadoImplementacao.SIM, EstadoImplementacao.NAO);
        MentoradoDiagnosticoInicial diagnostico = service().atualizarDiagnosticoInicial(id, request);

        assertThat(diagnostico).isSameAs(existente);
        assertThat(diagnostico.getQuantidadeColaboradores()).isEqualTo(8);
        assertThat(diagnostico.getCulturaConstruida()).isEqualTo(EstadoImplementacao.SIM);
    }

    // E17/M27 — ranking com as 4 ferramentas obrigatórias nomeadas (ver ROADMAP.md § "Blueprint (M27)").
    @Test
    void atualizarFerramentasObrigatoriasRecalculaOsContadoresGenericos() {
        UUID id = UUID.randomUUID();
        Mentorado mentorado = new Mentorado(null, "Maria Souza", null, Plano.GRATUITO, BigDecimal.ZERO, 0, 0);
        when(mentoradoRepository.findById(id)).thenReturn(Optional.of(mentorado));
        when(mentoradoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new com.sawhub.hub.mentorado.dto.AtualizarFerramentasObrigatoriasRequest(
                EstadoImplementacao.SIM, EstadoImplementacao.EM_CONSTRUCAO, EstadoImplementacao.NAO, EstadoImplementacao.SIM);
        Mentorado atualizado = service().atualizarFerramentasObrigatorias(id, request);

        assertThat(atualizado.getFerramentaDre()).isEqualTo(EstadoImplementacao.SIM);
        assertThat(atualizado.getFerramentaManualCultura()).isEqualTo(EstadoImplementacao.EM_CONSTRUCAO);
        // ferramentasTotal fixo em 4; ferramentasConcluidas = 2 (DRE + manual de processos, os
        // únicos SIM) — EM_CONSTRUCAO não conta como concluída.
        assertThat(atualizado.getFerramentasTotal()).isEqualTo(4);
        assertThat(atualizado.getFerramentasConcluidas()).isEqualTo(2);
    }

    // E17/M27 — "dois eixos de acompanhamento" (ver ROADMAP.md § "Blueprint (M27)").
    @Test
    void atualizarAcompanhamentoPreencheOsDoisEixosEMarcaAData() {
        UUID id = UUID.randomUUID();
        Mentorado mentorado = new Mentorado(null, "Maria Souza", null, Plano.GRATUITO, BigDecimal.ZERO, 0, 0);
        when(mentoradoRepository.findById(id)).thenReturn(Optional.of(mentorado));
        when(mentoradoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new com.sawhub.hub.mentorado.dto.AtualizarAcompanhamentoRequest(NivelEngajamento.BAIXO, RiscoChurn.ALTO);
        Mentorado atualizado = service().atualizarAcompanhamento(id, request);

        assertThat(atualizado.getNivelEngajamento()).isEqualTo(NivelEngajamento.BAIXO);
        assertThat(atualizado.getRiscoChurn()).isEqualTo(RiscoChurn.ALTO);
        assertThat(atualizado.getAcompanhamentoAvaliadoEm()).isNotNull();
    }
}
