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
import com.sawhub.hub.financeiro.CategoriaFinanceira;
import com.sawhub.hub.financeiro.CategoriaFinanceiraRepository;
import com.sawhub.hub.financeiro.ContaPagarReceber;
import com.sawhub.hub.financeiro.ContaPagarReceberRepository;
import com.sawhub.hub.financeiro.LancamentoFinanceiro;
import com.sawhub.hub.financeiro.LancamentoFinanceiroRepository;
import com.sawhub.hub.mentorado.Encaminhamento;
import com.sawhub.hub.mentorado.EncaminhamentoRepository;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.team.Area;
import com.sawhub.hub.team.Colaborador;
import com.sawhub.hub.team.ColaboradorRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    private ContaPagarReceberRepository contaPagarReceberRepository;
    @Mock
    private LeadRepository leadRepository;
    @Mock
    private MetaComercialRepository metaComercialRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private DemoDataSeeder seeder() {
        return new DemoDataSeeder(usuarioRepository, colaboradorRepository, mentoradoRepository,
                encaminhamentoRepository, categoriaFinanceiraRepository, lancamentoFinanceiroRepository,
                contaPagarReceberRepository, leadRepository, metaComercialRepository, passwordEncoder);
    }

    @BeforeEach
    void skipSeedFinanceiroPorPadrao() {
        // A maioria dos testes aqui é sobre colaborador/mentorado — sem isto, toda vez que
        // categoriaFinanceiraRepository.count() não for explicitamente stubado, o Mockito devolve
        // 0 por padrão e o seedFinanceiro() roda por inteiro sem querer.
        lenient().when(categoriaFinanceiraRepository.count()).thenReturn(1L);
    }

    @BeforeEach
    void skipSeedComercialPorPadrao() {
        lenient().when(leadRepository.count()).thenReturn(1L);
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
    void naoRecriaFinanceiroSeJaExistirCategoria() {
        when(colaboradorRepository.count()).thenReturn(2L);
        when(mentoradoRepository.count()).thenReturn(1L);
        when(categoriaFinanceiraRepository.count()).thenReturn(1L);

        seeder().run(null);

        verify(categoriaFinanceiraRepository, never()).save(any());
    }

    @Test
    void seedaSeteCategoriasELancamentosDosDoisMesesEDuasContas() {
        when(colaboradorRepository.count()).thenReturn(2L);
        when(mentoradoRepository.count()).thenReturn(1L);
        when(categoriaFinanceiraRepository.count()).thenReturn(0L);
        when(categoriaFinanceiraRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(lancamentoFinanceiroRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(contaPagarReceberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        seeder().run(null);

        verify(categoriaFinanceiraRepository, times(7)).save(any(CategoriaFinanceira.class));
        // 3 receitas (assinaturas/loja/impostos-não-conta) + ... — junho sem eventos (6 lançamentos),
        // julho com eventos (7 lançamentos) = 13 ao todo.
        verify(lancamentoFinanceiroRepository, times(13)).save(any(LancamentoFinanceiro.class));

        ArgumentCaptor<ContaPagarReceber> contaCaptor = ArgumentCaptor.forClass(ContaPagarReceber.class);
        verify(contaPagarReceberRepository, times(2)).save(contaCaptor.capture());
        assertThat(contaCaptor.getAllValues()).anySatisfy(c ->
                assertThat(c.getStatus().name()).isEqualTo("VENCIDO"));
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
        Colaborador paula = new Colaborador(null, "Paula Mendes", Area.COMERCIAL, null, new BigDecimal("22.5"));
        when(colaboradorRepository.findAllByOrderByNomeAsc()).thenReturn(List.of(paula));

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
}
