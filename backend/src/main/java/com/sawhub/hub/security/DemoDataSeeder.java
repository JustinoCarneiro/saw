package com.sawhub.hub.security;

import com.sawhub.hub.comercial.Lead;
import com.sawhub.hub.comercial.LeadRepository;
import com.sawhub.hub.comercial.MetaComercial;
import com.sawhub.hub.comercial.MetaComercialRepository;
import com.sawhub.hub.comercial.StatusLead;
import com.sawhub.hub.financeiro.CategoriaFinanceira;
import com.sawhub.hub.financeiro.CategoriaFinanceiraRepository;
import com.sawhub.hub.financeiro.ContaPagarReceber;
import com.sawhub.hub.financeiro.ContaPagarReceberRepository;
import com.sawhub.hub.financeiro.GrupoDre;
import com.sawhub.hub.financeiro.LancamentoFinanceiro;
import com.sawhub.hub.financeiro.LancamentoFinanceiroRepository;
import com.sawhub.hub.financeiro.OrigemReceita;
import com.sawhub.hub.financeiro.StatusLancamento;
import com.sawhub.hub.financeiro.TipoConta;
import com.sawhub.hub.financeiro.TipoLancamento;
import com.sawhub.hub.mentorado.Encaminhamento;
import com.sawhub.hub.mentorado.EncaminhamentoRepository;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.mentorado.Plano;
import com.sawhub.hub.team.Area;
import com.sawhub.hub.team.Colaborador;
import com.sawhub.hub.team.ColaboradorRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dados de demonstração — mesmos nomes/números do protótipo estático aprovado, pra dar
 * continuidade visual na apresentação. Roda depois do FundadorBootstrap (@Order maior).
 * Desligável via sawhub.seed-demo-data=false (produção não deve ter isso).
 */
