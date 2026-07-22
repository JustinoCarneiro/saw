package com.sawhub.hub.security;

import com.sawhub.hub.aviso.Aviso;
import com.sawhub.hub.aviso.AvisoRepository;
import com.sawhub.hub.aviso.CategoriaAviso;
import com.sawhub.hub.comercial.FormaPagamento;
import com.sawhub.hub.comercial.Lead;
import com.sawhub.hub.comercial.LeadRepository;
import com.sawhub.hub.comercial.MetaComercial;
import com.sawhub.hub.comercial.MetaComercialRepository;
import com.sawhub.hub.comercial.OrigemVenda;
import com.sawhub.hub.comercial.ProdutoVenda;
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
import com.sawhub.hub.financeiro.LancamentoFinanceiro;
import com.sawhub.hub.financeiro.LancamentoFinanceiroRepository;
import com.sawhub.hub.financeiro.OrigemReceita;
import com.sawhub.hub.financeiro.StatusLancamento;
import com.sawhub.hub.financeiro.TipoLancamento;
import com.sawhub.hub.mentorado.Encaminhamento;
import com.sawhub.hub.mentorado.EncaminhamentoRepository;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.mentorado.TipoContrato;
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
import java.time.Duration;
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
        // Achado do Marcos (22/07/2026, conferido direto em produção) — "gestao_perf@"/"comercial@"
        // são contas por PAPEL (não nome de pessoa) em produção, igual ao Fundador
        // (admin@sawhub.com.br, ver application.yml); "Ricardo Costa"/"Juliana Lima" já batem com
        // produção como estão (nome de pessoa mesmo, não trocar).
        criarColaborador("Gestão de Performance", "gestao_perf@sawhub.com.br", Area.GESTAO_PERFORMANCE);
        criarColaborador("Comercial", "comercial@sawhub.com.br", Area.COMERCIAL);
        criarColaborador("Ricardo Costa", "ricardo@sawhub.com.br", Area.GESTAO_PERFORMANCE);
        criarColaborador("Juliana Lima", "juliana@sawhub.com.br", Area.MARKETING);
    }

    private void criarColaborador(String nome, String email, Area area) {
        Usuario usuario = usuarioRepository.save(
                new Usuario(email, passwordEncoder.encode("trocar-no-primeiro-login"), Perfil.ADMIN));
        colaboradorRepository.save(new Colaborador(usuario, nome, area));
    }

    private void seedMentorados() {
        if (mentoradoRepository.count() > 0) {
            return;
        }
        seedMentorado("João Silva", "joao@saborearte.com.br", "Restaurante Sabor & Arte",
                "18.0", 3, 3, new int[][]{{1, 1}, {1, 1}, {1, 1}, {1, 1}, {1, 1}, {2, 1}, {2, 1}, {2, 1}, {2, 1}, {2, 0}},
                "(11) 91234-5678", "Apaixonado por gestão e por negócios sustentáveis.",
                TipoContrato.MENTORIA_CONTINUA, LocalDate.of(2026, 1, 15));
        seedMentorado("Ana Costa", "ana@anacosta.com.br", "Cantina Ana Costa",
                "12.0", 2, 3, new int[][]{{1, 1}, {1, 1}, {1, 1}, {1, 1}, {2, 1}, {2, 1}, {1, 0}, {1, 0}, {2, 0}},
                "(11) 98877-6655", "Focada em padronizar a cozinha e reduzir desperdício.",
                TipoContrato.MENTORIA_INDIVIDUAL, LocalDate.of(2026, 2, 1));
        seedMentorado("Carlos Menezes", "carlos@pointdocarlos.com.br", "Point do Carlos",
                "5.0", 1, 3, new int[][]{{1, 1}, {1, 1}, {1, 1}, {2, 1}, {1, 0}, {1, 0}, {1, 0}, {2, 0}, {2, 0}, {2, 0}},
                null, null, TipoContrato.MENTORIA_CONTINUA, LocalDate.of(2026, 3, 10));
        seedMentorado("Rafael Gomes", "rafael@bistrogomes.com.br", "Bistrô Gomes",
                "-3.0", 0, 3, new int[][]{{1, 1}, {1, 1}, {1, 0}, {1, 0}, {1, 0}, {2, 0}, {2, 0}, {2, 0}},
                "(21) 99988-7766", "Buscando virar o jogo do fluxo de caixa.",
                TipoContrato.MENTORIA_INDIVIDUAL, LocalDate.of(2026, 2, 20));
        seedMentorado("Fernanda Lima", "fernanda@cantinadafernanda.com.br", "Cantina da Fernanda",
                "24.0", 3, 3, new int[][]{{1, 1}, {1, 1}, {1, 1}, {1, 1}, {1, 1}, {2, 1}, {2, 1}, {2, 1}, {2, 1}, {2, 1}},
                "(31) 97766-5544", "Sempre em busca da próxima tendência de cardápio.",
                TipoContrato.CONSULTORIA, LocalDate.of(2026, 4, 1));
        // Marina fica sem TipoContrato de propósito — demonstra o bucket "Não informado" do
        // Dashboard Admin (M28), não é um esquecimento.
        seedMentorado("Marina Souza", "marina@sabordamarina.com.br", "Sabor da Marina",
                "-8.0", 0, 3, new int[][]{{1, 1}, {1, 0}, {1, 0}, {1, 0}, {2, 0}, {2, 0}, {2, 0}},
                null, null, null, null);
    }

    private void seedMentorado(String nome, String email, String negocio, String crescimentoPct,
                                int ferramentasConcluidas, int ferramentasTotal, int[][] encaminhamentosPesoConcluido,
                                String telefone, String bio,
                                TipoContrato tipoContrato, LocalDate dataFechamentoContrato) {
        Usuario usuario = usuarioRepository.save(
                new Usuario(email, passwordEncoder.encode("trocar-no-primeiro-login"), Perfil.MENTORADO));
        Mentorado mentorado = new Mentorado(usuario, nome, negocio,
                new BigDecimal(crescimentoPct), ferramentasConcluidas, ferramentasTotal);
        mentorado.atualizarPerfil(telefone, bio, null);
        mentorado.atualizarDadosContrato(null, null, null, tipoContrato, null, dataFechamentoContrato);
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
        // M26 — guarda por lancamento (não mais por categoria): a migration V40 já pré-cadastra
        // "Mentoria Contínua"/"Eventos" (entre outras) pra qualquer ambiente, então
        // categoriaFinanceiraRepository.count() > 0 seria sempre verdadeiro mesmo num banco novo,
        // pulando o resto do seed de demo por engano.
        if (lancamentoFinanceiroRepository.count() > 0) {
            return;
        }
        // "Mentoria Contínua"/"Eventos" já vêm da V40 (garantidas em qualquer ambiente, não só
        // aqui) — recriar geraria violação de uq_categoria_financeira_origem_receita.
        CategoriaFinanceira assinaturas = categoriaFinanceiraRepository.findByOrigemReceita(OrigemReceita.ASSINATURA)
                .orElseThrow(() -> new IllegalStateException("Categoria ASSINATURA (V40) não encontrada."));
        CategoriaFinanceira eventos = categoriaFinanceiraRepository.findByOrigemReceita(OrigemReceita.EVENTO)
                .orElseThrow(() -> new IllegalStateException("Categoria EVENTO (V40) não encontrada."));
        // Achado do Marcos (22/07/2026, revisão ao vivo dos dados de demo): "Loja SAW" (receita) e
        // "Impostos sobre vendas"/"Infraestrutura"/"Marketing"/"Equipe & Folha" (despesa) eram
        // categorias inventadas aqui no seeder — não batem com o raio-x real da planilha "DRE
        // Financeira Saw" (ver comentário da V50: a lista real de Receita (resumo) é Total, Fixas,
        // Variáveis, Eventos, Mentoria Contínua, Mentoria Individual, Patrocínio, Produtos Digitais
        // — "Loja SAW" não está nela). Trocado por buscar as 48 subcategorias reais de despesa já
        // confirmadas via raio-x (V52) em vez de criar categoria nova — mesmo padrão já usado
        // abaixo pra Patrocínio/Produtos Digitais. A escolha de QUAL das subcategorias reais
        // representa cada linha da demo (ex.: "Sistemas" pra servidor/ferramentas) é só uma
        // aproximação razoável, não confirmada linha a linha pelo cliente — mesma ressalva já
        // registrada no comentário da V52 pro mapeamento subcategoria->grupo.
        CategoriaFinanceira impostos = buscarCategoriaFinanceiraPorNome("Impostos");
        CategoriaFinanceira sistemas = buscarCategoriaFinanceiraPorNome("Sistemas");
        CategoriaFinanceira trafegoPago = buscarCategoriaFinanceiraPorNome("Tráfego Pago");
        CategoriaFinanceira diretor = buscarCategoriaFinanceiraPorNome("Diretor");

        // Junho e julho/2026 — dois meses fechados pra comparativo mês a mês do DRE (H14.2) e
        // MRR/churn do dashboard (H14.3). Assinaturas somam os planos dos 6 mentorados seedados.
        seedMesFinanceiro(LocalDate.of(2026, 6, 5), assinaturas, eventos, impostos, sistemas, trafegoPago, diretor,
                "1580.00", "0.00", "95.00", "620.00", "300.00", "3800.00");
        seedMesFinanceiro(LocalDate.of(2026, 7, 5), assinaturas, eventos, impostos, sistemas, trafegoPago, diretor,
                "1782.00", "200.00", "108.00", "620.00", "350.00", "3800.00");

        // Pedido do Marcos (22/07/2026) — "Patrocínio"/"Produtos Digitais" (categorias reais da
        // planilha, V50/V40) tinham venda de verdade possível desde sempre via LeadService, mas a
        // Composição da receita do Dashboard só agrupava por OrigemReceita e não mostrava nenhuma
        // das duas. Uma linha de cada em julho/2026 prova a composição rica na demo, não só nos
        // testes unitários.
        CategoriaFinanceira patrocinio = categoriaFinanceiraRepository.findByNomeIgnoreCase("Patrocínio").stream()
                .findFirst().orElseThrow(() -> new IllegalStateException("Categoria Patrocínio (V50) não encontrada."));
        CategoriaFinanceira produtosDigitais = categoriaFinanceiraRepository.findByNomeIgnoreCase("Produtos Digitais")
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Categoria Produtos Digitais (V40) não encontrada."));
        lancamentoFinanceiroRepository.save(new LancamentoFinanceiro(TipoLancamento.RECEITA, patrocinio,
                "Patrocínio Encontro Nacional SAW 2026", new BigDecimal("2500.00"), LocalDate.of(2026, 7, 8),
                StatusLancamento.REALIZADO));
        lancamentoFinanceiroRepository.save(new LancamentoFinanceiro(TipoLancamento.RECEITA, produtosDigitais,
                "Venda avulsa — Pacote de Planilhas Gerenciais", new BigDecimal("450.00"), LocalDate.of(2026, 7, 12),
                StatusLancamento.REALIZADO));

        // Lançamentos em aberto (M26 — antes vivia em ContaPagarReceber) — mistura de previsto e
        // vencido, pra tela de Contas ter o que mostrar. dataCompetencia nasce igual à
        // dataVencimento (melhor palpite disponível antes de liquidar — ver LancamentoFinanceiro).
        lancamentoFinanceiroRepository.save(new LancamentoFinanceiro(TipoLancamento.DESPESA, sistemas,
                "Servidor Hostinger — agosto", new BigDecimal("180.00"), LocalDate.of(2026, 8, 10),
                StatusLancamento.PREVISTO, null, LocalDate.of(2026, 8, 10)));
        LancamentoFinanceiro vencida = new LancamentoFinanceiro(TipoLancamento.RECEITA, assinaturas,
                "Mensalidade em atraso — Rafael Gomes", new BigDecimal("297.00"), LocalDate.of(2026, 7, 1),
                StatusLancamento.PREVISTO, null, LocalDate.of(2026, 7, 1));
        vencida.marcarVencida();
        lancamentoFinanceiroRepository.save(vencida);
    }

    private void seedMesFinanceiro(LocalDate data, CategoriaFinanceira assinaturas,
                                    CategoriaFinanceira eventos, CategoriaFinanceira impostos, CategoriaFinanceira sistemas,
                                    CategoriaFinanceira trafegoPago, CategoriaFinanceira diretor,
                                    String valorAssinaturas, String valorEventos, String valorImpostos,
                                    String valorSistemas, String valorTrafegoPago, String valorDiretor) {
        lancamentoFinanceiroRepository.save(new LancamentoFinanceiro(TipoLancamento.RECEITA, assinaturas,
                "Assinaturas do mês", new BigDecimal(valorAssinaturas), data, StatusLancamento.REALIZADO));
        if (new BigDecimal(valorEventos).compareTo(BigDecimal.ZERO) > 0) {
            lancamentoFinanceiroRepository.save(new LancamentoFinanceiro(TipoLancamento.RECEITA, eventos,
                    "Inscrições em eventos", new BigDecimal(valorEventos), data, StatusLancamento.REALIZADO));
        }
        lancamentoFinanceiroRepository.save(new LancamentoFinanceiro(TipoLancamento.DESPESA, impostos,
                "Impostos sobre vendas", new BigDecimal(valorImpostos), data, StatusLancamento.REALIZADO));
        lancamentoFinanceiroRepository.save(new LancamentoFinanceiro(TipoLancamento.DESPESA, sistemas,
                "Servidor e ferramentas", new BigDecimal(valorSistemas), data, StatusLancamento.REALIZADO));
        lancamentoFinanceiroRepository.save(new LancamentoFinanceiro(TipoLancamento.DESPESA, trafegoPago,
                "Campanhas e anúncios", new BigDecimal(valorTrafegoPago), data, StatusLancamento.REALIZADO));
        lancamentoFinanceiroRepository.save(new LancamentoFinanceiro(TipoLancamento.DESPESA, diretor,
                "Folha de pagamento", new BigDecimal(valorDiretor), data, StatusLancamento.REALIZADO));
    }

    private void seedComercial() {
        if (leadRepository.count() > 0) {
            return;
        }
        Colaborador comercial = colaboradorRepository.findAll().stream()
                .filter(c -> c.getArea() == Area.COMERCIAL)
                .findFirst()
                .orElse(null);

        // Funil com leads em todos os estágios (H13.2) — fechados/perdidos ficam com
        // dataFechamento = agora (Lead.fecharVenda/perder não aceita data injetada, ver
        // ROADMAP.md M05), o que cai dentro do mês corrente por construção, exatamente como o
        // dashboard espera. Produtos e valores refletem a mesma faixa de preço real de produção
        // (Mentoria Contínua ~R$26k, Formação Profissional ~R$597, Ficha Técnica ~R$97) — nome e
        // e-mail do cliente são fictícios de propósito, não são os leads reais.
        seedLeadFechado("Cliente Demo 1", "cliente1@exemplo.com.br", comercial,
                ProdutoVenda.MENTORIA_CONTINUA, new BigDecimal("26000"));
        seedLeadFechado("Cliente Demo 2", "cliente2@exemplo.com.br", comercial,
                ProdutoVenda.FORMACAO_PROFISSIONAL, new BigDecimal("597"));
        seedLead("Cliente Demo 3", "cliente3@exemplo.com.br", comercial, StatusLead.PERDIDO, "Optou por concorrente");
        seedLead("Cliente Demo 4", "cliente4@exemplo.com.br", comercial, StatusLead.PROPOSTA, null);
        seedLead("Cliente Demo 5", "cliente5@exemplo.com.br", comercial, StatusLead.EM_CONTATO, null);
        seedLead("Cliente Demo 6", "cliente6@exemplo.com.br", null, StatusLead.SOLICITACAO, null);

        if (comercial != null) {
            metaComercialRepository.save(new MetaComercial(comercial, 2026, 7, 5, new BigDecimal("10.00")));
        }
    }

    private void seedLead(String nome, String email, Colaborador vendedor, StatusLead statusAlvo, String motivoPerdido) {
        Lead lead = new Lead(nome, email, null, null);
        if (statusAlvo != StatusLead.SOLICITACAO) {
            lead.moverParaEmContato(vendedor);
            if (statusAlvo == StatusLead.PERDIDO) {
                lead.perder(motivoPerdido);
            } else if (statusAlvo != StatusLead.EM_CONTATO) {
                lead.moverParaProposta();
            }
        }
        leadRepository.save(lead);
    }

    // M28 — os 2 leads FECHADO do seed passam a nascer via fecharVenda() (M25, formulário único
    // de venda) em vez do caminho legado Lead.fechar(Plano), removido junto com Plano.
    private void seedLeadFechado(String nome, String email, Colaborador vendedor,
                                  ProdutoVenda produtoVenda, BigDecimal valorTotalVenda) {
        Lead lead = new Lead(nome, email, null, null);
        lead.moverParaEmContato(vendedor);
        lead.moverParaProposta();
        lead.fecharVenda(produtoVenda, OrigemVenda.DIRETA, valorTotalVenda, null, FormaPagamento.PIX);
        leadRepository.save(lead);
    }

    private void seedMentoriasEAtas() {
        if (mentoriaRepository.count() > 0) {
            return;
        }
        Colaborador gestaoPerf = buscarColaboradorPorNome("Gestão de Performance");
        Colaborador ricardo = buscarColaboradorPorNome("Ricardo Costa");
        Mentorado joao = buscarMentoradoPorNome("João Silva");
        Mentorado ana = buscarMentoradoPorNome("Ana Costa");
        Mentorado carlos = buscarMentoradoPorNome("Carlos Menezes");
        Mentorado rafael = buscarMentoradoPorNome("Rafael Gomes");
        Mentorado fernanda = buscarMentoradoPorNome("Fernanda Lima");
        Mentorado marina = buscarMentoradoPorNome("Marina Souza");
        if (gestaoPerf == null || ricardo == null || joao == null) {
            return; // seedColaboradores/seedMentorados não rodaram (ex.: banco já tinha dado de outra fonte)
        }

        // 1) Mentoria individual já REALIZADA, com ata PUBLICADA (IA "rodou" — dado simulado,
        // não chamou a API de verdade) — demonstra o fluxo completo pronto pra apresentação.
        Mentoria m1 = new Mentoria(TipoMentoria.INDIVIDUAL, gestaoPerf, Set.of(joao),
                Instant.parse("2026-07-02T14:00:00Z"), 60, "https://meet.google.com/joao-gestao-perf", null);
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

        // 3) Próxima mentoria confirmada (futura) — aparece na agenda, sem ata ainda. Relativa a
        // Instant.now() de propósito (achado ao vivo na Fase 5: era uma data fixa —
        // 2026-07-15T15:00 — que "andou pra trás" conforme o tempo real passou, até coincidir
        // com o próprio dia do teste e virar "dentro da janela de 10min" quando devia estar
        // "vários dias no futuro", quebrando mentorias.spec.ts de forma intermitente).
        Mentoria m3 = new Mentoria(TipoMentoria.INDIVIDUAL, gestaoPerf, Set.of(rafael),
                Instant.now().plus(Duration.ofDays(10)), 60, "https://meet.google.com/rafael-gestao-perf", null);
        m3.confirmar();
        mentoriaRepository.save(m3);

        // 4) Mentoria agendada (ainda não confirmada), em grupo. Mesmo raciocínio relativo do m3
        // acima — sempre depois de m3 na linha do tempo, como o número de ordem já sugeria.
        Mentoria m4 = new Mentoria(TipoMentoria.GRUPO, ricardo, Set.of(fernanda, marina),
                Instant.now().plus(Duration.ofDays(15)), 90, null, "SAW HUB — Sala de reuniões");
        mentoriaRepository.save(m4);
    }

    private Colaborador buscarColaboradorPorNome(String nome) {
        return colaboradorRepository.findAll().stream()
                .filter(c -> c.getNome().equals(nome))
                .findFirst()
                .orElse(null);
    }

    private Mentorado buscarMentoradoPorNome(String nome) {
        return mentoradoRepository.buscarComFiltro(null, null).stream()
                .filter(m -> m.getNome().equals(nome))
                .findFirst()
                .orElse(null);
    }

    // 22/07/2026 — busca em vez de criar: seedFinanceiro() usa só subcategorias reais confirmadas
    // via raio-x (V52), nunca inventa categoria nova (ver comentário em seedFinanceiro()).
    private CategoriaFinanceira buscarCategoriaFinanceiraPorNome(String nome) {
        return categoriaFinanceiraRepository.findByNomeIgnoreCase(nome).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Categoria " + nome + " (V52) não encontrada."));
    }

    private void seedAvisos() {
        if (avisoRepository.count() > 0) {
            return;
        }
        avisoRepository.save(new Aviso("Nova mentoria em grupo disponível", "Inscrições abertas pra Liderança que Gera Resultados.",
                CategoriaAviso.MENTORIAS));
        avisoRepository.save(new Aviso("Novo material na biblioteca", "Planilha de Fluxo de Caixa Diário já disponível pra download.",
                CategoriaAviso.MATERIAIS));
        avisoRepository.save(new Aviso("Workshop de Precificação Estratégica", "Vagas limitadas — garanta a sua.",
                CategoriaAviso.EVENTOS));
        avisoRepository.save(new Aviso("Manutenção programada", "A plataforma ficará indisponível por 30 minutos no domingo às 2h.",
                CategoriaAviso.GERAL));
    }

    private void seedConteudos() {
        if (conteudoRepository.count() > 0) {
            return;
        }
        criarConteudo("Ficha técnica — modelo", TipoConteudo.PLANILHA, true, null);
        criarConteudo("Manual da Cultura SAW", TipoConteudo.DOCUMENTO, true, null);
        // H6.3 — duração real (não fictícia por acaso): "Como calcular seu DRE" é o único vídeo
        // seedado, dá pro indicador "minutos assistidos" mostrar um número plausível em demo.
        criarConteudo("Como calcular seu DRE", TipoConteudo.VIDEO, true, 12);
        criarConteudo("Apresentação: Precificação estratégica", TipoConteudo.APRESENTACAO, false, null);
    }

    // Devolve o Conteudo salvo (M12: seedMentoriasEAtas usa o retorno pra associar materiaisRecomendados).
    private Conteudo criarConteudo(String titulo, TipoConteudo tipo, boolean publicado, Integer duracaoMinutos) {
        Conteudo conteudo = new Conteudo(titulo, tipo, "https://cdn.sawhub.com.br/conteudos/" + tipo.name().toLowerCase());
        if (publicado) {
            conteudo.publicar();
        }
        conteudo.definirDuracaoMinutos(duracaoMinutos);
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
        // vendaEmAtacado=true de propósito: único produto do seed que faz sentido comprar em mais
        // de uma unidade (ex.: presentear a equipe) — fixture também usado pelo E2E de ajuste de
        // quantidade no carrinho (loja.spec.ts).
        criarProduto("Pacote de Planilhas Gerenciais",
                "12 planilhas prontas para gestão completa do seu restaurante.", CategoriaProduto.PLANILHA,
                "97.00", "197.00", "4.8", true, true, true);
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
                        "Pedido " + pedido.getId(), pedido.getValorTotal(), LocalDate.now(), StatusLancamento.REALIZADO)));
    }

    private Produto criarProduto(String titulo, String descricao, CategoriaProduto categoria, String preco,
                                  String precoOriginal, String avaliacaoMedia, boolean destaque, boolean publicado) {
        return criarProduto(titulo, descricao, categoria, preco, precoOriginal, avaliacaoMedia, destaque, publicado, false);
    }

    private Produto criarProduto(String titulo, String descricao, CategoriaProduto categoria, String preco,
                                  String precoOriginal, String avaliacaoMedia, boolean destaque, boolean publicado,
                                  boolean vendaEmAtacado) {
        Produto produto = new Produto(titulo, descricao, categoria, new BigDecimal(preco),
                precoOriginal == null ? null : new BigDecimal(precoOriginal),
                avaliacaoMedia == null ? null : new BigDecimal(avaliacaoMedia),
                destaque, "https://cdn.sawhub.com.br/produtos/" + categoria.name().toLowerCase() + ".zip", null,
                vendaEmAtacado);
        if (publicado) {
            produto.publicar();
        }
        return produtoRepository.save(produto);
    }
}
