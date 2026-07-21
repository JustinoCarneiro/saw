package com.sawhub.hub.mentorado.dto;

import com.sawhub.hub.common.dto.ImportErro;
import java.util.List;

/** M23 item 4 (bulk-CREATE, 19/07/2026) — variante de
 * {@link com.sawhub.hub.common.dto.ImportResultResponse} pra import que CRIA credencial nova por
 * linha: precisa devolver a senha temporária de cada mentorado criado (não fica recuperável
 * depois — só o hash é persistido, mesma razão de {@link MentoradoCriadoResponse}). Tudo-ou-nada
 * como todo import do projeto: se {@code erros} não está vazio, {@code criados} é sempre vazio (e
 * {@code atualizados} sempre zero).
 *
 * <p>M28 — {@code atualizados} conta linhas resolvidas por e-mail já existente (import único,
 * ver {@link ImportarMentoradoDiretoLinha}); essas não aparecem em {@code criados} porque não
 * geram credencial nova. */
public record ImportMentoradoDiretoResultResponse(
        int totalLinhas, int importados, int atualizados, List<ImportErro> erros, List<MentoradoCriadoResponse> criados
) {
}
