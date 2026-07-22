package com.sawhub.hub.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sawhub.hub.comercial.Lead;
import com.sawhub.hub.comercial.LeadRepository;
import com.sawhub.hub.comercial.MetaComercial;
import com.sawhub.hub.comercial.MetaComercialRepository;
import com.sawhub.hub.conteudo.Conteudo;
import com.sawhub.hub.conteudo.ConteudoRepository;
import com.sawhub.hub.evento.Evento;
import com.sawhub.hub.evento.EventoRepository;
import com.sawhub.hub.evento.InscricaoEventoRepository;
import com.sawhub.hub.financeiro.CategoriaFinanceira;
import com.sawhub.hub.financeiro.CategoriaFinanceiraRepository;
import com.sawhub.hub.financeiro.LancamentoFinanceiro;
import com.sawhub.hub.financeiro.LancamentoFinanceiroRepository;
import com.sawhub.hub.financeiro.OrigemReceita;
import com.sawhub.hub.mentorado.Encaminhamento;
import com.sawhub.hub.mentorado.EncaminhamentoRepository;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.mentoria.Ata;
import com.sawhub.hub.mentoria.AtaEncaminhamentoSugeridoRepository;
import com.sawhub.hub.mentoria.AtaRepository;
import com.sawhub.hub.mentoria.Mentoria;
import com.sawhub.hub.mentoria.MentoriaRepository;
import com.sawhub.hub.team.Area;
import com.sawhub.hub.team.Colaborador;
import com.sawhub.hub.team.ColaboradorRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Dado de demo, não produção — mas achado H1 da revisão de segurança é justamente que
 * este seeder cria contas ADMIN reais com senha conhecida ("trocar-no-primeiro-login").
 * Estes testes fixam a idempotência (nunca duplicar em cima de dado já seedado) e o
 * volume exato esperado, pra qualquer alteração aqui ser deliberada, não acidental.
 */
