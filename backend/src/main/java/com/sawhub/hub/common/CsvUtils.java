package com.sawhub.hub.common;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.commons.csv.CSVRecord;
import org.springframework.web.multipart.MultipartFile;

/** M21 — utilitários compartilhados entre todos os CsvServices do projeto.
 * M23: estendido com helpers de boolean/Instant/int/URL/e-mail pra cobrir os módulos restantes. */
public final class CsvUtils {

    private static final DateTimeFormatter DATA_PT_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA_PT_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Set<Character> PREFIXOS_FORMULA = Set.of('=', '+', '-', '@');

    // Achado (médio) do revisor-seguranca do M21: sem um teto dedicado, o limite de 5.000 linhas
    // só era checado DEPOIS de o arquivo inteiro (até os 150MB do limite global do servlet,
    // dimensionado pro áudio do M06) já ter sido lido, decodificado e parseado inteiro em memória
    // — o custo de memória já tinha sido pago antes da rejeição. 2MB é generoso pro teto de 5.000
    // linhas de CSV financeiro (bem acima da média realista por linha) e barato de checar antes de
    // qualquer parsing.
    private static final long TAMANHO_MAXIMO_BYTES = 2L * 1024 * 1024;

    // Achado (baixo) do revisor-seguranca do M21: exigirCsv() só validava a extensão, divergindo
    // do que o próprio Blueprint (ROADMAP.md) documentava como "mesmo padrão de defesa em
    // profundidade do AudioStorageService (M06)", que checa extensão E content-type. Allow-list
    // permissiva de propósito — diferente de áudio, o content-type de CSV varia bastante entre
    // navegador/SO (alguns mandam text/plain ou application/octet-stream pra um .csv legítimo) e
    // o arquivo nunca é executado, só parseado como dado — o ganho de segurança de ser mais
    // rígido aqui é baixo, o risco real é só usabilidade (rejeitar upload legítimo).
    private static final Set<String> CONTENT_TYPES_PERMITIDOS = Set.of(
            "text/csv", "application/csv", "application/vnd.ms-excel", "text/plain", "application/octet-stream");

    private CsvUtils() {
    }

