package com.sawhub.hub.comercial;

import com.sawhub.hub.comercial.dto.AvancarLeadRequest;
import com.sawhub.hub.comercial.dto.CriarLeadRequest;
import com.sawhub.hub.comercial.dto.CriarMetaComercialRequest;
import com.sawhub.hub.comercial.dto.DashboardComercialResponse;
import com.sawhub.hub.comercial.dto.EventoVendaResumo;
import com.sawhub.hub.comercial.dto.FecharVendaRequest;
import com.sawhub.hub.comercial.dto.LeadResponse;
import com.sawhub.hub.comercial.dto.MetaComercialResponse;
import com.sawhub.hub.comercial.dto.RankingItem;
import com.sawhub.hub.comercial.dto.VendedorResumo;
import com.sawhub.hub.common.dto.ImportResultResponse;
import com.sawhub.hub.evento.EventoRepository;
import com.sawhub.hub.evento.StatusEvento;
import com.sawhub.hub.security.AppUserPrincipal;
import com.sawhub.hub.security.RequiresModulo;
import com.sawhub.hub.team.Area;
import com.sawhub.hub.team.Colaborador;
import com.sawhub.hub.team.ColaboradorRepository;
import com.sawhub.hub.team.Modulo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** H13.1–H13.3 — pipeline, dashboard e ranking comercial. */
@RestController
@RequestMapping("/api/v1/admin/comercial")
@RequiresModulo(Modulo.COMERCIAL)
@Validated
public class ComercialController {

    private final LeadService leadService;
    private final ComercialDashboardService dashboardService;
    private final RankingComercialService rankingService;
    private final MetaComercialService metaComercialService;
    private final ColaboradorRepository colaboradorRepository;
    private final LeadCsvService leadCsvService;
    private final EventoRepository eventoRepository;
    private final VendaIngressoCsvService vendaIngressoCsvService;

    public ComercialController(LeadService leadService, ComercialDashboardService dashboardService,
                                RankingComercialService rankingService, MetaComercialService metaComercialService,
                                ColaboradorRepository colaboradorRepository,
                                LeadCsvService leadCsvService, EventoRepository eventoRepository,
                                VendaIngressoCsvService vendaIngressoCsvService) {
        this.leadService = leadService;
        this.dashboardService = dashboardService;
        this.rankingService = rankingService;
        this.metaComercialService = metaComercialService;
        this.colaboradorRepository = colaboradorRepository;
        this.leadCsvService = leadCsvService;
        this.eventoRepository = eventoRepository;
        this.vendaIngressoCsvService = vendaIngressoCsvService;
    }

    /** Só leitura, pro seletor de vendedor na tela de funil (mover lead pra Em contato) —
     * TeamController é gated por Modulo.TIME (só Fundador), então uma área Comercial não-Fundador
     * não conseguiria listar colaboradores por ali. Escopo mínimo: nome/id de quem é da própria
     * área Comercial. */
    @GetMapping("/vendedores")
    public List<VendedorResumo> vendedores() {
        return colaboradorRepository.findAllByAreaIn(List.of(Area.COMERCIAL, Area.ADMIN)).stream()
                .sorted(Comparator.comparing(Colaborador::getNome))
                .map(VendedorResumo::from)
                .toList();
    }

    @GetMapping("/leads")
    public List<LeadResponse> listarLeads(@RequestParam(required = false) StatusLead status,
                                           @RequestParam(required = false) UUID vendedorId) {
        return leadService.listar(status, vendedorId).stream().map(LeadResponse::from).toList();
    }

    // Fase 5 (H13.4) — cadastro manual pro time comercial, além do formulário público (H1.3) e do
    // import CSV (M22): mesmo LeadService.criar() de sempre, só um segundo chamador autenticado.
    @PostMapping("/leads")
    @ResponseStatus(HttpStatus.CREATED)
    public LeadResponse criarLead(@Valid @RequestBody CriarLeadRequest request) {
        return LeadResponse.from(leadService.criar(request));
    }

    @PatchMapping("/leads/{id}/avancar")
    public LeadResponse avancar(@PathVariable UUID id, @Valid @RequestBody AvancarLeadRequest request) {
        return LeadResponse.from(leadService.avancar(id, request));
    }

    // M25 — "formulário único de venda": fecha o Lead (deve estar em PROPOSTA, ver
    // Lead.fecharVenda) já distribuindo parcelamento pro financeiro e ingresso de evento pro
    // credenciamento. Endpoint dedicado, não substitui PATCH .../avancar com FECHADO.
    @PostMapping("/leads/{id}/fechar-venda")
    public LeadResponse fecharVenda(@PathVariable UUID id, @Valid @RequestBody FecharVendaRequest request) {
        return LeadResponse.from(leadService.fecharVenda(id, request));
    }

