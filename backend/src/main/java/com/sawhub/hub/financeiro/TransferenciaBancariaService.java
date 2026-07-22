package com.sawhub.hub.financeiro;

import com.sawhub.hub.financeiro.dto.CriarTransferenciaRequest;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferenciaBancariaService {

    private final ContaBancariaRepository contaBancariaRepository;
    private final TransferenciaBancariaRepository transferenciaRepository;

    public TransferenciaBancariaService(ContaBancariaRepository contaBancariaRepository,
                                         TransferenciaBancariaRepository transferenciaRepository) {
        this.contaBancariaRepository = contaBancariaRepository;
        this.transferenciaRepository = transferenciaRepository;
    }

    @Transactional
    public TransferenciaBancaria registrar(CriarTransferenciaRequest request) {
        if (request.contaOrigemId().equals(request.contaDestinoId())) {
            throw new IllegalArgumentException("Conta de origem e destino não podem ser a mesma.");
        }
        ContaBancaria origem = contaBancariaRepository.findById(request.contaOrigemId())
                .orElseThrow(() -> new IllegalArgumentException("Conta de origem não encontrada."));
        ContaBancaria destino = contaBancariaRepository.findById(request.contaDestinoId())
                .orElseThrow(() -> new IllegalArgumentException("Conta de destino não encontrada."));

        return transferenciaRepository.save(new TransferenciaBancaria(origem, destino, request.valor(),
                request.data(), request.descricao()));
    }

    public List<TransferenciaBancaria> listar(LocalDate de, LocalDate ate) {
        return transferenciaRepository.buscarPorPeriodo(de, ate);
    }
}
