package com.sawhub.hub.common;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Set;
import org.springframework.web.multipart.MultipartFile;

/** M21 — utilitários compartilhados entre {@code LancamentoCsvService}/{@code ContaCsvService}
 * (import/export CSV do Financeiro). */
public final class CsvUtils {

    private static final DateTimeFormatter DATA_PT_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
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
}
