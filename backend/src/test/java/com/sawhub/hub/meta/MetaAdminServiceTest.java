package com.sawhub.hub.meta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.meta.dto.AtualizarMetaRequest;
import com.sawhub.hub.meta.dto.CriarMetaAdminRequest;
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

/** Fase 5 (H3.4) — achado ao vivo (22/07/2026): tela Admin de Metas era só leitura + CSV, sem
 * forma de criar/editar/avançar status de uma meta existente. Sem escopo por usuário/posse,
 * diferente de MetaService (self-service do mentorado) — Admin opera sobre qualquer mentorado. */
@ExtendWith(MockitoExtension.class)
class MetaAdminServiceTest {

    @Mock
    private MetaRepository metaRepository;
    @Mock
    private MentoradoRepository mentoradoRepository;

    private MetaAdminService service() {
        return new MetaAdminService(metaRepository, mentoradoRepository);
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
    void criarVinculaAoMentoradoInformadoNoRequest() {
        Mentorado mentorado = mentorado(UUID.randomUUID());
        when(mentoradoRepository.findById(mentorado.getId())).thenReturn(Optional.of(mentorado));
        when(metaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Meta criada = service().criar(new CriarMetaAdminRequest(
                mentorado.getId(), "Reduzir CMV", "desc", LocalDate.now().plusDays(30)));

        assertThat(criada.getMentorado()).isSameAs(mentorado);
        assertThat(criada.getStatus()).isEqualTo(StatusMeta.ATIVA);
        assertThat(criada.getProgressoPct()).isZero();
    }

    @Test
    void criarComMentoradoInexistenteLancaNoSuchElement() {
        UUID mentoradoId = UUID.randomUUID();
        when(mentoradoRepository.findById(mentoradoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().criar(new CriarMetaAdminRequest(
                mentoradoId, "x", null, LocalDate.now().plusDays(10))))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void atualizarEditaTituloDescricaoPrazoEProgresso() {
        Mentorado mentorado = mentorado(UUID.randomUUID());
        Meta meta = metaDe(mentorado, UUID.randomUUID());
        when(metaRepository.buscarPorIdComMentorado(meta.getId())).thenReturn(Optional.of(meta));
        when(metaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Meta atualizada = service().atualizar(meta.getId(), new AtualizarMetaRequest(
                "Reduzir CMV pra 28%", "nova desc", LocalDate.now().plusDays(45), 60));

        assertThat(atualizada.getTitulo()).isEqualTo("Reduzir CMV pra 28%");
        assertThat(atualizada.getDescricao()).isEqualTo("nova desc");
        assertThat(atualizada.getProgressoPct()).isEqualTo(60);
    }

    @Test
    void atualizarMetaInexistenteLancaNoSuchElement() {
        UUID id = UUID.randomUUID();
        when(metaRepository.buscarPorIdComMentorado(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().atualizar(id,
                new AtualizarMetaRequest("x", null, LocalDate.now().plusDays(10), 0)))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void avancarStatusParaConcluidaChamaConcluirNaEntidade() {
        Mentorado mentorado = mentorado(UUID.randomUUID());
        Meta meta = metaDe(mentorado, UUID.randomUUID());
        when(metaRepository.buscarPorIdComMentorado(meta.getId())).thenReturn(Optional.of(meta));
        when(metaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Meta resultado = service().avancarStatus(meta.getId(), StatusMeta.CONCLUIDA);

        assertThat(resultado.getStatus()).isEqualTo(StatusMeta.CONCLUIDA);
        assertThat(resultado.getProgressoPct()).isEqualTo(100);
    }

    @Test
    void avancarStatusParaPausadaChamaPausarNaEntidade() {
        Mentorado mentorado = mentorado(UUID.randomUUID());
        Meta meta = metaDe(mentorado, UUID.randomUUID());
        when(metaRepository.buscarPorIdComMentorado(meta.getId())).thenReturn(Optional.of(meta));
        when(metaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Meta resultado = service().avancarStatus(meta.getId(), StatusMeta.PAUSADA);

        assertThat(resultado.getStatus()).isEqualTo(StatusMeta.PAUSADA);
    }

    @Test
    void avancarStatusParaAtivaChamaReativarNaEntidade() {
        Mentorado mentorado = mentorado(UUID.randomUUID());
        Meta meta = metaDe(mentorado, UUID.randomUUID());
        meta.pausar();
        when(metaRepository.buscarPorIdComMentorado(meta.getId())).thenReturn(Optional.of(meta));
        when(metaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Meta resultado = service().avancarStatus(meta.getId(), StatusMeta.ATIVA);

        assertThat(resultado.getStatus()).isEqualTo(StatusMeta.ATIVA);
    }

    @Test
    void avancarStatusDeMetaInexistenteLancaNoSuchElement() {
        UUID id = UUID.randomUUID();
        when(metaRepository.buscarPorIdComMentorado(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().avancarStatus(id, StatusMeta.CONCLUIDA))
                .isInstanceOf(NoSuchElementException.class);
    }
}