    // M25 — só leitura, pro seletor de evento no formulário único de venda (produtoVenda =
    // INGRESSO_EVENTO). Mesmo raciocínio de vendedores(): EventoController é gated por
    // Modulo.CONTEUDOS, escopo mínimo aqui é só eventos ainda abertos pra venda.
    @GetMapping("/eventos")
    public List<EventoVendaResumo> eventosParaVenda() {
        return eventoRepository.buscarPorStatusIn(List.of(StatusEvento.PROGRAMADO, StatusEvento.AO_VIVO), null)
                .stream().map(EventoVendaResumo::from).toList();
    }

    // M22 — mesmos filtros de GET /leads.
    @GetMapping("/leads/export")
    public ResponseEntity<byte[]> exportarLeads(@RequestParam(required = false) StatusLead status,
                                                 @RequestParam(required = false) UUID vendedorId) {
        String csv = leadCsvService.exportar(status, vendedorId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"leads.csv\"")
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    // M22 — toda linha vira um Lead novo em SOLICITACAO (mesmo estágio do formulário público);
    // 200 se tudo foi validado e persistido, 422 se nada foi (ver erros no corpo).
    @PostMapping("/leads/import")
    public ResponseEntity<ImportResultResponse> importarLeads(@RequestParam("arquivo") MultipartFile arquivo) {
        ImportResultResponse resultado = leadCsvService.importar(arquivo);
        HttpStatus status = resultado.erros().isEmpty() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status).body(resultado);
    }

    // Change request pós-MVP ("importação de planilhas de eventos passados") — oposto de
    // eventosParaVenda() (só PROGRAMADO/AO_VIVO): aqui é o seletor de evento pro import de
    // histórico, que só faz sentido pra evento que JÁ aconteceu.
    @GetMapping("/eventos/historico")
    public List<EventoVendaResumo> eventosHistorico() {
        return eventoRepository.buscarPorStatusIn(List.of(StatusEvento.REALIZADO), null)
                .stream().map(EventoVendaResumo::from).toList();
    }

    // Change request pós-MVP ("importação de planilhas de eventos passados pra popular
    // histórico") — uma aba por evento na planilha real, por isso eventoId é path param, não
    // coluna do CSV (ver VendaIngressoCsvService). 200 se tudo foi validado e persistido, 422 se
    // nada foi (ver erros no corpo) — mesmo contrato dos demais imports (M21/M22).
    @PostMapping("/eventos/{eventoId}/ingressos/import")
    public ResponseEntity<ImportResultResponse> importarVendasIngresso(
            @PathVariable UUID eventoId, @RequestParam("arquivo") MultipartFile arquivo) {
        ImportResultResponse resultado = vendaIngressoCsvService.importar(eventoId, arquivo);
        HttpStatus status = resultado.erros().isEmpty() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status).body(resultado);
    }

    @GetMapping("/dashboard")
    public DashboardComercialResponse dashboard(@RequestParam @Min(2020) int ano, @RequestParam @Min(1) @Max(12) int mes) {
        return dashboardService.dashboard(ano, mes);
    }

    @GetMapping("/ranking")
    public List<RankingItem> ranking(@RequestParam @Min(2020) int ano, @RequestParam @Min(1) @Max(12) int mes) {
        return rankingService.ranking(ano, mes);
    }

    // Pedido do Marcos (22/07/2026) — até esta leva não existia jeito de definir meta pela UI
    // (só o seed de demonstração); o Ranking só listava vendedor com meta já plantada no banco.
    // GET pra edição (mostrar meta atual por vendedor); PUT é upsert por (vendedor, ano, mês).
    @GetMapping("/metas")
    public List<MetaComercialResponse> metas(@RequestParam @Min(2020) int ano, @RequestParam @Min(1) @Max(12) int mes) {
        return metaComercialService.listar(ano, mes);
    }

    // Pedido do Marcos (22/07/2026) — meta e % de comissão só podem ser definidos pelo Admin, não
    // pelo próprio vendedor: área Comercial também acessa este módulo (Modulo.COMERCIAL), então o
    // gate de módulo sozinho não bastava aqui, precisa de checagem de Area explícita.
    @PutMapping("/metas")
    public MetaComercialResponse definirMeta(@AuthenticationPrincipal AppUserPrincipal principal,
                                              @Valid @RequestBody CriarMetaComercialRequest request) {
        if (principal.getArea() != Area.ADMIN) {
            throw new AccessDeniedException("Só o Admin pode definir meta e comissão.");
        }
        return MetaComercialResponse.from(metaComercialService.definir(request));
    }

    @GetMapping("/ranking/vendedores/{id}/vendas")
    public List<LeadResponse> detalharVendasDoRanking(@PathVariable("id") UUID vendedorId, @RequestParam int ano, @RequestParam int mes) {
        return rankingService.detalharVendas(vendedorId, ano, mes);
    }
}
