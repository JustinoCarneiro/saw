package com.sawhub.hub.mentorado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

/** M23 — mesmo padrão de defesa em profundidade do AudioStorageService (M06): allow-list de
 * extensão + checagem de content-type, nunca confiando só no nome escolhido pelo cliente. Aqui
 * só PDF é aceito (documento de contrato assinado). */
class ContratoDocumentoStorageServiceTest {

    @TempDir
    Path tempDir;

    private ContratoDocumentoStorageService service() {
        return new ContratoDocumentoStorageService(tempDir.toString());
    }

    @Test
    void salvaPdfValido() throws IOException {
        var arquivo = new MockMultipartFile("arquivo", "contrato.pdf", "application/pdf", "conteudo-fake".getBytes());

        String url = service().salvar(UUID.randomUUID(), arquivo);

        assertThat(url).endsWith(".pdf");
        assertThat(java.nio.file.Files.exists(tempDir.resolve(url))).isTrue();
    }

    @Test
    void rejeitaContentTypeQueNaoEPdf() {
        var arquivo = new MockMultipartFile("arquivo", "contrato.pdf", "image/png", "conteudo".getBytes());

        assertThatThrownBy(() -> service().salvar(UUID.randomUUID(), arquivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PDF");
    }

    @Test
    void rejeitaExtensaoForaDaAllowList() {
        var arquivo = new MockMultipartFile("arquivo", "script.php", "application/pdf", "conteudo".getBytes());

        assertThatThrownBy(() -> service().salvar(UUID.randomUUID(), arquivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Extensão");
    }

    @Test
    void rejeitaArquivoSemContentType() {
        var arquivo = new MockMultipartFile("arquivo", "contrato.pdf", null, "conteudo".getBytes());

        assertThatThrownBy(() -> service().salvar(UUID.randomUUID(), arquivo))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // Achado (médio) do revisor-seguranca do M23: o único teto de tamanho era o global
    // multipart.max-file-size (150MB, dimensionado pro áudio de mentoria do M06) — grande demais
    // pra um PDF de contrato, e nada aqui impunha um limite dedicado.
    @Test
    void rejeitaArquivoAcimaDoLimite() {
        byte[] conteudoGrande = new byte[16 * 1024 * 1024 + 1];
        var arquivo = new MockMultipartFile("arquivo", "contrato.pdf", "application/pdf", conteudoGrande);

        assertThatThrownBy(() -> service().salvar(UUID.randomUUID(), arquivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("15MB");
    }

    @Test
    void resolverRetornaCaminhoDentroDoDiretorioBase() {
        Path resolvido = service().resolver("algum-arquivo.pdf");

        assertThat(resolvido).isEqualTo(tempDir.resolve("algum-arquivo.pdf"));
    }
}
