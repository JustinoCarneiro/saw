package com.sawhub.hub.meta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sawhub.hub.meta.dto.AtualizarMetaRequest;
import com.sawhub.hub.meta.dto.CriarMetaRequest;
import com.sawhub.hub.meta.dto.ResumoMetasResponse;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
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

/** H3.1–H3.3 — RED primeiro: MetaService ainda não existe neste ponto do ciclo. */
@ExtendWith(MockitoExtension.class)
class MetaServiceTest {

    @Mock
    private MetaRepository metaRepository;
    @Mock
    private MentoradoRepository mentoradoRepository;

    private MetaService service() {
        return new MetaService(metaRepository, mentoradoRepository);
    }

    private static Mentorado mentorado(UUID id) {
        Mentorado m = new Mentorado(null, "Maria", null, BigDecimal.ZERO, 0, 0);
        ReflectionTestUtils.setField(m, "id", id);
        return m;
    }

    private static Meta metaDe(Mentorado mentorado, UUID id) {
        Meta meta = new Meta(mentorado, "Reduzir CMV", "desc", LocalDate.now().plusDays(30));
        ReflectionTestUtils.setField(meta, "id", id);
        return meta;
    }

    @Test
    void criarResolveMentoradoDoUsuarioAutenticadoESalvaAtiva() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(metaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Meta criada = service().criar(usuarioId, new CriarMetaRequest("Reduzir CMV", "desc", LocalDate.now().plusDays(30)));

        assertThat(criada.getMentorado()).isSameAs(mentorado);
        assertThat(criada.getTitulo()).isEqualTo("Reduzir CMV");
        assertThat(criada.getStatus()).isEqualTo(StatusMeta.ATIVA);
        assertThat(criada.getProgressoPct()).isZero();
    }

    @Test
    void listarDelegaParaRepositorioComMentoradoDoUsuarioAutenticado() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(metaRepository.buscarPorMentorado(mentorado.getId(), StatusMeta.ATIVA)).thenReturn(List.of());

        service().listar(usuarioId, StatusMeta.ATIVA);

        verify(metaRepository).buscarPorMentorado(mentorado.getId(), StatusMeta.ATIVA);
    }

    @Test
    void atualizarEditaCamposDaPropriaMeta() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado eu = mentorado(UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(eu));
        Meta minhaMeta = metaDe(eu, UUID.randomUUID());
        when(metaRepository.findById(minhaMeta.getId())).thenReturn(Optional.of(minhaMeta));
        when(metaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Meta atualizada = service().atualizar(usuarioId, minhaMeta.getId(),
                new AtualizarMetaRequest("Novo título", "Nova desc", LocalDate.now().plusDays(60), 70));

        assertThat(atualizada.getTitulo()).isEqualTo("Novo título");
        assertThat(atualizada.getProgressoPct()).isEqualTo(70);
    }

    @Test
    void atualizarMetaDeOutroMentoradoRejeitadoComoSeNaoExistisse() {
        // Isolamento por tenant (CLAUDE.md): 404 genérico, não 403 — não confirma nem nega que a
        // meta existe pra outro mentorado (mesmo princípio já aplicado em M07/H1.1).
        UUID usuarioId = UUID.randomUUID();
        Mentorado eu = mentorado(UUID.randomUUID());
        Mentorado outro = mentorado(UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(eu));
        Meta metaDeOutro = metaDe(outro, UUID.randomUUID());
        when(metaRepository.findById(metaDeOutro.getId())).thenReturn(Optional.of(metaDeOutro));

        assertThatThrownBy(() -> service().atualizar(usuarioId, metaDeOutro.getId(),
                new AtualizarMetaRequest("x", null, LocalDate.now().plusDays(10), 50)))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void avancarStatusConcluidaChamaConcluirNaEntidadeECravaProgresso100() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado eu = mentorado(UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(eu));
        Meta minhaMeta = metaDe(eu, UUID.randomUUID());
        when(metaRepository.findById(minhaMeta.getId())).thenReturn(Optional.of(minhaMeta));
        when(metaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Meta resultado = service().avancarStatus(usuarioId, minhaMeta.getId(), StatusMeta.CONCLUIDA);

        assertThat(resultado.getStatus()).isEqualTo(StatusMeta.CONCLUIDA);
        assertThat(resultado.getProgressoPct()).isEqualTo(100);
    }

    @Test
    void avancarStatusDeMetaDeOutroMentoradoRejeitado() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado eu = mentorado(UUID.randomUUID());
        Mentorado outro = mentorado(UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(eu));
        Meta metaDeOutro = metaDe(outro, UUID.randomUUID());
        when(metaRepository.findById(metaDeOutro.getId())).thenReturn(Optional.of(metaDeOutro));

        assertThatThrownBy(() -> service().avancarStatus(usuarioId, metaDeOutro.getId(), StatusMeta.CONCLUIDA))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void resumoCalculaMediaEContagensSobreTodasAsMetasIndependenteDeFiltro() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado eu = mentorado(UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(eu));

        Meta concluida = metaDe(eu, UUID.randomUUID());
        concluida.concluir();
        Meta atrasada = new Meta(eu, "Atrasada", null, LocalDate.now().plusDays(30));
        ReflectionTestUtils.setField(atrasada, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(atrasada, "prazo", LocalDate.now().minusDays(5));
        Meta noPrazo = metaDe(eu, UUID.randomUUID());

        // resumo() sempre busca SEM filtro de status (null) — pega as 3, não só as ATIVAs.
        when(metaRepository.buscarPorMentorado(eu.getId(), null)).thenReturn(List.of(concluida, atrasada, noPrazo));

        ResumoMetasResponse resumo = service().resumo(usuarioId);

        assertThat(resumo.concluidas()).isEqualTo(1);
        assertThat(resumo.atrasadas()).isEqualTo(1);
        assertThat(resumo.noPrazo()).isEqualTo(1);
        // (100 + 0 + 0) / 3 = 33.33... -> 33
        assertThat(resumo.conclusaoMediaPct()).isEqualTo(33);
    }
}
