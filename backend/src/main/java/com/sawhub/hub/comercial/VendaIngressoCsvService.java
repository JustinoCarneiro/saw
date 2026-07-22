package com.sawhub.hub.comercial;

import com.sawhub.hub.common.CsvUtils;
import com.sawhub.hub.common.dto.ImportErro;
import com.sawhub.hub.common.dto.ImportResultResponse;
import com.sawhub.hub.evento.Evento;
import com.sawhub.hub.evento.EventoRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Change request pós-MVP (E13, "importação de planilhas de eventos passados pra popular
 * histórico", reunião 17/07/2026) — popula {@link VendaIngresso} a partir da planilha real
 * "Vendas Eventos" (uma aba por evento — por isso {@code eventoId} é parâmetro do import, não
 * coluna do CSV, ver docs/reuniao-2026-07-17-atualizacoes.md § "Planilhas reais do Comercial/
 * Financeiro").
 *
 * <p>Escopo deliberadamente estrutural, duas decisões conscientes:
 * <ul>
 *     <li>Não cria {@code LancamentoFinanceiro} — evita duplicar receita se o Admin já importou o
 *     total financeiro daqueles meses via {@code LancamentoCsvService} (M21); a fonte de verdade
 *     pro dinheiro histórico continua sendo o import financeiro, não este.</li>
 *     <li>Não chama {@code Evento.ocuparVaga()} — o evento já aconteceu, não faz sentido aplicar
 *     limite de capacidade retroativo a dado histórico (a "vaga" só importa pra vender o próximo
 *     ingresso, não pra contar o que já foi vendido).</li>
 * </ul>
 * Cada linha vira um {@link Lead} novo (nascido FECHADO, ver {@link Lead#criarFechadoParaImportacaoIngresso})
 * + N {@link VendaIngresso} (uma por unidade de "quantidade de ingressos"), mesmo padrão de
 * passada única/tudo-ou-nada dos demais CsvServices (M21/M22). */
@Service
public class VendaIngressoCsvService {

    private static final String[] CABECALHO_IMPORT = {
            "nomeAluno", "quantidadeIngressos", "valorLiquidoIngresso", "tipoIngresso", "email"
    };
    private static final int LIMITE_LINHAS = 2000;
    private static final Pattern EMAIL_SIMPLES = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    // Achado médio do revisor-seguranca: CsvUtils.parseIntOpcional só valida > 0, sem teto — sem
    // este limite, uma célula com "999999999" vira `new ArrayList<>(quantidade)` em parseVendas(),
    // tentando alocar ~2 bilhões de posições (OutOfMemoryError, não capturado pelo catch de
    // IllegalArgumentException). 500 é generoso pra qualquer venda real de ingresso numa linha só.
    private static final int QUANTIDADE_MAXIMA = 500;

    private final EventoRepository eventoRepository;
    private final LeadRepository leadRepository;
    private final VendaIngressoRepository vendaIngressoRepository;

    public VendaIngressoCsvService(EventoRepository eventoRepository, LeadRepository leadRepository,
                                    VendaIngressoRepository vendaIngressoRepository) {
        this.eventoRepository = eventoRepository;
        this.leadRepository = leadRepository;
        this.vendaIngressoRepository = vendaIngressoRepository;
    }

    @Transactional
    public ImportResultResponse importar(UUID eventoId, MultipartFile arquivo) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new IllegalArgumentException("Evento não encontrado."));

        CsvUtils.exigirArquivoCsv(arquivo);
        String conteudo = CsvUtils.lerConteudo(arquivo);
        String primeiraLinha = conteudo.lines().findFirst().orElse("");
        char delimitador = CsvUtils.detectarDelimitador(primeiraLinha);
        CSVFormat formato = CSVFormat.Builder.create()
                .setDelimiter(delimitador)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .build();

        List<CSVRecord> registros;
        try (CSVParser parser = CSVParser.parse(conteudo, formato)) {
            CsvUtils.exigirColunas(parser.getHeaderNames(), CABECALHO_IMPORT);
            registros = parser.getRecords();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível interpretar o CSV.", e);
        }

        if (registros.isEmpty()) {
            throw new IllegalArgumentException("Arquivo CSV sem nenhuma linha de dados.");
        }
        if (registros.size() > LIMITE_LINHAS) {
            throw new IllegalArgumentException("Arquivo excede o limite de " + LIMITE_LINHAS + " linhas.");
        }

        List<ImportErro> erros = new ArrayList<>();
        List<Lead> leadsParaSalvar = new ArrayList<>();
        List<List<VendaIngresso>> vendasPorLinha = new ArrayList<>();
        for (CSVRecord registro : registros) {
            int linha = (int) registro.getRecordNumber() + 1;
            try {
                Lead lead = parseLead(registro, delimitador);
                leadsParaSalvar.add(lead);
                vendasPorLinha.add(parseVendas(registro, lead, evento));
            } catch (IllegalArgumentException e) {
                erros.add(new ImportErro(linha, e.getMessage()));
            }
        }

        if (!erros.isEmpty()) {
            return new ImportResultResponse(registros.size(), 0, erros);
        }

        leadRepository.saveAll(leadsParaSalvar);
        int totalIngressos = 0;
        for (List<VendaIngresso> vendas : vendasPorLinha) {
            vendaIngressoRepository.saveAll(vendas);
            totalIngressos += vendas.size();
        }
        return new ImportResultResponse(registros.size(), totalIngressos, List.of());
    }

    private Lead parseLead(CSVRecord registro, char delimitador) {
        String nome = registro.get("nomeAluno");
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do aluno em branco.");
        }
        if (nome.length() > 120) {
            throw new IllegalArgumentException("Nome excede 120 caracteres.");
        }
        String email = registro.get("email");
        if (email == null || email.isBlank() || !EMAIL_SIMPLES.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("E-mail \"" + email + "\" inválido.");
        }
        // Achado baixo do revisor-seguranca: sem este teto, um valor além do VARCHAR real do
        // banco estoura DataIntegrityViolationException no save (409 genérico, mensagem enganosa
        // de "alterado por outra operação simultânea") em vez de um erro de linha claro — mesmos
        // limites de VendaIngressoRequest (path JSON de fecharVenda), que este CSV precisa
        // espelhar por ser um segundo caminho de escrita pra mesma entidade.
        CsvUtils.exigirTamanhoMaximo(email, 255, "E-mail");
        Integer quantidade = quantidade(registro);
        BigDecimal valorLiquido = CsvUtils.parseValor(registro.get("valorLiquidoIngresso"), delimitador);
        OrigemVenda origemVenda = mapearOrigem(colunaOpcional(registro, "origemVenda"));
        String telefone = colunaOpcional(registro, "telefone");
        CsvUtils.exigirTamanhoMaximo(telefone, 20, "Telefone");

        return Lead.criarFechadoParaImportacaoIngresso(nome.trim(), email.trim(), telefone, origemVenda,
                valorLiquido.multiply(BigDecimal.valueOf(quantidade)));
    }

    private List<VendaIngresso> parseVendas(CSVRecord registro, Lead lead, Evento evento) {
        String nome = registro.get("nomeAluno").trim();
        Integer quantidade = quantidade(registro);
        CategoriaIngresso categoria = CsvUtils.parseEnum(CategoriaIngresso.class, registro.get("tipoIngresso"), "Tipo de ingresso");
        String nomeEmpresa = colunaOpcional(registro, "nomeEmpresa");
        CsvUtils.exigirTamanhoMaximo(nomeEmpresa, 255, "Nome da empresa");
        String telefone = colunaOpcional(registro, "telefone");
        String email = registro.get("email").trim();

        List<VendaIngresso> vendas = new ArrayList<>(quantidade);
        for (int i = 0; i < quantidade; i++) {
            vendas.add(new VendaIngresso(lead, evento, categoria, nome, null, false, nomeEmpresa, telefone, email));
        }
        return vendas;
    }

    private static Integer quantidade(CSVRecord registro) {
        Integer quantidade = CsvUtils.parseIntOpcional(registro.get("quantidadeIngressos"), "Quantidade de ingressos");
        if (quantidade == null) {
            throw new IllegalArgumentException("Quantidade de ingressos em branco ou inválida.");
        }
        if (quantidade > QUANTIDADE_MAXIMA) {
            throw new IllegalArgumentException("Quantidade de ingressos excede o máximo de " + QUANTIDADE_MAXIMA + ".");
        }
        return quantidade;
    }

    // "Origem da Venda" real mistura canal (CORTESIA/HOTMART/PARCEIRO) com nome de vendedora — só
    // os 3 canais têm valor de enum equivalente (docs/reuniao-2026-07-17-atualizacoes.md §
    // "Implicações pro desenho"); qualquer outro texto (nome de vendedor) vira DIRETA, mesmo
    // critério já confirmado pelo Marcos pra venda direta comercial (Lead.vendedor cobre o nome,
    // não faz sentido aqui — o lead do import nunca tem vendedor interno atribuído).
    private static OrigemVenda mapearOrigem(String bruto) {
        if (bruto == null || bruto.isBlank()) {
            return OrigemVenda.DIRETA;
        }
        String normalizado = bruto.trim().toUpperCase(Locale.ROOT);
        return switch (normalizado) {
            case "CORTESIA" -> OrigemVenda.CORTESIA;
            case "HOTMART" -> OrigemVenda.HOTMART;
            case "PARCEIRO" -> OrigemVenda.PARCEIRO;
            case "PATROCINIO", "PATROCÍNIO" -> OrigemVenda.PATROCINIO;
            case "PALESTRANTE" -> OrigemVenda.PALESTRANTE;
            default -> OrigemVenda.DIRETA;
        };
    }

    private static String colunaOpcional(CSVRecord registro, String nome) {
        if (!registro.isMapped(nome)) {
            return null;
        }
        String valor = registro.get(nome);
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