@Component
@Order(10)
@ConditionalOnProperty(value = "sawhub.seed-demo-data", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final ColaboradorRepository colaboradorRepository;
    private final MentoradoRepository mentoradoRepository;
    private final EncaminhamentoRepository encaminhamentoRepository;
    private final CategoriaFinanceiraRepository categoriaFinanceiraRepository;
    private final LancamentoFinanceiroRepository lancamentoFinanceiroRepository;
    private final ContaPagarReceberRepository contaPagarReceberRepository;
    private final LeadRepository leadRepository;
    private final MetaComercialRepository metaComercialRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UsuarioRepository usuarioRepository, ColaboradorRepository colaboradorRepository,
                           MentoradoRepository mentoradoRepository, EncaminhamentoRepository encaminhamentoRepository,
                           CategoriaFinanceiraRepository categoriaFinanceiraRepository,
                           LancamentoFinanceiroRepository lancamentoFinanceiroRepository,
                           ContaPagarReceberRepository contaPagarReceberRepository,
                           LeadRepository leadRepository,
                           MetaComercialRepository metaComercialRepository,
                           PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.colaboradorRepository = colaboradorRepository;
        this.mentoradoRepository = mentoradoRepository;
        this.encaminhamentoRepository = encaminhamentoRepository;
        this.categoriaFinanceiraRepository = categoriaFinanceiraRepository;
        this.lancamentoFinanceiroRepository = lancamentoFinanceiroRepository;
        this.contaPagarReceberRepository = contaPagarReceberRepository;
        this.leadRepository = leadRepository;
        this.metaComercialRepository = metaComercialRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedColaboradores();
        seedMentorados();
        seedFinanceiro();
        seedComercial();
    }

    private void seedColaboradores() {
        if (colaboradorRepository.count() > 1) {
            return; // já rodou (sobra só o Fundador do FundadorBootstrap na 1ª execução)
        }
        criarColaborador("Lucas Alves", "lucas@sawhub.com.br", Area.GESTAO_PERFORMANCE, 38, "15.2");
        criarColaborador("Paula Mendes", "paula@sawhub.com.br", Area.COMERCIAL, null, "22.5");
        criarColaborador("Ricardo Costa", "ricardo@sawhub.com.br", Area.GESTAO_PERFORMANCE, 42, "12.8");
        criarColaborador("Juliana Lima", "juliana@sawhub.com.br", Area.MARKETING, null, null);
    }

    private void criarColaborador(String nome, String email, Area area, Integer carteira, String conversaoPct) {
        Usuario usuario = usuarioRepository.save(
                new Usuario(email, passwordEncoder.encode("trocar-no-primeiro-login"), Perfil.ADMIN));
        colaboradorRepository.save(new Colaborador(usuario, nome, area, carteira,
                conversaoPct == null ? null : new BigDecimal(conversaoPct)));
    }

    private void seedMentorados() {
        if (mentoradoRepository.count() > 0) {
            return;
        }
        seedMentorado("João Silva", "joao@saborearte.com.br", "Restaurante Sabor & Arte", Plano.PROFISSIONAL,
                "18.0", 3, 3, new int[][]{{1, 1}, {1, 1}, {1, 1}, {1, 1}, {1, 1}, {2, 1}, {2, 1}, {2, 1}, {2, 1}, {2, 0}});
        seedMentorado("Ana Costa", "ana@anacosta.com.br", "Cantina Ana Costa", Plano.ESSENCIAL,
                "12.0", 2, 3, new int[][]{{1, 1}, {1, 1}, {1, 1}, {1, 1}, {2, 1}, {2, 1}, {1, 0}, {1, 0}, {2, 0}});
        seedMentorado("Carlos Menezes", "carlos@pointdocarlos.com.br", "Point do Carlos", Plano.PROFISSIONAL,
                "5.0", 1, 3, new int[][]{{1, 1}, {1, 1}, {1, 1}, {2, 1}, {1, 0}, {1, 0}, {1, 0}, {2, 0}, {2, 0}, {2, 0}});
        seedMentorado("Rafael Gomes", "rafael@bistrogomes.com.br", "Bistrô Gomes", Plano.ESSENCIAL,
                "-3.0", 0, 3, new int[][]{{1, 1}, {1, 1}, {1, 0}, {1, 0}, {1, 0}, {2, 0}, {2, 0}, {2, 0}});
        seedMentorado("Fernanda Lima", "fernanda@cantinadafernanda.com.br", "Cantina da Fernanda", Plano.BASICO,
                "24.0", 3, 3, new int[][]{{1, 1}, {1, 1}, {1, 1}, {1, 1}, {1, 1}, {2, 1}, {2, 1}, {2, 1}, {2, 1}, {2, 1}});
        seedMentorado("Marina Souza", "marina@sabordamarina.com.br", "Sabor da Marina", Plano.BASICO,
                "-8.0", 0, 3, new int[][]{{1, 1}, {1, 0}, {1, 0}, {1, 0}, {2, 0}, {2, 0}, {2, 0}});
    }

    private void seedMentorado(String nome, String email, String negocio, Plano plano, String crescimentoPct,
                                int ferramentasConcluidas, int ferramentasTotal, int[][] encaminhamentosPesoConcluido) {
        Usuario usuario = usuarioRepository.save(
                new Usuario(email, passwordEncoder.encode("trocar-no-primeiro-login"), Perfil.MENTORADO));
        Mentorado mentorado = mentoradoRepository.save(new Mentorado(usuario, nome, negocio, plano,
                new BigDecimal(crescimentoPct), ferramentasConcluidas, ferramentasTotal));

        int i = 1;
        for (int[] pesoConcluido : encaminhamentosPesoConcluido) {
            int peso = pesoConcluido[0];
            boolean concluido = pesoConcluido[1] == 1;
            encaminhamentoRepository.save(
                    new Encaminhamento(mentorado, "Encaminhamento " + i++, peso, concluido));
        }
    }

    private void seedFinanceiro() {
        if (categoriaFinanceiraRepository.count() > 0) {
            return;
        }
        CategoriaFinanceira assinaturas = categoriaFinanceiraRepository.save(new CategoriaFinanceira(
                "Assinaturas", TipoLancamento.RECEITA, GrupoDre.RECEITA_BRUTA, OrigemReceita.ASSINATURA));
        CategoriaFinanceira loja = categoriaFinanceiraRepository.save(new CategoriaFinanceira(
                "Loja SAW", TipoLancamento.RECEITA, GrupoDre.RECEITA_BRUTA, OrigemReceita.LOJA));
        CategoriaFinanceira eventos = categoriaFinanceiraRepository.save(new CategoriaFinanceira(
                "Eventos", TipoLancamento.RECEITA, GrupoDre.RECEITA_BRUTA, OrigemReceita.EVENTO));
        CategoriaFinanceira impostos = categoriaFinanceiraRepository.save(new CategoriaFinanceira(
                "Impostos sobre vendas", TipoLancamento.DESPESA, GrupoDre.DEDUCOES, null));
        CategoriaFinanceira infra = categoriaFinanceiraRepository.save(new CategoriaFinanceira(
                "Infraestrutura", TipoLancamento.DESPESA, GrupoDre.CUSTOS, null));
        CategoriaFinanceira marketing = categoriaFinanceiraRepository.save(new CategoriaFinanceira(
                "Marketing", TipoLancamento.DESPESA, GrupoDre.DESPESA_OPERACIONAL, null));
        CategoriaFinanceira equipe = categoriaFinanceiraRepository.save(new CategoriaFinanceira(
                "Equipe & Folha", TipoLancamento.DESPESA, GrupoDre.DESPESA_OPERACIONAL, null));

        // Junho e julho/2026 — dois meses fechados pra comparativo mês a mês do DRE (H14.2) e
        // MRR/churn do dashboard (H14.3). Assinaturas somam os planos dos 6 mentorados seedados.
        seedMesFinanceiro(LocalDate.of(2026, 6, 5), assinaturas, loja, eventos, impostos, infra, marketing, equipe,
                "1580.00", "420.00", "0.00", "95.00", "620.00", "300.00", "3800.00");
        seedMesFinanceiro(LocalDate.of(2026, 7, 5), assinaturas, loja, eventos, impostos, infra, marketing, equipe,
                "1782.00", "510.00", "200.00", "108.00", "620.00", "350.00", "3800.00");

        // Contas em aberto — mistura de pendente e vencida, pra tela de Contas ter o que mostrar.
        contaPagarReceberRepository.save(new ContaPagarReceber(TipoConta.A_PAGAR, "Servidor Hostinger — agosto",
                new BigDecimal("180.00"), LocalDate.of(2026, 8, 10), infra));
        ContaPagarReceber vencida = new ContaPagarReceber(TipoConta.A_RECEBER, "Mensalidade em atraso — Rafael Gomes",
                new BigDecimal("297.00"), LocalDate.of(2026, 7, 1), assinaturas);
        vencida.marcarVencida();
        contaPagarReceberRepository.save(vencida);
    }

    private void seedMesFinanceiro(LocalDate data, CategoriaFinanceira assinaturas, CategoriaFinanceira loja,
                                    CategoriaFinanceira eventos, CategoriaFinanceira impostos, CategoriaFinanceira infra,
                                    CategoriaFinanceira marketing, CategoriaFinanceira equipe,
                                    String valorAssinaturas, String valorLoja, String valorEventos, String valorImpostos,
                                    String valorInfra, String valorMarketing, String valorEquipe) {
        lancamentoFinanceiroRepository.save(new LancamentoFinanceiro(TipoLancamento.RECEITA, assinaturas,
                "Assinaturas do mês", new BigDecimal(valorAssinaturas), data, StatusLancamento.REALIZADO, null));
        lancamentoFinanceiroRepository.save(new LancamentoFinanceiro(TipoLancamento.RECEITA, loja,
                "Vendas Loja SAW", new BigDecimal(valorLoja), data, StatusLancamento.REALIZADO, null));
        if (new BigDecimal(valorEventos).compareTo(BigDecimal.ZERO) > 0) {
            lancamentoFinanceiroRepository.save(new LancamentoFinanceiro(TipoLancamento.RECEITA, eventos,
                    "Inscrições em eventos", new BigDecimal(valorEventos), data, StatusLancamento.REALIZADO, null));
        }
        lancamentoFinanceiroRepository.save(new LancamentoFinanceiro(TipoLancamento.DESPESA, impostos,
                "Impostos sobre vendas", new BigDecimal(valorImpostos), data, StatusLancamento.REALIZADO, null));
        lancamentoFinanceiroRepository.save(new LancamentoFinanceiro(TipoLancamento.DESPESA, infra,
                "Servidor e ferramentas", new BigDecimal(valorInfra), data, StatusLancamento.REALIZADO, null));
        lancamentoFinanceiroRepository.save(new LancamentoFinanceiro(TipoLancamento.DESPESA, marketing,
                "Campanhas e anúncios", new BigDecimal(valorMarketing), data, StatusLancamento.REALIZADO, null));
        lancamentoFinanceiroRepository.save(new LancamentoFinanceiro(TipoLancamento.DESPESA, equipe,
                "Folha de pagamento", new BigDecimal(valorEquipe), data, StatusLancamento.REALIZADO, null));
    }

    private void seedComercial() {
        if (leadRepository.count() > 0) {
            return;
        }
        Colaborador paula = colaboradorRepository.findAllByOrderByNomeAsc().stream()
                .filter(c -> c.getArea() == Area.COMERCIAL)
                .findFirst()
                .orElse(null);

        // Funil com leads em todos os estágios (H13.2) — fechados/perdidos ficam com
        // dataFechamento = agora (Lead.fechar/perder não aceita data injetada, ver ROADMAP.md M05),
        // o que cai dentro do mês corrente por construção, exatamente como o dashboard espera.
        seedLead("Beatriz Ramos", "beatriz@padariaramos.com.br", Plano.ESSENCIAL, paula, StatusLead.FECHADO, Plano.ESSENCIAL, null);
        seedLead("Diego Martins", "diego@churrascariamartins.com.br", Plano.PROFISSIONAL, paula, StatusLead.FECHADO, Plano.PROFISSIONAL, null);
        seedLead("Sandra Nunes", "sandra@cafesandra.com.br", Plano.BASICO, paula, StatusLead.PERDIDO, null, "Optou por concorrente");
        seedLead("Fábio Teixeira", "fabio@pizzariateixeira.com.br", Plano.BASICO, paula, StatusLead.PROPOSTA, null, null);
        seedLead("Renata Alves", "renata@barelvas.com.br", null, paula, StatusLead.EM_CONTATO, null, null);
        seedLead("Marcelo Duarte", "marcelo@duartegrill.com.br", Plano.ESSENCIAL, null, StatusLead.SOLICITACAO, null, null);

        if (paula != null) {
            metaComercialRepository.save(new MetaComercial(paula, 2026, 7, 5));
        }
    }

    private void seedLead(String nome, String email, Plano planoInteresse, Colaborador vendedor, StatusLead statusAlvo,
                           Plano planoFechado, String motivoPerdido) {
        Lead lead = new Lead(nome, email, null, null, planoInteresse);
        if (statusAlvo != StatusLead.SOLICITACAO) {
            lead.moverParaEmContato(vendedor);
            if (statusAlvo == StatusLead.PERDIDO) {
                lead.perder(motivoPerdido);
            } else if (statusAlvo != StatusLead.EM_CONTATO) {
                lead.moverParaProposta();
                if (statusAlvo == StatusLead.FECHADO) {
                    lead.fechar(planoFechado);
                }
            }
        }
        leadRepository.save(lead);
    }
}
