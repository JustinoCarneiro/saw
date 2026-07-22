package com.sawhub.hub.mentorado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sawhub.hub.meta.Meta;
import com.sawhub.hub.mentorado.dto.AtualizarEncaminhamentoAdminRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** Fase 5 (H4.6) — achado ao vivo (22/07/2026): tela Admin de Tarefas era só leitura + CSV, sem
 * forma de editar/avançar status de um encaminhamento existente (inclusive os materializados na
 * publicação de ata, M06). Sem escopo por usuário/posse, diferente de TarefaService — Admin edita
 * qualquer encaminhamento de qualquer mentorado. */
@ExtendWith(MockitoExtension.class)
class EncaminhamentoAdminServiceTest {

    @Mock
    private EncaminhamentoRepository encaminhamentoRepository;

    private EncaminhamentoAdminService service() {
        return new EncaminhamentoAdminService(encaminhamentoRepository);
    }

    private static Mentorado mentorado(UUID id) {
        Mentorado m = new Mentorado(null, "Maria", null, BigDecimal.ZERO, 0, 0);
        ReflectionTestUtils.setField(m, "id", id);
        return m;
    }

    private static Encaminhamento tarefaSemPrazoDe(Mentorado mentorado, UUID id) {
        // Peso 2, sem prazo, com mentoria de origem — perfil de um encaminhamento materializado
        // na publicação de uma ata (M06), o caso real que motivou este gap.
        Encaminhamento e = new Encaminhamento(mentorado, "Ajustar ficha técnica", 2, false, null);
        ReflectionTestUtils.setField(e, "id", id);
        return e;
    }

    @Test
    void atualizarEditaTituloPrazoEPrioridadeSemMexerNoPeso() {
        Mentorado mentorado = mentorado(UUID.randomUUID());
        Encaminhamento tarefa = tarefaSemPrazoDe(mentorado, UUID.randomUUID());
        when(encaminhamentoRepository.buscarPorIdComMentorado(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(encaminhamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Encaminhamento atualizada = service().atualizar(tarefa.getId(),
                new AtualizarEncaminhamentoAdminRequest("Ajustar ficha técnica (revisão)", LocalDate.now().plusDays(7), Prioridade.ALTA));

        assertThat(atualizada.getTitulo()).isEqualTo("Ajustar ficha técnica (revisão)");
        assertThat(atualizada.getPrazo()).isEqualTo(LocalDate.now().plusDays(7));
        assertThat(atualizada.getPrioridade()).isEqualTo(Prioridade.ALTA);
        // peso 2 (vindo da ata) preservado — edição Admin nunca muda peso.
        assertThat(atualizada.getPeso()).isEqualTo(2);
    }

    @Test
    void atualizarAceitaPrazoNuloSemForcarUmaData() {
        Mentorado mentorado = mentorado(UUID.randomUUID());
        Encaminhamento tarefa = tarefaSemPrazoDe(mentorado, UUID.randomUUID());
        when(encaminhamentoRepository.buscarPorIdComMentorado(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(encaminhamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Encaminhamento atualizada = service().atualizar(tarefa.getId(),
                new AtualizarEncaminhamentoAdminRequest("Só o título mudou", null, Prioridade.MEDIA));

        assertThat(atualizada.getPrazo()).isNull();
    }

    @Test
    void atualizarPreservaMetaJaVinculada() {
        Mentorado mentorado = mentorado(UUID.randomUUID());
        Meta meta = new Meta(mentorado, "Reduzir CMV", null, LocalDate.now().plusDays(60));
        Encaminhamento tarefa = new Encaminhamento(mentorado, "Tarefa com meta", LocalDate.now().plusDays(5), Prioridade.MEDIA, meta);
        ReflectionTestUtils.setField(tarefa, "id", UUID.randomUUID());
        when(encaminhamentoRepository.buscarPorIdComMentorado(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(encaminhamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Encaminhamento atualizada = service().atualizar(tarefa.getId(),
                new AtualizarEncaminhamentoAdminRequest("Novo título", LocalDate.now().plusDays(5), Prioridade.MEDIA));

        assertThat(atualizada.getMeta()).isSameAs(meta);
    }

    @Test
    void atualizarTarefaInexistenteLancaNoSuchElement() {
        UUID id = UUID.randomUUID();
        when(encaminhamentoRepository.buscarPorIdComMentorado(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().atualizar(id,
                new AtualizarEncaminhamentoAdminRequest("x", null, Prioridade.MEDIA)))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void avancarStatusParaEmAndamentoChamaIniciarNaEntidade() {
        Mentorado mentorado = mentorado(UUID.randomUUID());
        Encaminhamento tarefa = tarefaSemPrazoDe(mentorado, UUID.randomUUID());
        when(encaminhamentoRepository.buscarPorIdComMentorado(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(encaminhamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Encaminhamento resultado = service().avancarStatus(tarefa.getId(), StatusTarefa.EM_ANDAMENTO);

        assertThat(resultado.getStatus()).isEqualTo(StatusTarefa.EM_ANDAMENTO);
    }

    @Test
    void avancarStatusParaConcluidaChamaConcluirNaEntidade() {
        Mentorado mentorado = mentorado(UUID.randomUUID());
        Encaminhamento tarefa = tarefaSemPrazoDe(mentorado, UUID.randomUUID());
        when(encaminhamentoRepository.buscarPorIdComMentorado(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(encaminhamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Encaminhamento resultado = service().avancarStatus(tarefa.getId(), StatusTarefa.CONCLUIDA);

        assertThat(resultado.getStatus()).isEqualTo(StatusTarefa.CONCLUIDA);
    }

    @Test
    void avancarStatusParaPendenteChamaReabrirNaEntidade() {
        Mentorado mentorado = mentorado(UUID.randomUUID());
        Encaminhamento tarefa = tarefaSemPrazoDe(mentorado, UUID.randomUUID());
        tarefa.concluir();
        when(encaminhamentoRepository.buscarPorIdComMentorado(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(encaminhamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Encaminhamento resultado = service().avancarStatus(tarefa.getId(), StatusTarefa.PENDENTE);

        assertThat(resultado.getStatus()).isEqualTo(StatusTarefa.PENDENTE);
    }

    @Test
    void avancarStatusDeTarefaInexistenteLancaNoSuchElement() {
        UUID id = UUID.randomUUID();
        when(encaminhamentoRepository.buscarPorIdComMentorado(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().avancarStatus(id, StatusTarefa.CONCLUIDA))
                .isInstanceOf(NoSuchElementException.class);
    }
}
