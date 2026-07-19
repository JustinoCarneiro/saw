package com.sawhub.hub.mentorado;

import com.sawhub.hub.comercial.Lead;
import com.sawhub.hub.comercial.LeadRepository;
import com.sawhub.hub.comercial.ProdutoVenda;
import com.sawhub.hub.comercial.StatusLead;
import com.sawhub.hub.mentorado.dto.AtualizarDadosContratoRequest;
import com.sawhub.hub.mentorado.dto.AtualizarDiagnosticoInicialRequest;
import com.sawhub.hub.mentorado.dto.AtualizarMentoradoRequest;
import com.sawhub.hub.mentorado.dto.CriarMentoradoDiretoRequest;
import com.sawhub.hub.mentorado.dto.ImportarMentoradoDiretoLinha;
import com.sawhub.hub.security.Perfil;
import com.sawhub.hub.security.Usuario;
import com.sawhub.hub.security.UsuarioRepository;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** H11.1 — CRUD administrativo de mentorados; fecha a pendência deixada pelo M05 (Lead FECHADO
 * não cria conta de login sozinho). */
@Service
public class MentoradoAdminService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final MentoradoRepository mentoradoRepository;
    private final UsuarioRepository usuarioRepository;
    private final LeadRepository leadRepository;
    private final MentoradoDiagnosticoInicialRepository diagnosticoInicialRepository;
    private final ContratoDocumentoStorageService contratoDocumentoStorageService;
    private final PasswordEncoder passwordEncoder;

    public MentoradoAdminService(MentoradoRepository mentoradoRepository, UsuarioRepository usuarioRepository,
                                  LeadRepository leadRepository,
                                  MentoradoDiagnosticoInicialRepository diagnosticoInicialRepository,
                                  ContratoDocumentoStorageService contratoDocumentoStorageService,
                                  PasswordEncoder passwordEncoder) {
        this.mentoradoRepository = mentoradoRepository;
        this.usuarioRepository = usuarioRepository;
        this.leadRepository = leadRepository;
        this.diagnosticoInicialRepository = diagnosticoInicialRepository;
        this.contratoDocumentoStorageService = contratoDocumentoStorageService;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Mentorado> listar(Plano plano, StatusMentorado status, String busca) {
        return mentoradoRepository.buscarComFiltro(plano, status, busca);
    }

    @Transactional
    public Mentorado atualizar(UUID id, AtualizarMentoradoRequest request) {
        Mentorado mentorado = buscar(id);
        mentorado.atualizar(request.nome(), request.negocio(), request.plano());
        mentorado.definirVencimentoPlano(request.vencimentoPlano());
        // Fase 5 (H11.1) — Admin também pode preencher contato/bio/foto, além da autoedição do
        // próprio mentorado (H9.1) — mesmo método de domínio, dois chamadores.
        mentorado.atualizarPerfil(request.telefone(), request.bio(), request.fotoUrl());
        return mentoradoRepository.save(mentorado);
    }

    @Transactional
    public Mentorado ativar(UUID id) {
        Mentorado mentorado = buscar(id);
        mentorado.ativar();
        return mentoradoRepository.save(mentorado);
    }

    @Transactional
    public Mentorado desativar(UUID id) {
        Mentorado mentorado = buscar(id);
        mentorado.desativar();
        return mentoradoRepository.save(mentorado);
    }

    /** Único endpoint que efetivamente cria a conta de login do mentorado — nasce sempre de um
     * lead FECHADO (H11.1 fechando a pendência do M05), nunca de um cadastro avulso nesta leva. */
    @Transactional
    public MentoradoCriado criarAPartirDeLead(UUID leadId) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new IllegalArgumentException("Lead não encontrado."));
        if (lead.getStatus() != StatusLead.FECHADO) {
            throw new IllegalStateException("Só é possível criar mentorado a partir de um lead Fechado.");
        }
        if (lead.getMentorado() != null) {
            throw new IllegalStateException("Este lead já tem um mentorado vinculado.");
        }
        if (usuarioRepository.findByEmail(lead.getEmail()).isPresent()) {
            throw new IllegalStateException("Já existe uma conta com este e-mail.");
        }

        String senhaTemporaria = gerarSenhaTemporaria();
        Usuario usuario = usuarioRepository.save(
                new Usuario(lead.getEmail(), passwordEncoder.encode(senhaTemporaria), Perfil.MENTORADO));
        Plano plano = lead.getPlanoFechado() != null ? lead.getPlanoFechado() : Plano.GRATUITO;
        Mentorado mentorado = new Mentorado(usuario, lead.getNome(), null, plano, BigDecimal.ZERO, 0, 0);

        // M25 — Suposição 6 do Blueprint: quando o lead foi fechado via fecharVenda() (formulário
        // único de venda) com produto de mentoria/consultoria, propaga pro Mentorado em vez de
        // deixar o admin redigitar o mesmo dado que já foi preenchido na venda.
        TipoContrato tipoContrato = mapearTipoContrato(lead.getProdutoVenda());
        if (tipoContrato != null) {
            LocalDate dataFechamentoContrato = lead.getDataFechamento() != null
                    ? lead.getDataFechamento().atZone(ZoneOffset.UTC).toLocalDate() : null;
            mentorado.atualizarDadosContrato(null, null, null, tipoContrato, lead.getValorTotalVenda(),
                    dataFechamentoContrato);
        }

        mentorado = mentoradoRepository.save(mentorado);

        lead.vincularMentorado(mentorado);
        leadRepository.save(lead);

        return new MentoradoCriado(mentorado, senhaTemporaria);
    }

    // Venda de ingresso/produto digital não é um contrato de mentoria — sem mapeamento, o Lead
    // fica sem tipoContrato propagado (null é o retorno esperado, não um erro).
    private static TipoContrato mapearTipoContrato(ProdutoVenda produtoVenda) {
        if (produtoVenda == null) {
            return null;
        }
        return switch (produtoVenda) {
            case MENTORIA_CONTINUA -> TipoContrato.MENTORIA_CONTINUA;
            case MENTORIA_INDIVIDUAL -> TipoContrato.MENTORIA_INDIVIDUAL;
            case CONSULTORIA -> TipoContrato.CONSULTORIA;
            // FORMULA_SAW/FORMACAO_PROFISSIONAL/FICHA_TECNICA_LUCRATIVA são categoria própria de
            // ProdutoVenda (18-19/07/2026), mas ainda sem confirmação de que viram contrato de
            // Mentorado com vencimento — ficam sem mapear pra TipoContrato por enquanto, mesmo
            // tratamento de INGRESSO_EVENTO/PRODUTO_DIGITAL.
            case FORMULA_SAW, FORMACAO_PROFISSIONAL, FICHA_TECNICA_LUCRATIVA, INGRESSO_EVENTO, PRODUTO_DIGITAL -> null;
        };
    }

    /** M23 — "criar mentorado direto": ao contrário de {@link #criarAPartirDeLead}, não exige um
     * Lead pré-existente — cria um Lead já FECHADO ({@link Lead#criarJaFechado}) e na sequência
     * Usuario+Mentorado, mesma geração de senha temporária. */
    @Transactional
    public MentoradoCriado criarDireto(CriarMentoradoDiretoRequest request) {
        if (usuarioRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalStateException("Já existe uma conta com este e-mail.");
        }

        Lead lead = leadRepository.save(
                Lead.criarJaFechado(request.nome(), request.email(), request.telefone(), request.tipoContrato()));

        String senhaTemporaria = gerarSenhaTemporaria();
        Usuario usuario = usuarioRepository.save(
                new Usuario(request.email(), passwordEncoder.encode(senhaTemporaria), Perfil.MENTORADO));
        Mentorado mentorado = new Mentorado(usuario, request.nome(), request.negocio(), Plano.GRATUITO,
                BigDecimal.ZERO, 0, 0);
        mentorado.atualizarPerfil(request.telefone(), null, null);
        mentorado.atualizarDadosContrato(null, null, null, request.tipoContrato(), request.valorContrato(),
                request.dataFechamentoContrato());
        mentorado = mentoradoRepository.save(mentorado);

        lead.vincularMentorado(mentorado);
        leadRepository.save(lead);

        return new MentoradoCriado(mentorado, senhaTemporaria);
    }

    /** M23 item 4 (bulk-CREATE import, 19/07/2026) — mesma lógica de {@link #criarDireto}, mas
     * com o conjunto completo de dados que a migração real de ~40 empresas do Notion carrega
     * (nomeFantasia/cnpj/sócios + Diagnóstico Inicial, que {@link CriarMentoradoDiretoRequest} do
     * formulário único do Admin não pede). Chamado uma vez por linha já validada do CSV — quem
     * garante "tudo-ou-nada" entre várias linhas é o {@code @Transactional} de quem chama em
     * loop (mesmo padrão de {@code TeamCsvService}), não este método isoladamente. */
    @Transactional
    public MentoradoCriado criarDiretoDeImportacao(ImportarMentoradoDiretoLinha linha) {
        if (usuarioRepository.findByEmail(linha.email()).isPresent()) {
            throw new IllegalStateException("Já existe uma conta com este e-mail.");
        }

        Lead lead = leadRepository.save(
                Lead.criarJaFechado(linha.nome(), linha.email(), linha.telefone(), linha.tipoContrato()));

        String senhaTemporaria = gerarSenhaTemporaria();
        Usuario usuario = usuarioRepository.save(
                new Usuario(linha.email(), passwordEncoder.encode(senhaTemporaria), Perfil.MENTORADO));
        Mentorado mentorado = new Mentorado(usuario, linha.nome(), linha.negocio(), Plano.GRATUITO,
                BigDecimal.ZERO, 0, 0);
        mentorado.atualizarPerfil(linha.telefone(), null, null);
        mentorado.atualizarDadosContrato(linha.nomeFantasia(), linha.cnpj(), linha.socios(), linha.tipoContrato(),
                linha.valorContrato(), linha.dataFechamentoContrato());
        mentorado = mentoradoRepository.save(mentorado);

        lead.vincularMentorado(mentorado);
        leadRepository.save(lead);

        if (linha.temDadosDeDiagnostico()) {
            MentoradoDiagnosticoInicial diagnostico = new MentoradoDiagnosticoInicial(mentorado);
            diagnostico.atualizar(linha.faturamentoAnual(), linha.quantidadeColaboradores(),
                    linha.empresaRegularizada(), linha.quantidadeLojas(), linha.cmvDefinido(), linha.cmvDetalhe(),
                    linha.tempoMedioAtendimento(), linha.culturaConstruida(), linha.processosDesenhados());
            diagnosticoInicialRepository.save(diagnostico);
        }

        return new MentoradoCriado(mentorado, senhaTemporaria);
    }

    /** M23 — edição administrativa dos dados de contrato (H11.1 estendida). */
    @Transactional
    public Mentorado atualizarDadosContrato(UUID id, AtualizarDadosContratoRequest request) {
        Mentorado mentorado = buscar(id);
        mentorado.atualizarDadosContrato(request.nomeFantasia(), request.cnpj(), request.socios(),
                request.tipoContrato(), request.valorContrato(), request.dataFechamentoContrato());
        return mentoradoRepository.save(mentorado);
    }

    /** M23 — upload do PDF do contrato assinado; substitui o anterior se já existia (sem apagar
     * o arquivo antigo do disco nesta leva — pendência conhecida, mesma classe de decisão adiada
     * já registrada pro áudio de ata do M06). */
    @Transactional
    public Mentorado salvarDocumentoContrato(UUID id, MultipartFile arquivo) {
        Mentorado mentorado = buscar(id);
        String url = contratoDocumentoStorageService.salvar(id, arquivo);
        mentorado.atualizarDocumentoContrato(url);
        return mentoradoRepository.save(mentorado);
    }

    /** M23 — caminho em disco do PDF do contrato, pra streaming no download. */
    public java.nio.file.Path resolverDocumentoContrato(UUID id) {
        Mentorado mentorado = buscar(id);
        if (mentorado.getDocumentoContratoUrl() == null) {
            throw new IllegalStateException("Este mentorado ainda não tem documento de contrato.");
        }
        return contratoDocumentoStorageService.resolver(mentorado.getDocumentoContratoUrl());
    }

    /** M23 — null se a Leia ainda não preencheu o Diagnóstico Inicial desse mentorado. */
    public MentoradoDiagnosticoInicial buscarDiagnosticoInicial(UUID id) {
        return diagnosticoInicialRepository.findByMentoradoId(id).orElse(null);
    }

    /** M23 — Diagnóstico Inicial (feito pela Leia antes da 1ª reunião com o Mateus). Upsert: cria
     * na primeira vez, atualiza nas seguintes — é preenchido incrementalmente, não tudo de uma
     * vez (mesmo padrão observado no Notion real da operação). */
    @Transactional
    public MentoradoDiagnosticoInicial atualizarDiagnosticoInicial(UUID id, AtualizarDiagnosticoInicialRequest request) {
        Mentorado mentorado = buscar(id);
        MentoradoDiagnosticoInicial diagnostico = diagnosticoInicialRepository.findByMentoradoId(id)
                .orElseGet(() -> new MentoradoDiagnosticoInicial(mentorado));
        diagnostico.atualizar(request.faturamentoAnual(), request.quantidadeColaboradores(),
                request.empresaRegularizada(), request.quantidadeLojas(), request.cmvDefinido(),
                request.cmvDetalhe(), request.tempoMedioAtendimento(), request.culturaConstruida(),
                request.processosDesenhados());
        return diagnosticoInicialRepository.save(diagnostico);
    }

    private Mentorado buscar(UUID id) {
        return mentoradoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mentorado não encontrado."));
    }

    private static String gerarSenhaTemporaria() {
        byte[] bytes = new byte[18];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record MentoradoCriado(Mentorado mentorado, String senhaTemporaria) {
    }
}
