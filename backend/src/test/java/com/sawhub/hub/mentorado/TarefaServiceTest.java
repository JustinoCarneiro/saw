package com.sawhub.hub.mentorado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sawhub.hub.meta.Meta;
import com.sawhub.hub.meta.MetaRepository;
import com.sawhub.hub.mentorado.dto.AtualizarTarefaRequest;
import com.sawhub.hub.mentorado.dto.CriarTarefaRequest;
import com.sawhub.hub.mentorado.dto.ResumoTarefasResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** H4.1–H4.4 — RED primeiro: TarefaService ainda não existe neste ponto do ciclo. */
@ExtendWith(MockitoExtension.class)
class TarefaServiceTest {

    @Mock
    private EncaminhamentoRepository encaminhamentoRepository;
    @Mock
    private MentoradoRepository mentoradoRepository;
    @Mock
    private MetaRepository metaRepository;

    private TarefaService service() {
        return new TarefaService(encaminhamentoRepository, mentoradoRepository, metaRepository);
    }

    private static Mentorado mentorado(UUID id) {
        Mentorado m = new Mentorado(null, "Maria", null, BigDecimal.ZERO, 0, 0);
        ReflectionTestUtils.setField(m, "id", id);
        return m;
    }

    private static Meta metaDe(Mentorado mentorado, UUID id) {
        Meta meta = new Meta(mentorado, "Reduzir CMV", null, LocalDate.now().plusDays(60));
        ReflectionTestUtils.setField(meta, "id", id);
        return meta;
    }

    private static Encaminhamento tarefaDe(Mentorado mentorado, UUID id) {
        Encaminhamento e = new Encaminhamento(mentorado, "Revisar indicadores", LocalDate.now().plusDays(10), Prioridade.ALTA, null);
        ReflectionTestUtils.setField(e, "id", id);
        return e;
    }

    @Test
    void criarSemMetaSalvaComPesoFixo1EStatusPendente() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(encaminhamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Encaminhamento criada = service().criar(usuarioId,
                new CriarTarefaRequest("Revisar indicadores", LocalDate.now().plusDays(10), Prioridade.ALTA, null));

        assertThat(criada.getMentorado()).isSameAs(mentorado);
        assertThat(criada.getPeso()).isEqualTo(1);
        assertThat(criada.getStatus()).isEqualTo(StatusTarefa.PENDENTE);
        assertThat(criada.getMeta()).isNull();
    }

    @Test
    void criarComMetaDoProprioMentoradoVinculaOk() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        Meta minhaMeta = metaDe(mentorado, UUID.randomUUID());
        when(metaRepository.findById(minhaMeta.getId())).thenReturn(Optional.of(minhaMeta));
        when(encaminhamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Encaminhamento criada = service().criar(usuarioId,
                new CriarTarefaRequest("Revisar indicadores", LocalDate.now().plusDays(10), Prioridade.ALTA, minhaMeta.getId()));

        assertThat(criada.getMeta()).isSameAs(minhaMeta);
    }

    @Test
    void criarComMetaDeOutroMentoradoRejeitadaComoSeNaoExistisse() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado eu = mentorado(UUID.randomUUID());
        Mentorado outro = mentorado(UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(eu));
        Meta metaDeOutro = metaDe(outro, UUID.randomUUID());
        when(metaRepository.findById(metaDeOutro.getId())).thenReturn(Optional.of(metaDeOutro));

        assertThatThrownBy(() -> service().criar(usuarioId,
                new CriarTarefaRequest("x", LocalDate.now().plusDays(10), Prioridade.ALTA, metaDeOutro.getId())))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void listarDelegaParaRepositorioComMentoradoStatusEBusca() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(encaminhamentoRepository.buscarPorMentorado(mentorado.getId(), StatusTarefa.PENDENTE, "indicadores"))
                .thenReturn(List.of());

        service().listar(usuarioId, StatusTarefa.PENDENTE, "indicadores");

