package com.sawhub.hub.security;

import com.sawhub.hub.aviso.Aviso;
import com.sawhub.hub.aviso.AvisoRepository;
import com.sawhub.hub.aviso.CategoriaAviso;
import com.sawhub.hub.comercial.Lead;
import com.sawhub.hub.comercial.LeadRepository;
import com.sawhub.hub.comercial.MetaComercial;
import com.sawhub.hub.comercial.MetaComercialRepository;
import com.sawhub.hub.comercial.StatusLead;
import com.sawhub.hub.conteudo.Conteudo;
import com.sawhub.hub.conteudo.ConteudoRepository;
import com.sawhub.hub.conteudo.TipoConteudo;
import com.sawhub.hub.evento.Evento;
import com.sawhub.hub.evento.EventoRepository;
import com.sawhub.hub.evento.InscricaoEvento;
import com.sawhub.hub.evento.InscricaoEventoRepository;
import com.sawhub.hub.evento.TipoEvento;
import com.sawhub.hub.loja.CategoriaProduto;
import com.sawhub.hub.loja.Pedido;
import com.sawhub.hub.loja.PedidoRepository;
import com.sawhub.hub.loja.Produto;
import com.sawhub.hub.loja.ProdutoRepository;
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
import com.sawhub.hub.mentoria.Ata;
import com.sawhub.hub.mentoria.AtaEncaminhamentoSugerido;
import com.sawhub.hub.mentoria.AtaEncaminhamentoSugeridoRepository;
import com.sawhub.hub.mentoria.AtaRepository;
import com.sawhub.hub.mentoria.Mentoria;
import com.sawhub.hub.mentoria.MentoriaRepository;
import com.sawhub.hub.mentoria.TipoMentoria;
import com.sawhub.hub.team.Area;
import com.sawhub.hub.team.Colaborador;
import com.sawhub.hub.team.ColaboradorRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
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
    private final MentoriaRepository mentoriaRepository;
    private final AtaRepository ataRepository;
    private final AtaEncaminhamentoSugeridoRepository ataEncaminhamentoSugeridoRepository;
    private final ConteudoRepository conteudoRepository;
    private final EventoRepository eventoRepository;
    private final InscricaoEventoRepository inscricaoEventoRepository;
    private final ProdutoRepository produtoRepository;
    private final PedidoRepository pedidoRepository;
    private final AvisoRepository avisoRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UsuarioRepository usuarioRepository, ColaboradorRepository colaboradorRepository,
                           MentoradoRepository mentoradoRepository, EncaminhamentoRepository encaminhamentoRepository,
                           CategoriaFinanceiraRepository categoriaFinanceiraRepository,
                           LancamentoFinanceiroRepository lancamentoFinanceiroRepository,
                           ContaPagarReceberRepository contaPagarReceberRepository,
                           LeadRepository leadRepository,
                           MetaComercialRepository metaComercialRepository,
                           MentoriaRepository mentoriaRepository,
                           AtaRepository ataRepository,
                           AtaEncaminhamentoSugeridoRepository ataEncaminhamentoSugeridoRepository,
                           ConteudoRepository conteudoRepository,
                           EventoRepository eventoRepository,
                           InscricaoEventoRepository inscricaoEventoRepository,
                           ProdutoRepository produtoRepository,
                           PedidoRepository pedidoRepository,
                           AvisoRepository avisoRepository,
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
        this.mentoriaRepository = mentoriaRepository;
        this.ataRepository = ataRepository;
        this.ataEncaminhamentoSugeridoRepository = ataEncaminhamentoSugeridoRepository;
        this.conteudoRepository = conteudoRepository;
        this.eventoRepository = eventoRepository;
        this.inscricaoEventoRepository = inscricaoEventoRepository;
        this.produtoRepository = produtoRepository;
        this.pedidoRepository = pedidoRepository;
        this.avisoRepository = avisoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedColaboradores();
        seedMentorados();
        seedFinanceiro();
        seedComercial();
        // M12: seedMentoriasEAtas associa a Ficha técnica a uma mentoria (materiaisRecomendados,
        // H5.2) — precisa que o Conteudo já exista.
        seedConteudos();
        seedMentoriasEAtas();
        seedEventos();
        // M13: precisa que eventos e mentorados já existam.
        seedInscricoesEventos();
        // M14: precisa que mentorados e a CategoriaFinanceira "Loja SAW" (seedFinanceiro) já existam.
        seedLoja();
        seedAvisos();
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
                "18.0", 3, 3, new int[][]{{1, 1}, {1, 1}, {1, 1}, {1, 1}, {1, 1}, {2, 1}, {2, 1}, {2, 1}, {2, 1}, {2, 0}},
                "(11) 91234-5678", "Apaixonado por gestão e por negócios sustentáveis.", "Gestão, Finanças, Liderança",
                LocalDate.of(2026, 10, 15));
        seedMentorado("Ana Costa", "ana@anacosta.com.br", "Cantina Ana Costa", Plano.ESSENCIAL,
                "12.0", 2, 3, new int[][]{{1, 1}, {1, 1}, {1, 1}, {1, 1}, {2, 1}, {2, 1}, {1, 0}, {1, 0}, {2, 0}},
                "(11) 98877-6655", "Focada em padronizar a cozinha e reduzir desperdício.", "Processos, Finanças",
                LocalDate.of(2026, 9, 20));
        seedMentorado("Carlos Menezes", "carlos@pointdocarlos.com.br", "Point do Carlos", Plano.PROFISSIONAL,
                "5.0", 1, 3, new int[][]{{1, 1}, {1, 1}, {1, 1}, {2, 1}, {1, 0}, {1, 0}, {1, 0}, {2, 0}, {2, 0}, {2, 0}},
                null, null, null, LocalDate.of(2026, 8, 5));
        seedMentorado("Rafael Gomes", "rafael@bistrogomes.com.br", "Bistrô Gomes", Plano.ESSENCIAL,
                "-3.0", 0, 3, new int[][]{{1, 1}, {1, 1}, {1, 0}, {1, 0}, {1, 0}, {2, 0}, {2, 0}, {2, 0}},
                "(21) 99988-7766", "Buscando virar o jogo do fluxo de caixa.", "Finanças",
                LocalDate.of(2026, 8, 30));
        seedMentorado("Fernanda Lima", "fernanda@cantinadafernanda.com.br", "Cantina da Fernanda", Plano.BASICO,
                "24.0", 3, 3, new int[][]{{1, 1}, {1, 1}, {1, 1}, {1, 1}, {1, 1}, {2, 1}, {2, 1}, {2, 1}, {2, 1}, {2, 1}},
                "(31) 97766-5544", "Sempre em busca da próxima tendência de cardápio.", "Marketing, Processos",
                LocalDate.of(2026, 11, 10));
        seedMentorado("Marina Souza", "marina@sabordamarina.com.br", "Sabor da Marina", Plano.BASICO,
                "-8.0", 0, 3, new int[][]{{1, 1}, {1, 0}, {1, 0}, {1, 0}, {2, 0}, {2, 0}, {2, 0}},
                null, null, null, LocalDate.of(2026, 7, 25));
    }

    private void seedMentorado(String nome, String email, String negocio, Plano plano, String crescimentoPct,
                                int ferramentasConcluidas, int ferramentasTotal, int[][] encaminhamentosPesoConcluido,
                                String telefone, String bio, String areasInteresse, LocalDate vencimentoPlano) {
        Usuario usuario = usuarioRepository.save(
                new Usuario(email, passwordEncoder.encode("trocar-no-primeiro-login"), Perfil.MENTORADO));
        Mentorado mentorado = new Mentorado(usuario, nome, negocio, plano,
                new BigDecimal(crescimentoPct), ferramentasConcluidas, ferramentasTotal);
        mentorado.atualizarPerfil(telefone, bio, areasInteresse, null);
        mentorado.definirVencimentoPlano(vencimentoPlano);
        mentorado = mentoradoRepository.save(mentorado);

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

    private void seedMentoriasEAtas() {
        if (mentoriaRepository.count() > 0) {
            return;
        }
        Colaborador lucas = buscarColaboradorPorNome("Lucas Alves");
        Colaborador ricardo = buscarColaboradorPorNome("Ricardo Costa");
        Mentorado joao = buscarMentoradoPorNome("João Silva");
        Mentorado ana = buscarMentoradoPorNome("Ana Costa");
        Mentorado carlos = buscarMentoradoPorNome("Carlos Menezes");
        Mentorado rafael = buscarMentoradoPorNome("Rafael Gomes");
        Mentorado fernanda = buscarMentoradoPorNome("Fernanda Lima");
        Mentorado marina = buscarMentoradoPorNome("Marina Souza");
        if (lucas == null || ricardo == null || joao == null) {
            return; // seedColaboradores/seedMentorados não rodaram (ex.: banco já tinha dado de outra fonte)
        }

        // 1) Mentoria individual já REALIZADA, com ata PUBLICADA (IA "rodou" — dado simulado,
        // não chamou a API de verdade) — demonstra o fluxo completo pronto pra apresentação.
        Mentoria m1 = new Mentoria(TipoMentoria.INDIVIDUAL, lucas, Set.of(joao),
                Instant.parse("2026-07-02T14:00:00Z"), 60, "https://meet.google.com/joao-lucas", null);
        m1.confirmar();
        m1.realizar();
        // M12 (H5.2) — "materiais recomendados": a ficha técnica é literalmente o assunto da ata
        // desta mentoria (ver resumo abaixo), bom fixture pra mostrar o campo populado de verdade
        // (não só um array vazio) na tela do mentorado e no E2E.
        Conteudo fichaTecnica = buscarConteudoPorTitulo("Ficha técnica — modelo");
        if (fichaTecnica != null) {
            m1.atualizarMateriaisRecomendados(Set.of(fichaTecnica));
        }
        mentoriaRepository.save(m1);
        Ata ata1 = new Ata(m1);
        ata1.concluirProcessamento(
                "Transcrição simulada: conversamos sobre a atualização da ficha técnica dos pratos principais "
                        + "e a necessidade de revisar o cardápio pra refletir os novos preços de insumos.",
                "João revisou os resultados do último mês (crescimento de 18%) e alinhou com o mentor os "
                        + "próximos passos: atualizar a ficha técnica dos pratos principais e revisar o cardápio "
                        + "com os novos preços de insumos.");
        ata1.publicar();
        ataRepository.save(ata1);
        encaminhamentoRepository.save(new Encaminhamento(joao, "Atualizar ficha técnica dos pratos principais", 2, false, m1));

        // 2) Mentoria em grupo REALIZADA, ata com IA CONCLUÍDA mas ainda em RASCUNHO — demonstra
        // a tela de revisão humana (sugestões aguardando aceite antes de publicar).
        Mentoria m2 = new Mentoria(TipoMentoria.GRUPO, ricardo, Set.of(ana, carlos),
                Instant.parse("2026-07-05T16:00:00Z"), 90, "https://meet.google.com/grupo-ricardo", null);
        m2.confirmar();
        m2.realizar();
        mentoriaRepository.save(m2);
        Ata ata2 = new Ata(m2);
        ata2.concluirProcessamento(
                "Transcrição simulada: discussão em grupo sobre precificação do buffet e digitalização do cardápio.",
                "Encontro em grupo sobre precificação e digitalização de cardápio. Ana e Carlos trouxeram "
                        + "dúvidas semelhantes sobre como repassar o aumento de custo sem perder cliente.");
        ataRepository.save(ata2);
        ataEncaminhamentoSugeridoRepository.save(new AtaEncaminhamentoSugerido(ata2, "Revisar precificação do buffet", 2, true));
        ataEncaminhamentoSugeridoRepository.save(new AtaEncaminhamentoSugerido(ata2, "Digitalizar cardápio (QR code)", 1, true));

        // 3) Próxima mentoria confirmada (futura) — aparece na agenda, sem ata ainda.
        Mentoria m3 = new Mentoria(TipoMentoria.INDIVIDUAL, lucas, Set.of(rafael),
                Instant.parse("2026-07-15T15:00:00Z"), 60, "https://meet.google.com/rafael-lucas", null);
        m3.confirmar();
        mentoriaRepository.save(m3);

        // 4) Mentoria agendada (ainda não confirmada), em grupo.
        Mentoria m4 = new Mentoria(TipoMentoria.GRUPO, ricardo, Set.of(fernanda, marina),
                Instant.parse("2026-07-20T10:00:00Z"), 90, null, "SAW HUB — Sala de reuniões");
        mentoriaRepository.save(m4);
    }

    private Colaborador buscarColaboradorPorNome(String nome) {
        return colaboradorRepository.findAllByOrderByNomeAsc().stream()
                .filter(c -> c.getNome().equals(nome))
                .findFirst()
                .orElse(null);
    }

    private Mentorado buscarMentoradoPorNome(String nome) {
        return mentoradoRepository.buscarComFiltro(null, null, null).stream()
                .filter(m -> m.getNome().equals(nome))
                .findFirst()
                .orElse(null);
    }

    private void seedAvisos() {
        if (avisoRepository.count() > 0) {
            return;
        }
        avisoRepository.save(new Aviso("Nova mentoria em grupo disponível", "Inscrições abertas pra Liderança que Gera Resultados.",
                CategoriaAviso.MENTORIAS, Plano.GRATUITO));
        avisoRepository.save(new Aviso("Novo material na biblioteca", "Planilha de Fluxo de Caixa Diário já disponível pra download.",
                CategoriaAviso.MATERIAIS, Plano.GRATUITO));
        avisoRepository.save(new Aviso("Workshop de Precificação Estratégica", "Vagas limitadas — garanta a sua.",
                CategoriaAviso.EVENTOS, Plano.BASICO));
        avisoRepository.save(new Aviso("Manutenção programada", "A plataforma ficará indisponível por 30 minutos no domingo às 2h.",
                CategoriaAviso.GERAL, Plano.GRATUITO));
    }

    private void seedConteudos() {
        if (conteudoRepository.count() > 0) {
            return;
        }
        criarConteudo("Ficha técnica — modelo", TipoConteudo.PLANILHA, Plano.GRATUITO, true);
        criarConteudo("Manual da Cultura SAW", TipoConteudo.DOCUMENTO, Plano.GRATUITO, true);
        criarConteudo("Como calcular seu DRE", TipoConteudo.VIDEO, Plano.BASICO, true);
        criarConteudo("Apresentação: Precificação estratégica", TipoConteudo.APRESENTACAO, Plano.ESSENCIAL, false);
    }

    // Devolve o Conteudo salvo (M12: seedMentoriasEAtas usa o retorno pra associar materiaisRecomendados).
    private Conteudo criarConteudo(String titulo, TipoConteudo tipo, Plano planoMinimo, boolean publicado) {
        Conteudo conteudo = new Conteudo(titulo, tipo, "https://cdn.sawhub.com.br/conteudos/" + tipo.name().toLowerCase(), planoMinimo);
        if (publicado) {
            conteudo.publicar();
        }
        return conteudoRepository.save(conteudo);
    }

    private Conteudo buscarConteudoPorTitulo(String titulo) {
        return conteudoRepository.findAll().stream()
                .filter(c -> c.getTitulo().equals(titulo))
                .findFirst()
                .orElse(null);
    }

    private void seedEventos() {
        if (eventoRepository.count() > 0) {
            return;
        }
        eventoRepository.save(new Evento("Encontro Nacional SAW 2026", TipoEvento.AO_VIVO, "Gestão de restaurantes",
                Instant.parse("2026-09-10T19:00:00Z"), null, "https://meet.google.com/encontro-saw", 200));
        eventoRepository.save(new Evento("Workshop de Gestão Financeira", TipoEvento.PRESENCIAL, "Financeiro",
                Instant.parse("2026-08-05T18:00:00Z"), "Av. Paulista, 1000 — São Paulo/SP", null, 40));

        Evento realizado = new Evento("Live: Tendências do setor", TipoEvento.AO_VIVO, "Mercado",
                Instant.parse("2026-06-15T19:00:00Z"), null, "https://meet.google.com/live-tendencias", null);
        realizado.iniciar();
        realizado.finalizar();
        eventoRepository.save(realizado);

        Evento cancelado = new Evento("Feirão de fornecedores", TipoEvento.PRESENCIAL, "Compras",
                Instant.parse("2026-06-20T14:00:00Z"), "Expo Center Norte", null, 150);
        cancelado.cancelar();
        eventoRepository.save(cancelado);
    }

    // M13 (H7.2) — João inscrito no Encontro Nacional (fixture pra "próximos eventos"/"inscrito"),
    // Carlos inscrito no Workshop — dado real pra E2E, não só endpoint funcional sem uso.
    private void seedInscricoesEventos() {
        if (inscricaoEventoRepository.count() > 0) {
            return;
        }
        Evento encontro = buscarEventoPorTitulo("Encontro Nacional SAW 2026");
        Evento workshop = buscarEventoPorTitulo("Workshop de Gestão Financeira");
        Mentorado joao = buscarMentoradoPorNome("João Silva");
        Mentorado carlos = buscarMentoradoPorNome("Carlos Menezes");
        if (encontro == null || workshop == null || joao == null || carlos == null) {
            return;
        }

        encontro.ocuparVaga();
        eventoRepository.save(encontro);
        inscricaoEventoRepository.save(new InscricaoEvento(joao, encontro));

        workshop.ocuparVaga();
        eventoRepository.save(workshop);
        inscricaoEventoRepository.save(new InscricaoEvento(carlos, workshop));
    }

    private Evento buscarEventoPorTitulo(String titulo) {
        return eventoRepository.findAll().stream()
                .filter(e -> e.getTitulo().equals(titulo))
                .findFirst()
                .orElse(null);
    }

    // M14 (E8 Loja) — catálogo variado (categorias/destaque/desconto do spec.md H8.1) + 1 pedido
    // já LIBERADO (Ana Costa) pra "Meus Pedidos" ter dado real, não só o catálogo vazio de
    // inscrições. O lançamento financeiro e o incremento de vendas espelham manualmente o que
    // PedidoPagamentoService faria de verdade num webhook — o seeder não passa pelo gateway.
    private void seedLoja() {
        if (produtoRepository.count() > 0) {
            return;
        }
        criarProduto("Pacote de Planilhas Gerenciais",
                "12 planilhas prontas para gestão completa do seu restaurante.", CategoriaProduto.PLANILHA,
                "97.00", "197.00", "4.8", true, true);
        criarProduto("Curso de Precificação Estratégica",
                "Aprenda a precificar seu cardápio sem perder margem.", CategoriaProduto.CURSO,
                "297.00", null, "4.6", false, true);
        Produto template = criarProduto("Template de Cardápio Digital",
                "Template editável pronto pra QR code, sem depender de designer.", CategoriaProduto.TEMPLATE,
                "47.00", null, "4.5", false, true);
        criarProduto("E-book: Gestão de Custos", "Guia prático de CMV e ponto de equilíbrio.",
                CategoriaProduto.EBOOK, "27.00", null, "4.3", false, true);
        criarProduto("Kit Abertura de Restaurante", "Tudo que você precisa pra abrir com o pé direito.",
                CategoriaProduto.KIT, "397.00", "497.00", "4.9", true, true);
        // Ainda não publicado de propósito — fixture pra confirmar que rascunho nunca aparece no
        // catálogo do mentorado nem pode ser adicionado ao carrinho (achado do M11/M12 reaplicado
        // aqui: mesmo invariante isPublicado()).
        criarProduto("Consultoria Express (1h)", "Sessão individual com o time SAW.",
                CategoriaProduto.CONSULTORIA, "250.00", null, null, false, false);

        Mentorado ana = buscarMentoradoPorNome("Ana Costa");
        if (ana == null) {
            return;
        }
        Pedido pedido = new Pedido(ana);
        pedido.adicionarItem(template, 1);
        pedido.iniciarCheckout("seed-preference-id");
        pedido.confirmarPagamento();
        pedido.liberar();
        pedidoRepository.save(pedido);

        template.incrementarVendas(1);
        produtoRepository.save(template);

        categoriaFinanceiraRepository.findByOrigemReceita(OrigemReceita.LOJA).ifPresent(categoriaLoja ->
                lancamentoFinanceiroRepository.save(new LancamentoFinanceiro(TipoLancamento.RECEITA, categoriaLoja,
                        "Pedido " + pedido.getId(), pedido.getValorTotal(), LocalDate.now(), StatusLancamento.REALIZADO, null)));
    }

    private Produto criarProduto(String titulo, String descricao, CategoriaProduto categoria, String preco,
                                  String precoOriginal, String avaliacaoMedia, boolean destaque, boolean publicado) {
        Produto produto = new Produto(titulo, descricao, categoria, new BigDecimal(preco),
                precoOriginal == null ? null : new BigDecimal(precoOriginal),
                avaliacaoMedia == null ? null : new BigDecimal(avaliacaoMedia),
                destaque, "https://cdn.sawhub.com.br/produtos/" + categoria.name().toLowerCase() + ".zip", null);
        if (publicado) {
            produto.publicar();
        }
        return produtoRepository.save(produto);
    }
}
