package com.sawhub.hub.financeiro;

import com.sawhub.hub.financeiro.dto.CaixaMensalResponse;
import com.sawhub.hub.financeiro.dto.ContaBancariaResponse;
import com.sawhub.hub.financeiro.dto.CriarContaBancariaRequest;
import com.sawhub.hub.financeiro.dto.CriarTransferenciaRequest;
import com.sawhub.hub.financeiro.dto.PosicaoCaixaMensalResponse;
import com.sawhub.hub.financeiro.dto.RegistrarPosicaoCaixaRequest;
import com.sawhub.hub.financeiro.dto.TransferenciaBancariaResponse;
import com.sawhub.hub.security.RequiresModulo;
import com.sawhub.hub.team.Modulo;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Change request pós-MVP (E14, "Caixa do mês" + "Transferências Entre Contas", reunião
 * 17/07/2026) — contas bancárias, posição mensal de caixa por conta e transferências internas.
 * Nenhuma dessas três coisas entra no DRE (ver Javadoc de {@link TransferenciaBancaria}). */
@RestController
@RequestMapping("/api/v1/admin/financeiro")
@RequiresModulo(Modulo.FINANCEIRO)
@Validated
public class CaixaController {

    private final ContaBancariaRepository contaBancariaRepository;
    private final ContaBancariaService contaBancariaService;
    private final CaixaMensalService caixaMensalService;
    private final TransferenciaBancariaService transferenciaBancariaService;

    public CaixaController(ContaBancariaRepository contaBancariaRepository, ContaBancariaService contaBancariaService,
                            CaixaMensalService caixaMensalService,
                            TransferenciaBancariaService transferenciaBancariaService) {
        this.contaBancariaRepository = contaBancariaRepository;
        this.contaBancariaService = contaBancariaService;
        this.caixaMensalService = caixaMensalService;
        this.transferenciaBancariaService = transferenciaBancariaService;
    }

    @GetMapping("/contas-bancarias")
    public List<ContaBancariaResponse> listarContas() {
        return contaBancariaRepository.findAll().stream().map(ContaBancariaResponse::from).toList();
    }

    @PostMapping("/contas-bancarias")
    @ResponseStatus(HttpStatus.CREATED)
    public ContaBancariaResponse criarConta(@Valid @RequestBody CriarContaBancariaRequest request) {
        return ContaBancariaResponse.from(contaBancariaService.criar(request));
    }

    @GetMapping("/caixa")
    public CaixaMensalResponse caixaDoMes(@RequestParam int ano, @RequestParam int mes) {
        return caixaMensalService.caixaDoMes(ano, mes);
    }

    // Upsert por (conta, ano, mês) — registrar de novo o mesmo período corrige o valor, não
    // duplica linha (ver CaixaMensalService.registrarPosicao). PUT porque é substituição integral
    // do estado daquele período, não um PATCH parcial.
    @PutMapping("/caixa")
    public PosicaoCaixaMensalResponse registrarPosicaoCaixa(@Valid @RequestBody RegistrarPosicaoCaixaRequest request) {
        return PosicaoCaixaMensalResponse.from(caixaMensalService.registrarPosicao(request));
    }

    @GetMapping("/transferencias")
    public List<TransferenciaBancariaResponse> listarTransferencias(@RequestParam LocalDate de, @RequestParam LocalDate ate) {
        return transferenciaBancariaService.listar(de, ate).stream().map(TransferenciaBancariaResponse::from).toList();
    }

    @PostMapping("/transferencias")
    @ResponseStatus(HttpStatus.CREATED)
    public TransferenciaBancariaResponse registrarTransferencia(@Valid @RequestBody CriarTransferenciaRequest request) {
        return TransferenciaBancariaResponse.from(transferenciaBancariaService.registrar(request));
    }
}