        verify(encaminhamentoRepository).buscarPorMentorado(mentorado.getId(), StatusTarefa.PENDENTE, "indicadores");
    }

    @Test
    void atualizarEditaCamposDaPropriaTarefa() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado eu = mentorado(UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(eu));
        Encaminhamento minhaTarefa = tarefaDe(eu, UUID.randomUUID());
        when(encaminhamentoRepository.buscarPorIdComMeta(minhaTarefa.getId())).thenReturn(Optional.of(minhaTarefa));
        when(encaminhamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Encaminhamento atualizada = service().atualizar(usuarioId, minhaTarefa.getId(),
                new AtualizarTarefaRequest("Novo título", LocalDate.now().plusDays(20), Prioridade.BAIXA, null));

        assertThat(atualizada.getTitulo()).isEqualTo("Novo título");
        assertThat(atualizada.getPrioridade()).isEqualTo(Prioridade.BAIXA);
        // peso não muda por edição — só o construtor self-service ou o fluxo Admin/ata definem peso.
        assertThat(atualizada.getPeso()).isEqualTo(1);
    }

    @Test
    void atualizarTarefaDeOutroMentoradoRejeitadaComoSeNaoExistisse() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado eu = mentorado(UUID.randomUUID());
        Mentorado outro = mentorado(UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(eu));
        Encaminhamento tarefaDeOutro = tarefaDe(outro, UUID.randomUUID());
        when(encaminhamentoRepository.buscarPorIdComMeta(tarefaDeOutro.getId())).thenReturn(Optional.of(tarefaDeOutro));

        assertThatThrownBy(() -> service().atualizar(usuarioId, tarefaDeOutro.getId(),
                new AtualizarTarefaRequest("x", LocalDate.now().plusDays(10), Prioridade.ALTA, null)))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void atualizarComMetaDeOutroMentoradoRejeitadaMesmoSendoDonoDaTarefa() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado eu = mentorado(UUID.randomUUID());
        Mentorado outro = mentorado(UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(eu));
        Encaminhamento minhaTarefa = tarefaDe(eu, UUID.randomUUID());
        when(encaminhamentoRepository.buscarPorIdComMeta(minhaTarefa.getId())).thenReturn(Optional.of(minhaTarefa));
        Meta metaDeOutro = metaDe(outro, UUID.randomUUID());
        when(metaRepository.findById(metaDeOutro.getId())).thenReturn(Optional.of(metaDeOutro));

        assertThatThrownBy(() -> service().atualizar(usuarioId, minhaTarefa.getId(),
                new AtualizarTarefaRequest("x", LocalDate.now().plusDays(10), Prioridade.ALTA, metaDeOutro.getId())))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void avancarStatusParaEmAndamentoChamaIniciarNaEntidade() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado eu = mentorado(UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(eu));
        Encaminhamento minhaTarefa = tarefaDe(eu, UUID.randomUUID());
        when(encaminhamentoRepository.buscarPorIdComMeta(minhaTarefa.getId())).thenReturn(Optional.of(minhaTarefa));
        when(encaminhamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Encaminhamento resultado = service().avancarStatus(usuarioId, minhaTarefa.getId(), StatusTarefa.EM_ANDAMENTO);

        assertThat(resultado.getStatus()).isEqualTo(StatusTarefa.EM_ANDAMENTO);
    }

    @Test
    void avancarStatusParaConcluidaChamaConcluirNaEntidade() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado eu = mentorado(UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(eu));
        Encaminhamento minhaTarefa = tarefaDe(eu, UUID.randomUUID());
        when(encaminhamentoRepository.buscarPorIdComMeta(minhaTarefa.getId())).thenReturn(Optional.of(minhaTarefa));
        when(encaminhamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Encaminhamento resultado = service().avancarStatus(usuarioId, minhaTarefa.getId(), StatusTarefa.CONCLUIDA);

        assertThat(resultado.getStatus()).isEqualTo(StatusTarefa.CONCLUIDA);
    }

    @Test
    void avancarStatusDeTarefaDeOutroMentoradoRejeitada() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado eu = mentorado(UUID.randomUUID());
        Mentorado outro = mentorado(UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(eu));
        Encaminhamento tarefaDeOutro = tarefaDe(outro, UUID.randomUUID());
        when(encaminhamentoRepository.buscarPorIdComMeta(tarefaDeOutro.getId())).thenReturn(Optional.of(tarefaDeOutro));

        assertThatThrownBy(() -> service().avancarStatus(usuarioId, tarefaDeOutro.getId(), StatusTarefa.CONCLUIDA))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void resumoCalculaContagensSobreTodasIndependenteDeFiltro() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado eu = mentorado(UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(eu));

        Encaminhamento pendente = tarefaDe(eu, UUID.randomUUID());
        Encaminhamento emAndamento = tarefaDe(eu, UUID.randomUUID());
        emAndamento.iniciar();
        Encaminhamento concluida = tarefaDe(eu, UUID.randomUUID());
        concluida.concluir();

        when(encaminhamentoRepository.buscarPorMentorado(eu.getId(), null, null))
                .thenReturn(List.of(pendente, emAndamento, concluida));

        ResumoTarefasResponse resumo = service().resumo(usuarioId);

        assertThat(resumo.total()).isEqualTo(3);
        assertThat(resumo.pendentes()).isEqualTo(1);
        assertThat(resumo.emAndamento()).isEqualTo(1);
        assertThat(resumo.concluidas()).isEqualTo(1);
    }
}
