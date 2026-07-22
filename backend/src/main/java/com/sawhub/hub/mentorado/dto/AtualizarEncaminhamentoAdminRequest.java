package com.sawhub.hub.mentorado.dto;

import com.sawhub.hub.mentorado.Prioridade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

// Diferente de AtualizarTarefaRequest (self-service do mentorado, M10): prazo é opcional aqui —
// encaminhamentos gerados por ata (M06) nascem sem prazo de propósito (Encaminhamento), editar
// pelo Admin não deve forçar a inventar uma data. Sem metaId: vínculo a Meta é decisão do
// mentorado (self-service); editar aqui preserva a meta já vinculada, se houver.
public record AtualizarEncaminhamentoAdminRequest(
        @NotBlank @Size(max = 255) String titulo,
        LocalDate prazo,
        Prioridade prioridade
) {
}
