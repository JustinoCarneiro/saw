package com.sawhub.hub.atividade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AtividadeLogServiceTest {

    @Mock
    private AtividadeLogRepository atividadeLogRepository;

    private AtividadeLogService service() {
        return new AtividadeLogService(atividadeLogRepository);
    }

    @Test
    void registrarPersisteTipoEDescricao() {
        ArgumentCaptor<AtividadeLog> captor = ArgumentCaptor.forClass(AtividadeLog.class);

        service().registrar("MENTORIA_CANCELADA", "Mentoria cancelada: João Silva");

        verify(atividadeLogRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo("MENTORIA_CANCELADA");
        assertThat(captor.getValue().getDescricao()).isEqualTo("Mentoria cancelada: João Silva");
    }

    @Test
    void listarRecentesDelegaParaRepositorioOrdenadoPorMaisRecente() {
        AtividadeLog log = new AtividadeLog("LEAD_FECHADO", "Lead fechado: Ana Costa");
        when(atividadeLogRepository.findAllByOrderByCriadoEmDesc()).thenReturn(List.of(log));

        assertThat(service().listarRecentes()).containsExactly(log);
    }
}
