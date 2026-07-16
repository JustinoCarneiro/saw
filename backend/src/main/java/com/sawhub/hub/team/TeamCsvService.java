package com.sawhub.hub.team;

import com.sawhub.hub.common.CsvUtils;
import com.sawhub.hub.common.dto.ImportErro;
import com.sawhub.hub.common.dto.ImportResultResponse;
import com.sawhub.hub.security.Perfil;
import com.sawhub.hub.security.Usuario;
import com.sawhub.hub.security.UsuarioRepository;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** M23 — export/import CSV de {@link Colaborador}. Import CRIA colaboradores novos (mesma lógica
 * de {@code TeamService.criar()}) — gera {@link Usuario} com senha informada no CSV. */
@Service
public class TeamCsvService {

    private static final String[] CABECALHO_EXPORT = {"nome", "email", "area"};
    private static final String[] CABECALHO_IMPORT = {"nome", "email", "senha", "area"};
    private static final int LIMITE_LINHAS = 5000;

    private final ColaboradorRepository colaboradorRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public TeamCsvService(ColaboradorRepository colaboradorRepository, UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder) {
        this.colaboradorRepository = colaboradorRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String exportar() {
        List<Colaborador> colaboradores = colaboradorRepository.findAll();
        StringWriter destino = new StringWriter();
        CSVFormat formato = CSVFormat.Builder.create().setDelimiter(';').setHeader(CABECALHO_EXPORT).build();
        try (CSVPrinter printer = new CSVPrinter(destino, formato)) {
            for (Colaborador c : colaboradores) {
                printer.printRecord(
                        CsvUtils.neutralizarFormula(c.getNome()),
                        c.getUsuario().getEmail(),
                        c.getArea().name());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível gerar o CSV.", e);
        }
        return destino.toString();
    }

    @Transactional
    public ImportResultResponse importar(MultipartFile arquivo) {
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
        List<LinhaValidada> validas = new ArrayList<>();
        Set<String> emailsNoArquivo = new HashSet<>();
        for (CSVRecord registro : registros) {
            int linha = (int) registro.getRecordNumber() + 1;
            try {
                LinhaValidada v = validarLinha(registro, emailsNoArquivo);
                validas.add(v);
                emailsNoArquivo.add(v.email());
            } catch (IllegalArgumentException e) {
                erros.add(new ImportErro(linha, e.getMessage()));
            }
        }

        if (!erros.isEmpty()) {
            return new ImportResultResponse(registros.size(), 0, erros);
        }

        for (LinhaValidada v : validas) {
            Usuario usuario = usuarioRepository.save(
                    new Usuario(v.email(), passwordEncoder.encode(v.senha()), Perfil.ADMIN));
            colaboradorRepository.save(new Colaborador(usuario, v.nome(), v.area()));
        }
        return new ImportResultResponse(registros.size(), validas.size(), List.of());
    }

    private LinhaValidada validarLinha(CSVRecord registro, Set<String> emailsJaVistosNoArquivo) {
        String nome = registro.get("nome");
        CsvUtils.exigirNaoVazio(nome, "Nome");

        String email = CsvUtils.normalizarEmail(registro.get("email"));
        if (!email.contains("@")) {
            throw new IllegalArgumentException("E-mail \"" + email + "\" inválido.");
        }
        if (emailsJaVistosNoArquivo.contains(email)) {
            throw new IllegalArgumentException("E-mail \"" + email + "\" duplicado no arquivo.");
        }
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("E-mail \"" + email + "\" já existe no sistema.");
        }

        String senha = registro.get("senha");
        CsvUtils.exigirNaoVazio(senha, "Senha");
        if (senha.trim().length() < 8) {
            throw new IllegalArgumentException("Senha deve ter ao menos 8 caracteres.");
        }

        Area area = CsvUtils.parseEnum(Area.class, registro.get("area"), "Área");

        return new LinhaValidada(nome.trim(), email, senha.trim(), area);
    }

    private record LinhaValidada(String nome, String email, String senha, Area area) {
    }
}
