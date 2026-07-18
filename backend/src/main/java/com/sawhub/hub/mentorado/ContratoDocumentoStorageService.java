package com.sawhub.hub.mentorado;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** M23 — armazenamento do PDF do documento de contrato em disco da própria VPS, mesmo padrão de
 * defesa em profundidade do {@code AudioStorageService} (M06): allow-list de extensão + checagem
 * de content-type. Diretório precisa entrar como volume Docker nomeado em
 * {@code docker-compose.full.yml} (mesma pendência de backup já registrada pro M06 — ver
 * CLAUDE.md § Backup, hoje só o Postgres está coberto). */
@Service
public class ContratoDocumentoStorageService {

    private static final Set<String> EXTENSOES_PERMITIDAS = Set.of(".pdf");

    // Achado (médio) da revisão de segurança do M23: o único teto de tamanho era o global
    // spring.servlet.multipart.max-file-size (150MB, dimensionado pro áudio de mentoria do M06,
    // que pode durar até 1h) — grande demais pra um PDF de contrato assinado, que fica na casa de
    // poucos MB. Sem um limite dedicado, uma conta autorizada (Modulo.COMERCIAL) podia esgotar
    // disco da VPS enviando repetidamente arquivos rotulados .pdf até o teto global.
    private static final long TAMANHO_MAXIMO_BYTES = 15L * 1024 * 1024;

    private final Path baseDir;

    public ContratoDocumentoStorageService(@Value("${sawhub.contrato.storage-dir:./data/contratos}") String storageDir) {
        this.baseDir = Path.of(storageDir);
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível preparar o diretório de contratos: " + baseDir, e);
        }
    }

    /** Retorna a URL/caminho relativo salvo em {@code Mentorado.documentoContratoUrl} — nunca o
     * caminho absoluto, pra não vazar estrutura de disco do servidor pro cliente HTTP. */
    public String salvar(UUID mentoradoId, MultipartFile arquivo) {
        exigirTipoPermitido(arquivo);
        String extensao = extensaoDe(arquivo.getOriginalFilename());
        String nomeArquivo = mentoradoId + "-" + System.currentTimeMillis() + extensao;
        try {
            Files.copy(arquivo.getInputStream(), baseDir.resolve(nomeArquivo));
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível salvar o documento do contrato.", e);
        }
        return nomeArquivo;
    }

    public Path resolver(String documentoContratoUrl) {
        return baseDir.resolve(documentoContratoUrl);
    }

    private static void exigirTipoPermitido(MultipartFile arquivo) {
        String contentType = arquivo.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).equals("application/pdf")) {
            throw new IllegalArgumentException("Arquivo precisa ser um PDF (content-type application/pdf).");
        }
        String extensao = extensaoDe(arquivo.getOriginalFilename());
        if (!EXTENSOES_PERMITIDAS.contains(extensao.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(
                    "Extensão de arquivo não suportada. Use: " + String.join(", ", EXTENSOES_PERMITIDAS));
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new IllegalArgumentException("Arquivo muito grande. Tamanho máximo: 15MB.");
        }
    }

    private static String extensaoDe(String nomeOriginal) {
        if (nomeOriginal == null || !nomeOriginal.contains(".")) {
            return "";
        }
        return nomeOriginal.substring(nomeOriginal.lastIndexOf('.'));
    }
}
