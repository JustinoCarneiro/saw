package com.sawhub.hub.mentoria;

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

/** Armazenamento do áudio da mentoria em disco da própria VPS (decisão do ROADMAP.md M06 — object
 * storage dedicado não se justifica na escala do MVP). O diretório é um volume Docker nomeado
 * (docker-compose.full.yml: backend_audios:/app/data) — sobrevive a rebuild/redeploy do container.
 * Pendência de infra real (fora do repo): incluir esse volume na rotina de backup da VPS (Coolify),
 * o mesmo tratamento dado ao Postgres (ver CLAUDE.md § Backup) — hoje só o banco está coberto. */
@Service
public class AudioStorageService {

    // Achado (médio) da revisão de segurança do M06: sem allow-list, o nome do arquivo enviado
    // pelo cliente virava extensão do arquivo salvo em disco sem checagem nenhuma (podia ser
    // .php/.html), e sem checar content-type, qualquer binário podia ser reencaminhado como
    // áudio pra Whisper API. As duas checagens juntas (extensão + content-type) são defesa em
    // profundidade suficiente pra uma ferramenta interna de admin — não é upload público.
    private static final Set<String> EXTENSOES_PERMITIDAS = Set.of(".mp3", ".wav", ".m4a", ".ogg", ".webm", ".aac", ".flac");

    private final Path baseDir;

    public AudioStorageService(@Value("${sawhub.audio.storage-dir:./data/audios}") String storageDir) {
        this.baseDir = Path.of(storageDir);
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível preparar o diretório de áudios: " + baseDir, e);
        }
    }

    /** Retorna a URL/caminho relativo salvo em {@code Ata.audioUrl} — nunca o caminho absoluto,
     * pra não vazar estrutura de disco do servidor pro cliente HTTP. */
    public String salvar(UUID ataId, MultipartFile arquivo) {
        exigirTipoPermitido(arquivo);
        String extensao = extensaoDe(arquivo.getOriginalFilename());
        String nomeArquivo = ataId + "-" + System.currentTimeMillis() + extensao;
        try {
            Files.copy(arquivo.getInputStream(), baseDir.resolve(nomeArquivo));
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível salvar o áudio.", e);
        }
        return nomeArquivo;
    }

    public Path resolver(String audioUrl) {
        return baseDir.resolve(audioUrl);
    }

    private static void exigirTipoPermitido(MultipartFile arquivo) {
        String contentType = arquivo.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("audio/")) {
            throw new IllegalArgumentException("Arquivo precisa ser um áudio (content-type audio/*).");
        }
        String extensao = extensaoDe(arquivo.getOriginalFilename());
        if (!EXTENSOES_PERMITIDAS.contains(extensao.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(
                    "Extensão de arquivo não suportada. Use: " + String.join(", ", EXTENSOES_PERMITIDAS));
        }
    }

    private static String extensaoDe(String nomeOriginal) {
        if (nomeOriginal == null || !nomeOriginal.contains(".")) {
            return "";
        }
        return nomeOriginal.substring(nomeOriginal.lastIndexOf('.'));
    }
}
