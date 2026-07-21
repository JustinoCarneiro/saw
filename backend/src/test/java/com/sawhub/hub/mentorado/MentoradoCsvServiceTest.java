package com.sawhub.hub.mentorado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sawhub.hub.security.Perfil;
import com.sawhub.hub.security.Usuario;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** M22 — export CSV de {@link Mentorado}. O import (bulk-UPDATE por e-mail) que este service
 * também tinha até o M28 foi removido — ver {@link MentoradoDiretoCsvServiceTest}, que agora cobre
 * criar-ou-atualizar num fluxo só. */
@ExtendWith(MockitoExtension.class)
class MentoradoCsvServiceTest {

    @Mock
    private MentoradoRepository mentoradoRepository;

    private MentoradoCsvService service() {
        return new MentoradoCsvService(mentoradoRepository);
    }

    private static Usuario usuario(String email) {
        return new Usuario(email, "hash", Perfil.MENTORADO);
    }

    @Test
    void exportarProduzCsvComPontoEVirgulaEDataPtBr() {
        Usuario usuario = usuario("joao@saborearte.com.br");
        Mentorado m = new Mentorado(usuario, "João Silva", "Sabor & Arte", BigDecimal.ZERO, 0, 0);
        m.atualizarDadosContrato(null, null, null, TipoContrato.MENTORIA_CONTINUA, null, LocalDate.of(2026, 1, 15));
        when(mentoradoRepository.buscarComFiltro(null, null)).thenReturn(List.of(m));

        String csv = service().exportar(null, null);

        assertThat(csv).contains("email;nome;negocio;tipoContrato;status");
        assertThat(csv).contains("joao@saborearte.com.br;João Silva;Sabor & Arte;MENTORIA_CONTINUA;ATIVO");
    }

    @Test
    void exportarNeutralizaNomeQueComecaComSinalDeFormula() {
        Usuario usuario = usuario("x@x.com");
        Mentorado m = new Mentorado(usuario, "=SOMA(A1:A2)", null, BigDecimal.ZERO, 0, 0);
        when(mentoradoRepository.buscarComFiltro(any(), any())).thenReturn(List.of(m));

        String csv = service().exportar(null, null);

        assertThat(csv).contains("'=SOMA(A1:A2)");
    }
}
