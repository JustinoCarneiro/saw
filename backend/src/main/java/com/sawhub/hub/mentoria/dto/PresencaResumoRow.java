package com.sawhub.hub.mentoria.dto;

import java.util.UUID;

/** E17/M27 — projeção escalar de propósito: `p.mentorado.id` numa query JPQL resolve pela coluna
 * FK direto, sem carregar o proxy LAZY de {@code Mentorado} (diferente de navegar
 * {@code presenca.getMentorado().getId()} em Java depois do fetch, que arriscaria
 * LazyInitializationException fora da transação, open-in-view=false). */
public record PresencaResumoRow(UUID mentoradoId, boolean presente) {
}