    public static void exigirArquivoCsv(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Nenhum arquivo enviado.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new IllegalArgumentException("Arquivo excede o tamanho máximo permitido (2MB).");
        }
        String nome = arquivo.getOriginalFilename();
        if (nome == null || !nome.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new IllegalArgumentException("Arquivo precisa ter extensão .csv.");
        }
        String contentType = arquivo.getContentType();
        if (contentType == null || !CONTENT_TYPES_PERMITIDOS.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Tipo de arquivo não suportado. Envie um CSV (.csv).");
        }
    }

    // Excel pt-BR exporta CSV com ";" como delimitador e "," como separador decimal (já que "," é
    // o separador decimal em pt-BR); Google Sheets/ferramentas internacionais usam ",". Detectado
    // pela linha de cabeçalho pra aceitar os dois sem pedir configuração manual.
    public static char detectarDelimitador(String primeiraLinha) {
        return primeiraLinha != null && primeiraLinha.contains(";") ? ';' : ',';
    }

    public static BigDecimal parseValor(String bruto, char delimitador) {
        if (bruto == null || bruto.isBlank()) {
            throw new IllegalArgumentException("Valor em branco.");
        }
        String normalizado = delimitador == ';' ? bruto.trim().replace(",", ".") : bruto.trim();
        try {
            return new BigDecimal(normalizado);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor \"" + bruto + "\" inválido.");
        }
    }

    public static LocalDate parseData(String bruto) {
        if (bruto == null || bruto.isBlank()) {
            throw new IllegalArgumentException("Data em branco.");
        }
        try {
            return LocalDate.parse(bruto.trim(), DATA_PT_BR);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data \"" + bruto + "\" inválida (use dd/MM/yyyy).");
        }
    }

    public static String formatarData(LocalDate data) {
        return data.format(DATA_PT_BR);
    }

    // Mitigação padrão OWASP de CSV/Formula Injection: um campo de texto livre que comece com um
    // desses caracteres pode ser interpretado como fórmula pelo Excel ao abrir o arquivo
    // exportado — prefixar com aspas simples neutraliza sem alterar o valor visível ao usuário.
    public static String neutralizarFormula(String valor) {
        if (valor != null && !valor.isEmpty() && PREFIXOS_FORMULA.contains(valor.charAt(0))) {
            return "'" + valor;
        }
        return valor;
    }

    // --- M23: helpers adicionais para os módulos restantes ---

    /** Aceita true/false/sim/nao/1/0 (case-insensitive). */
    public static boolean parseBooleano(String bruto, String rotulo) {
        if (bruto == null || bruto.isBlank()) {
            return false;
        }
        String normalizado = bruto.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(normalizado) || "sim".equals(normalizado) || "1".equals(normalizado)) {
            return true;
        }
        if ("false".equals(normalizado) || "nao".equals(normalizado) || "não".equals(normalizado) || "0".equals(normalizado)) {
            return false;
        }
        throw new IllegalArgumentException(rotulo + " \"" + bruto + "\" inválido (use true/false).");
    }

    /** Formato dd/MM/yyyy HH:mm, fuso America/Sao_Paulo → Instant. */
    public static Instant parseInstant(String bruto, String fuso) {
        if (bruto == null || bruto.isBlank()) {
            throw new IllegalArgumentException("Data/hora em branco.");
        }
        try {
            LocalDateTime ldt = LocalDateTime.parse(bruto.trim(), DATA_HORA_PT_BR);
            return ldt.atZone(ZoneId.of(fuso)).toInstant();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data/hora \"" + bruto + "\" inválida (use dd/MM/yyyy HH:mm).");
        }
    }

    public static String formatarInstant(Instant instant, String fuso) {
        return instant.atZone(ZoneId.of(fuso)).format(DATA_HORA_PT_BR);
    }

    public static Integer parseIntOpcional(String bruto, String rotulo) {
        if (bruto == null || bruto.isBlank()) {
            return null;
        }
        try {
            int valor = Integer.parseInt(bruto.trim());
            if (valor <= 0) {
                throw new IllegalArgumentException(rotulo + " deve ser maior que zero.");
            }
            return valor;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(rotulo + " \"" + bruto + "\" inválido.");
        }
    }

    public static void exigirUrl(String bruto, String rotulo) {
        if (bruto == null || bruto.isBlank()) {
            throw new IllegalArgumentException(rotulo + " em branco.");
        }
        if (!bruto.trim().matches("^https?://.+")) {
            throw new IllegalArgumentException(rotulo + " inválida (use http:// ou https://).");
        }
        if (bruto.trim().length() > 500) {
            throw new IllegalArgumentException(rotulo + " excede 500 caracteres.");
        }
    }

    public static void exigirUrlOpcional(String bruto, String rotulo) {
        if (bruto != null && !bruto.isBlank()) {
            exigirUrl(bruto, rotulo);
        }
    }

    /** Normaliza e-mail: trim + lowercase. */
    public static String normalizarEmail(String bruto) {
        if (bruto == null || bruto.isBlank()) {
            throw new IllegalArgumentException("E-mail em branco.");
        }
        return bruto.trim().toLowerCase(Locale.ROOT);
    }

    public static <E extends Enum<E>> E parseEnum(Class<E> tipo, String bruto, String rotulo) {
        if (bruto == null || bruto.isBlank()) {
            throw new IllegalArgumentException(rotulo + " em branco.");
        }
        try {
            return Enum.valueOf(tipo, bruto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(rotulo + " \"" + bruto + "\" inválido.");
        }
    }

    public static <E extends Enum<E>> E parseEnumOpcional(Class<E> tipo, String bruto, String rotulo, E padrao) {
        if (bruto == null || bruto.isBlank()) {
            return padrao;
        }
        return parseEnum(tipo, bruto, rotulo);
    }

    public static LocalDate parseDataOpcional(String bruto) {
        if (bruto == null || bruto.isBlank()) {
            return null;
        }
        return parseData(bruto);
    }

    public static void exigirColunas(List<String> cabecalhoEncontrado, String[] esperadas) {
        for (String esperada : esperadas) {
            if (!cabecalhoEncontrado.contains(esperada)) {
                throw new IllegalArgumentException(
                        "CSV sem a coluna \"" + esperada + "\". Colunas esperadas: " + String.join(", ", esperadas));
            }
        }
    }

    public static String lerConteudo(MultipartFile arquivo) {
        try {
            return new String(arquivo.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler o arquivo.", e);
        }
    }

    public static void exigirNaoVazio(String valor, String rotulo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(rotulo + " em branco.");
        }
    }

    public static void exigirTamanhoMaximo(String valor, int max, String rotulo) {
        if (valor != null && valor.length() > max) {
            throw new IllegalArgumentException(rotulo + " excede " + max + " caracteres.");
        }
    }
}
