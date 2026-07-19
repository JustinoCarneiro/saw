package com.sawhub.hub.comercial;

import com.sawhub.hub.comercial.dto.ConciliacaoVendaResponse;
import com.sawhub.hub.financeiro.ContaPagarReceber;
import com.sawhub.hub.financeiro.StatusConta;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;

/** Change request 17/07/2026 ("conciliação") — quanto do valor contratado numa venda já foi
 * efetivamente liquidado, cruzando {@link Lead#getValorTotalVenda()} com o pago no ato e as
 * parcelas estruturadas ({@link ParcelaVenda}). Vive em {@code comercial} (mesmo pacote de
 * {@link Lead}/{@link ParcelaVenda}, evita depender de {@code financeiro} → {@code comercial} e
 * criar ciclo de pacote) mas o endpoint é gated {@code Modulo.FINANCEIRO} — mesmo padrão já usado
 * em {@code MentoradoContratoController} (M23): pacote de origem do dado ≠ quem deveria ver o dado. */
@Service
public class ConciliacaoService {

    private final LeadRepository leadRepository;
    private final ParcelaVendaRepository parcelaVendaRepository;

    public ConciliacaoService(LeadRepository leadRepository, ParcelaVendaRepository parcelaVendaRepository) {
        this.leadRepository = leadRepository;
        this.parcelaVendaRepository = parcelaVendaRepository;
    }

    public List<ConciliacaoVendaResponse> listar() {
        return leadRepository.buscarComVendaFechada().stream().map(this::conciliar).toList();
    }

    private ConciliacaoVendaResponse conciliar(Lead lead) {
        BigDecimal recebido = zeroSeNulo(lead.getValorPagoNoAto()).add(zeroSeNulo(lead.getTaxaPlataformaRetida()));
        for (ParcelaVenda parcela : parcelaVendaRepository.buscarPorLeadIdComConta(lead.getId())) {
            ContaPagarReceber conta = parcela.getContaPagarReceber();
            if (conta == null) {
                continue;
            }
            if (conta.getStatus() == StatusConta.PAGO || conta.getStatus() == StatusConta.RECEBIDO) {
                recebido = recebido.add(parcela.getValor());
            } else if (conta.getStatus() == StatusConta.PARCIAL) {
                recebido = recebido.add(zeroSeNulo(conta.getValorPago()));
            }
        }

        BigDecimal total = lead.getValorTotalVenda();
        BigDecimal pendente = total.subtract(recebido);
        double percentual = total.signum() == 0 ? 0.0
                : recebido.divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue();

        return new ConciliacaoVendaResponse(lead.getId(), lead.getNome(), total, recebido, pendente, percentual);
    }

    private static BigDecimal zeroSeNulo(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
