package com.sawhub.hub.financeiro;

import com.sawhub.hub.financeiro.dto.CriarContaBancariaRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContaBancariaService {

    private final ContaBancariaRepository contaBancariaRepository;

    public ContaBancariaService(ContaBancariaRepository contaBancariaRepository) {
        this.contaBancariaRepository = contaBancariaRepository;
    }

    @Transactional
    public ContaBancaria criar(CriarContaBancariaRequest request) {
        try {
            return contaBancariaRepository.saveAndFlush(new ContaBancaria(request.nome().trim()));
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Já existe uma conta bancária com o nome \"" + request.nome() + "\".");
        }
    }
}
