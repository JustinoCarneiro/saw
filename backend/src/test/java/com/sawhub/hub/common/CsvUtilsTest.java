package com.sawhub.hub.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class CsvUtilsTest {

    @Test
    void detectaDelimitadorPontoEVirgulaQuandoPresenteNoCabecalho() {
        assertThat(CsvUtils.detectarDelimitador("tipo;categoria;valor")).isEqualTo(';');
    }

    @Test
    void detectaDelimitadorVirgulaQuandoSemPontoEVirgula() {
        assertThat(CsvUtils.detectarDelimitador("tipo,categoria,valor")).isEqualTo(',');
    }

    @Test
    void parseValorComVirgulaDecimalQuandoDelimitadorEhPontoEVirgula() {
        assertThat(CsvUtils.parseValor("1234,56", ';')).isEqualByComparingTo("1234.56");
    }

    @Test
    void parseValorComPontoDecimalQuandoDelimitadorEhVirgula() {
        assertThat(CsvUtils.parseValor("1234.56", ',')).isEqualByComparingTo("1234.56");
    }

    @Test
    void parseValorRejeitaTextoInvalido() {
        assertThatThrownBy(() -> CsvUtils.parseValor("abc", ','))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("abc");
    }

    @Test
    void parseValorRejeitaBranco() {
        assertThatThrownBy(() -> CsvUtils.parseValor("  ", ','))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseDataAceitaFormatoPtBr() {
        assertThat(CsvUtils.parseData("10/07/2026")).isEqualTo(LocalDate.of(2026, 7, 10));
    }

    @Test
    void parseDataRejeitaFormatoIso() {
        assertThatThrownBy(() -> CsvUtils.parseData("2026-07-10"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dd/MM/yyyy");
    }

    @Test
    void formatarDataProduzFormatoPtBr() {
        assertThat(CsvUtils.formatarData(LocalDate.of(2026, 7, 10))).isEqualTo("10/07/2026");
    }

    @Test
    void neutralizarFormulaPrefixaCamposQueComecamComSinalDeFormula() {
        assertThat(CsvUtils.neutralizarFormula("=SOMA(A1:A2)")).isEqualTo("'=SOMA(A1:A2)");
        assertThat(CsvUtils.neutralizarFormula("+55 11 99999")).isEqualTo("'+55 11 99999");
        assertThat(CsvUtils.neutralizarFormula("-10")).isEqualTo("'-10");
        assertThat(CsvUtils.neutralizarFormula("@usuario")).isEqualTo("'@usuario");
    }

    @Test
    void neutralizarFormulaNaoAlteraTextoNormal() {
        assertThat(CsvUtils.neutralizarFormula("Assinatura João Silva")).isEqualTo("Assinatura João Silva");
    }

    @Test
    void neutralizarFormulaAceitaNuloEVazio() {
        assertThat(CsvUtils.neutralizarFormula(null)).isNull();
        assertThat(CsvUtils.neutralizarFormula("")).isEmpty();
    }

    @Test
    void parseValorAceitaBigDecimalIgual() {
        assertThat(CsvUtils.parseValor("0.01", ',')).isEqualByComparingTo(new BigDecimal("0.01"));
    }

    // Achados do revisor-seguranca (M21): tamanho e content-type precisam ser checados ANTES de
    // qualquer parsing, não só a extensão.

    @Test
    void exigirArquivoCsvAceitaArquivoValido() {
        var arquivo = new MockMultipartFile("arquivo", "dados.csv", "text/csv", "conteudo".getBytes());
        assertThatCode(() -> CsvUtils.exigirArquivoCsv(arquivo)).doesNotThrowAnyException();
    }

    @Test
    void exigirArquivoCsvRejeitaArquivoVazio() {
        var arquivo = new MockMultipartFile("arquivo", "dados.csv", "text/csv", new byte[0]);
        assertThatThrownBy(() -> CsvUtils.exigirArquivoCsv(arquivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nenhum arquivo");
    }

    @Test
    void exigirArquivoCsvRejeitaAcimaDe2Mb() {
        var arquivo = new MockMultipartFile("arquivo", "dados.csv", "text/csv", new byte[2 * 1024 * 1024 + 1]);
        assertThatThrownBy(() -> CsvUtils.exigirArquivoCsv(arquivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tamanho máximo");
    }

    @Test
    void exigirArquivoCsvRejeitaExtensaoDiferenteDeCsv() {
        var arquivo = new MockMultipartFile("arquivo", "dados.xlsx", "text/csv", "conteudo".getBytes());
        assertThatThrownBy(() -> CsvUtils.exigirArquivoCsv(arquivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(".csv");
    }

    @Test
    void exigirArquivoCsvRejeitaContentTypeNaoPermitido() {
        var arquivo = new MockMultipartFile("arquivo", "dados.csv", "application/pdf", "conteudo".getBytes());
        assertThatThrownBy(() -> CsvUtils.exigirArquivoCsv(arquivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tipo de arquivo");
    }

    @Test
    void exigirArquivoCsvAceitaVariacoesRealistasDeContentType() {
        assertThatCode(() -> CsvUtils.exigirArquivoCsv(
                new MockMultipartFile("arquivo", "dados.csv", "text/plain", "x".getBytes())))
                .doesNotThrowAnyException();
        assertThatCode(() -> CsvUtils.exigirArquivoCsv(
                new MockMultipartFile("arquivo", "dados.csv", "application/vnd.ms-excel", "x".getBytes())))
                .doesNotThrowAnyException();
        assertThatCode(() -> CsvUtils.exigirArquivoCsv(
                new MockMultipartFile("arquivo", "dados.csv", "application/octet-stream", "x".getBytes())))
                .doesNotThrowAnyException();
    }
}
