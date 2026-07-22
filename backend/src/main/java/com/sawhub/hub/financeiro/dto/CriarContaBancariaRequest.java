package com.sawhub.hub.financeiro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarContaBancariaRequest(@NotBlank @Size(max = 80) String nome) {
}