@ExtendWith(MockitoExtension.class)
class DemoDataSeederTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ColaboradorRepository colaboradorRepository;
    @Mock
    private MentoradoRepository mentoradoRepository;
    @Mock
    private EncaminhamentoRepository encaminhamentoRepository;
    @Mock
    private CategoriaFinanceiraRepository categoriaFinanceiraRepository;
    @Mock
    private LancamentoFinanceiroRepository lancamentoFinanceiroRepository;
    @Mock
    private LeadRepository leadRepository;
    @Mock
    private MetaComercialRepository metaComercialRepository;
    @Mock
    private MentoriaRepository mentoriaRepository;
    @Mock
    private AtaRepository ataRepository;
    @Mock
    private AtaEncaminhamentoSugeridoRepository ataEncaminhamentoSugeridoRepository;
    @Mock
    private ConteudoRepository conteudoRepository;
    @Mock
    private EventoRepository eventoRepository;
    @Mock
    private InscricaoEventoRepository inscricaoEventoRepository;
    @Mock
    private com.sawhub.hub.loja.ProdutoRepository produtoRepository;
    @Mock
    private com.sawhub.hub.loja.PedidoRepository pedidoRepository;
    @Mock
    private com.sawhub.hub.aviso.AvisoRepository avisoRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private DemoDataSeeder seeder() {
        return new DemoDataSeeder(usuarioRepository, colaboradorRepository, mentoradoRepository,
                encaminhamentoRepository, categoriaFinanceiraRepository, lancamentoFinanceiroRepository,
                leadRepository, metaComercialRepository, mentoriaRepository,
                ataRepository, ataEncaminhamentoSugeridoRepository, conteudoRepository, eventoRepository,
                inscricaoEventoRepository, produtoRepository, pedidoRepository, avisoRepository, passwordEncoder);
    }

    @BeforeEach
    void skipSeedFinanceiroPorPadrao() {
        // A maioria dos testes aqui é sobre colaborador/mentorado — sem isto, toda vez que
        // lancamentoFinanceiroRepository.count() não for explicitamente stubado, o Mockito devolve
        // 0 por padrão e o seedFinanceiro() roda por inteiro sem querer. M26 trocou o guard de
        // categoriaFinanceiraRepository.count() pra lancamentoFinanceiroRepository.count() — a
        // migration V40 já pré-cadastra categorias em qualquer ambiente, então o guard antigo
        // sempre veria count()>0 mesmo num banco novo (ver DemoDataSeeder.seedFinanceiro).
        lenient().when(lancamentoFinanceiroRepository.count()).thenReturn(1L);
    }

    @BeforeEach
    void skipSeedComercialPorPadrao() {
        lenient().when(leadRepository.count()).thenReturn(1L);
    }

    @BeforeEach
    void skipSeedMentoriasConteudosEventosPorPadrao() {
        lenient().when(mentoriaRepository.count()).thenReturn(1L);
        lenient().when(conteudoRepository.count()).thenReturn(1L);
        lenient().when(eventoRepository.count()).thenReturn(1L);
    }

    private static Colaborador colaborador(String nome, Area area) {
        Colaborador c = new Colaborador(null, nome, area);
        return c;
    }

    private static Mentorado mentorado(String nome) {
        Mentorado m = new Mentorado(null, nome, null, BigDecimal.ZERO, 0, 0);
        ReflectionTestUtils.setField(m, "id", java.util.UUID.randomUUID());
        return m;
    }

    private void stubSaves() {
        lenient().when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(mentoradoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("hash-fixo");
    }

    @Test
    void naoRecriaColaboradoresSeJaExistiremMaisDeUm() {
        when(colaboradorRepository.count()).thenReturn(2L);
        when(mentoradoRepository.count()).thenReturn(1L);

        seeder().run(null);

        verify(colaboradorRepository, never()).save(any());
    }

    @Test
    void naoRecriaMentoradosSeJaExistirAlgum() {
        when(colaboradorRepository.count()).thenReturn(2L);
        when(mentoradoRepository.count()).thenReturn(1L);

        seeder().run(null);

        verify(mentoradoRepository, never()).save(any());
        verify(encaminhamentoRepository, never()).save(any());
    }

    @Test
    void seedaExatamenteQuatroColaboradoresComSenhaPadraoConhecida() {
        stubSaves();
        when(colaboradorRepository.count()).thenReturn(1L); // só o Fundador, do FundadorBootstrap
        when(mentoradoRepository.count()).thenReturn(1L);   // pula seedMentorados nesse teste

        seeder().run(null);

        verify(colaboradorRepository, times(4)).save(any(Colaborador.class));
        verify(passwordEncoder, times(4)).encode("trocar-no-primeiro-login");
    }

    @Test
    void seedaExatamenteSeisMentoradosComOTotalDeEncaminhamentosEsperado() {
        stubSaves();
        when(colaboradorRepository.count()).thenReturn(2L); // pula seedColaboradores nesse teste
        when(mentoradoRepository.count()).thenReturn(0L);

        seeder().run(null);

        verify(mentoradoRepository, times(6)).save(any(Mentorado.class));
        // 10 + 9 + 10 + 8 + 10 + 7 = 54 encaminhamentos ao todo (bate com o dado real seedado
        // e conferido via psql nesta mesma sessão de desenvolvimento).
        verify(encaminhamentoRepository, times(54)).save(any(Encaminhamento.class));
    }

    @Test
    void naoRecriaFinanceiroSeJaExistirLancamento() {
        when(colaboradorRepository.count()).thenReturn(2L);
        when(mentoradoRepository.count()).thenReturn(1L);
        when(lancamentoFinanceiroRepository.count()).thenReturn(1L);

        seeder().run(null);

        verify(categoriaFinanceiraRepository, never()).save(any());
    }

    // M26 — "Mentoria Contínua" (ASSINATURA) e "Eventos" (EVENTO) agora vêm pré-cadastradas pela
    // migration V40 em qualquer ambiente (não só seedadas aqui) — seedFinanceiro() as busca em
    // vez de criar. Achado do Marcos (22/07/2026): "Loja SAW"/"Impostos sobre vendas"/
    // "Infraestrutura"/"Marketing"/"Equipe & Folha" eram categorias inventadas aqui, sem bater com
    // o raio-x real da planilha "DRE Financeira Saw" — trocadas por busca das subcategorias reais
    // já confirmadas na V52 ("Impostos"/"Sistemas"/"Tráfego Pago"/"Diretor"), nenhuma criada mais.
    // "Loja SAW" não tinha equivalente real na lista de Receita (resumo) da V50 — removida, sem
    // substituto. As "contas em aberto" (antes ContaPagarReceber, agora LancamentoFinanceiro com
    // dataVencimento) também contam no total de lancamentoFinanceiroRepository.save().
    @Test
    void naoCriaCategoriaNovaEBuscaSubcategoriasReaisParaOsLancamentosDosDoisMesesEDuasContasEmAberto() {
        when(colaboradorRepository.count()).thenReturn(2L);
        when(mentoradoRepository.count()).thenReturn(1L);
        when(lancamentoFinanceiroRepository.count()).thenReturn(0L);
        CategoriaFinanceira categoriaAssinatura = new CategoriaFinanceira("Mentoria Contínua",
                com.sawhub.hub.financeiro.TipoLancamento.RECEITA, com.sawhub.hub.financeiro.GrupoDre.RECEITA_BRUTA,
                OrigemReceita.ASSINATURA);
        CategoriaFinanceira categoriaEvento = new CategoriaFinanceira("Eventos",
                com.sawhub.hub.financeiro.TipoLancamento.RECEITA, com.sawhub.hub.financeiro.GrupoDre.RECEITA_BRUTA,
                OrigemReceita.EVENTO);
        when(categoriaFinanceiraRepository.findByOrigemReceita(OrigemReceita.ASSINATURA))
                .thenReturn(Optional.of(categoriaAssinatura));
        when(categoriaFinanceiraRepository.findByOrigemReceita(OrigemReceita.EVENTO))
                .thenReturn(Optional.of(categoriaEvento));
        // Patrocínio (V50)/Produtos Digitais (V40)/subcategorias reais de despesa (V52) —
        // pré-cadastradas por migration em qualquer ambiente, resolvidas por nome (não têm
        // OrigemReceita própria — ver comentário no DashboardFaturamentoResponse).
        mockarCategoriaPorNome("Patrocínio", com.sawhub.hub.financeiro.TipoLancamento.RECEITA,
                com.sawhub.hub.financeiro.GrupoDre.RECEITA_BRUTA);
        mockarCategoriaPorNome("Produtos Digitais", com.sawhub.hub.financeiro.TipoLancamento.RECEITA,
                com.sawhub.hub.financeiro.GrupoDre.RECEITA_BRUTA);
        mockarCategoriaPorNome("Impostos", com.sawhub.hub.financeiro.TipoLancamento.DESPESA,
                com.sawhub.hub.financeiro.GrupoDre.DESPESA_OPERACIONAL);
        mockarCategoriaPorNome("Sistemas", com.sawhub.hub.financeiro.TipoLancamento.DESPESA,
                com.sawhub.hub.financeiro.GrupoDre.DESPESA_OPERACIONAL);
        mockarCategoriaPorNome("Tráfego Pago", com.sawhub.hub.financeiro.TipoLancamento.DESPESA,
                com.sawhub.hub.financeiro.GrupoDre.DESPESA_OPERACIONAL);
        mockarCategoriaPorNome("Diretor", com.sawhub.hub.financeiro.TipoLancamento.DESPESA,
                com.sawhub.hub.financeiro.GrupoDre.DESPESA_OPERACIONAL);
        when(lancamentoFinanceiroRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        seeder().run(null);

        // Nenhuma categoria nova — seedFinanceiro() só busca subcategorias reais já existentes.
        verify(categoriaFinanceiraRepository, never()).save(any(CategoriaFinanceira.class));
        // 11 dos dois meses (junho sem evento = 5, julho com evento = 6, "Loja SAW" removida de
        // ambos) + 2 contas em aberto + Patrocínio + Produtos Digitais = 15.
        ArgumentCaptor<LancamentoFinanceiro> lancamentoCaptor = ArgumentCaptor.forClass(LancamentoFinanceiro.class);
        verify(lancamentoFinanceiroRepository, times(15)).save(lancamentoCaptor.capture());
        assertThat(lancamentoCaptor.getAllValues()).anySatisfy(l ->
                assertThat(l.getStatus().name()).isEqualTo("VENCIDO"));
    }

    private void mockarCategoriaPorNome(String nome, com.sawhub.hub.financeiro.TipoLancamento tipo,
                                         com.sawhub.hub.financeiro.GrupoDre grupoDre) {
        when(categoriaFinanceiraRepository.findByNomeIgnoreCase(nome))
                .thenReturn(List.of(new CategoriaFinanceira(nome, tipo, grupoDre, null)));
    }

    @Test
    void naoRecriaComercialSeJaExistirLead() {
        when(colaboradorRepository.count()).thenReturn(2L);
        when(mentoradoRepository.count()).thenReturn(1L);
        when(leadRepository.count()).thenReturn(1L);

        seeder().run(null);

        verify(leadRepository, never()).save(any());
        verify(metaComercialRepository, never()).save(any());
    }

    @Test
    void seedaSeisLeadsEUmaMetaParaOVendedorComercial() {
        when(colaboradorRepository.count()).thenReturn(2L);
        when(mentoradoRepository.count()).thenReturn(1L);
        when(leadRepository.count()).thenReturn(0L);
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(metaComercialRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Colaborador paula = new Colaborador(null, "Paula Mendes", Area.COMERCIAL);
        when(colaboradorRepository.findAll()).thenReturn(List.of(paula));

        seeder().run(null);

        ArgumentCaptor<Lead> leadCaptor = ArgumentCaptor.forClass(Lead.class);
        verify(leadRepository, times(6)).save(leadCaptor.capture());
        assertThat(leadCaptor.getAllValues())
                .filteredOn(l -> l.getStatus().name().equals("FECHADO"))
                .hasSize(2);
        assertThat(leadCaptor.getAllValues())
                .filteredOn(l -> l.getStatus().name().equals("PERDIDO"))
                .hasSize(1);

        ArgumentCaptor<MetaComercial> metaCaptor = ArgumentCaptor.forClass(MetaComercial.class);
        verify(metaComercialRepository, times(1)).save(metaCaptor.capture());
        assertThat(metaCaptor.getValue().getVendedor()).isSameAs(paula);
    }

    @Test
    void naoRecriaMentoriasSeJaExistirAlguma() {
        when(colaboradorRepository.count()).thenReturn(2L);
        when(mentoradoRepository.count()).thenReturn(1L);
        when(mentoriaRepository.count()).thenReturn(1L);

        seeder().run(null);

        verify(mentoriaRepository, never()).save(any());
        verify(ataRepository, never()).save(any());
    }

    @Test
    void seedaQuatroMentoriasDuasAtasEUmEncaminhamentoMaterializado() {
        when(colaboradorRepository.count()).thenReturn(2L);
        when(mentoradoRepository.count()).thenReturn(1L);
        when(mentoriaRepository.count()).thenReturn(0L);
        when(mentoriaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ataRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ataEncaminhamentoSugeridoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(encaminhamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // M14: esta lista de mentorados inclui Ana Costa, então seedLoja() (que roda no mesmo
        // run()) encontra ela e tenta materializar o pedido de exemplo — precisa que
        // produtoRepository.save() devolva o argumento, senão o Produto fica null.
        when(produtoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Colaborador lucas = colaborador("Lucas Alves", Area.GESTAO_PERFORMANCE);
        Colaborador ricardo = colaborador("Ricardo Costa", Area.GESTAO_PERFORMANCE);
        when(colaboradorRepository.findAll()).thenReturn(List.of(lucas, ricardo));

        List<Mentorado> mentorados = List.of(mentorado("João Silva"), mentorado("Ana Costa"), mentorado("Carlos Menezes"),
                mentorado("Rafael Gomes"), mentorado("Fernanda Lima"), mentorado("Marina Souza"));
        when(mentoradoRepository.buscarComFiltro(null, null)).thenReturn(mentorados);

        seeder().run(null);

        verify(mentoriaRepository, times(4)).save(any(Mentoria.class));

        ArgumentCaptor<Ata> ataCaptor = ArgumentCaptor.forClass(Ata.class);
        verify(ataRepository, times(2)).save(ataCaptor.capture());
        assertThat(ataCaptor.getAllValues()).extracting(a -> a.getStatus().name())
                .containsExactlyInAnyOrder("PUBLICADA", "RASCUNHO");

        // A ata em RASCUNHO carrega as 2 sugestões da IA aguardando revisão humana.
        verify(ataEncaminhamentoSugeridoRepository, times(2)).save(any());
        // A ata PUBLICADA já materializou 1 encaminhamento de verdade (mesmo efeito de
        // AtaService.publicar(), replicado manualmente aqui porque o seeder não passa pelo service).
        verify(encaminhamentoRepository, times(1)).save(any(Encaminhamento.class));
    }

    @Test
    void naoRecriaConteudosSeJaExistirAlgum() {
        when(colaboradorRepository.count()).thenReturn(2L);
        when(mentoradoRepository.count()).thenReturn(1L);
        when(conteudoRepository.count()).thenReturn(1L);

        seeder().run(null);

        verify(conteudoRepository, never()).save(any());
    }

    @Test
    void seedaQuatroConteudosComUmNaoPublicado() {
        when(colaboradorRepository.count()).thenReturn(2L);
        when(mentoradoRepository.count()).thenReturn(1L);
        when(conteudoRepository.count()).thenReturn(0L);
        when(conteudoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        seeder().run(null);

        ArgumentCaptor<Conteudo> captor = ArgumentCaptor.forClass(Conteudo.class);
        verify(conteudoRepository, times(4)).save(captor.capture());
        assertThat(captor.getAllValues()).filteredOn(c -> !c.isPublicado()).hasSize(1);
    }

    @Test
    void naoRecriaEventosSeJaExistirAlgum() {
        when(colaboradorRepository.count()).thenReturn(2L);
        when(mentoradoRepository.count()).thenReturn(1L);
        when(eventoRepository.count()).thenReturn(1L);

        seeder().run(null);

        verify(eventoRepository, never()).save(any());
    }

    @Test
    void seedaQuatroEventosComStatusVariados() {
        when(colaboradorRepository.count()).thenReturn(2L);
        when(mentoradoRepository.count()).thenReturn(1L);
        when(eventoRepository.count()).thenReturn(0L);
        when(eventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        seeder().run(null);

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository, times(4)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(e -> e.getStatus().name())
                .containsExactlyInAnyOrder("PROGRAMADO", "PROGRAMADO", "REALIZADO", "CANCELADO");
    }

    @Test
    void naoRecriaProdutosSeJaExistirAlgum() {
        when(colaboradorRepository.count()).thenReturn(2L);
        when(mentoradoRepository.count()).thenReturn(1L);
        when(produtoRepository.count()).thenReturn(1L);

        seeder().run(null);

        verify(produtoRepository, never()).save(any());
    }

    @Test
    void seedaSeisProdutosComUmDespublicado() {
        when(colaboradorRepository.count()).thenReturn(2L);
        when(mentoradoRepository.count()).thenReturn(1L);
        when(produtoRepository.count()).thenReturn(0L);
        when(produtoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        seeder().run(null);

        ArgumentCaptor<com.sawhub.hub.loja.Produto> captor = ArgumentCaptor.forClass(com.sawhub.hub.loja.Produto.class);
        // 6 produtos seedados — Ana Costa não é encontrada nesse teste (mentoradoRepository
        // mockado sem seed de verdade), então o pedido de exemplo não roda e não gera um save extra.
        verify(produtoRepository, times(6)).save(captor.capture());
        assertThat(captor.getAllValues()).filteredOn(p -> !p.isPublicado()).hasSize(1);
    }

    @Test
    void naoRecriaAvisosSeJaExistirAlgum() {
        when(colaboradorRepository.count()).thenReturn(2L);
        when(mentoradoRepository.count()).thenReturn(1L);
        when(avisoRepository.count()).thenReturn(1L);

        seeder().run(null);

        verify(avisoRepository, never()).save(any());
    }

    @Test
    void seedaQuatroAvisosCobrindoAsQuatroCategorias() {
        when(colaboradorRepository.count()).thenReturn(2L);
        when(mentoradoRepository.count()).thenReturn(1L);
        when(avisoRepository.count()).thenReturn(0L);
        when(avisoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        seeder().run(null);

        ArgumentCaptor<com.sawhub.hub.aviso.Aviso> captor = ArgumentCaptor.forClass(com.sawhub.hub.aviso.Aviso.class);
        verify(avisoRepository, times(4)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(a -> a.getCategoria().name())
                .containsExactlyInAnyOrder("MENTORIAS", "MATERIAIS", "EVENTOS", "GERAL");
    }
}
