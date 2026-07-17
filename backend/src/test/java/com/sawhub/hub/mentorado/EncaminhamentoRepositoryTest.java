package com.sawhub.hub.mentorado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sawhub.hub.meta.Meta;
import com.sawhub.hub.meta.MetaRepository;
import com.sawhub.hub.security.Perfil;
import com.sawhub.hub.security.Usuario;
import com.sawhub.hub.security.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/** Bug achado ao vivo durante a verificação de M10 via curl: {@code TarefaService.atualizar()}/
 * {@code avancarStatus()} usavam o RETORNO de {@code encaminhamentoRepository.save(tarefa)} pra
 * montar o {@code TarefaResponse}. {@code save()} numa entidade já persistida faz {@code merge()},
 * que devolve um objeto gerenciado NUM NOVO CONTEXTO DE PERSISTÊNCIA — a associação {@code meta}
 * volta a ser um proxy LAZY não inicializado ali, mesmo já tendo sido carregada via FETCH JOIN em
 * {@code buscarPorIdComMeta()} momentos antes. Usa @DataJpaTest (sessão real do Hibernate) de
 * propósito — um teste baseado em mock nunca reproduziria isso (ver LeadRepositoryTest, M05, para
 * o precedente desta classe de bug). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class EncaminhamentoRepositoryTest {

    @Autowired
    private EncaminhamentoRepository encaminhamentoRepository;
    @Autowired
    private MentoradoRepository mentoradoRepository;
    @Autowired
    private MetaRepository metaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private EntityManager entityManager;

    private Mentorado criarMentorado(String sufixo) {
        Usuario usuario = usuarioRepository.save(
                new Usuario("mentorado" + sufixo + "@sawhub.com.br", "hash", Perfil.MENTORADO));
        return mentoradoRepository.save(
                new Mentorado(usuario, "Mentorado " + sufixo, null, Plano.ESSENCIAL, BigDecimal.ZERO, 0, 0));
    }

    private Meta criarMeta(Mentorado mentorado) {
        return metaRepository.save(new Meta(mentorado, "Reduzir CMV", null, LocalDate.now().plusDays(60)));
    }

    @Test
    void saveDeTarefaJaPersistidaDevolveMetaComoProxyLazyNaoInicializado() {
        // RED — documenta o bug: ler meta.getTitulo() a partir do RETORNO de save() quebra, mesmo
        // com buscarPorIdComMeta fazendo FETCH JOIN antes.
        Mentorado mentorado = criarMentorado("1");
        Meta meta = criarMeta(mentorado);
        Encaminhamento tarefa = encaminhamentoRepository.save(
                new Encaminhamento(mentorado, "Renegociar fornecedor", LocalDate.now().plusDays(10), Prioridade.ALTA, meta));
        entityManager.flush();
        entityManager.clear();

        Encaminhamento carregada = encaminhamentoRepository.buscarPorIdComMeta(tarefa.getId()).orElseThrow();
        // clear() ANTES de mutar/salvar — simula o fim da transação de buscarDoMentorado() em
        // produção (cada chamada de repositório é sua própria transação); sem isto, save() faria
        // um merge() no-op numa entidade ainda gerenciada e o bug não reproduz.
        entityManager.clear();
        carregada.iniciar();
        Encaminhamento devolvidaPeloSave = encaminhamentoRepository.save(carregada);
        entityManager.clear();

        assertThatThrownBy(() -> devolvidaPeloSave.getMeta().getTitulo())
                .isInstanceOf(LazyInitializationException.class);
    }

    @Test
    void usarReferenciaPreSaveMantemMetaInicializadaMesmoForaDaTransacaoOriginal() {
        // GREEN — a correção real (TarefaService retorna a referência pré-save, não o valor de
        // save()): a mesma entidade `carregada`, já com meta inicializada via FETCH JOIN, continua
        // legível depois do save() e do fim da transação.
        Mentorado mentorado = criarMentorado("2");
        Meta meta = criarMeta(mentorado);
        Encaminhamento tarefa = encaminhamentoRepository.save(
                new Encaminhamento(mentorado, "Renegociar fornecedor", LocalDate.now().plusDays(10), Prioridade.ALTA, meta));
        entityManager.flush();
        entityManager.clear();

        Encaminhamento carregada = encaminhamentoRepository.buscarPorIdComMeta(tarefa.getId()).orElseThrow();
        entityManager.clear(); // mesma simulação de transação separada, ver teste acima
        carregada.iniciar();
        encaminhamentoRepository.save(carregada); // retorno intencionalmente ignorado
        entityManager.clear();

        assertThat(carregada.getMeta().getTitulo()).isEqualTo("Reduzir CMV");
    }

    @Test
    void listarTodasComMentoradoContinuaLegivelForaDaTransacaoOriginal() {
        // Fase 5 — achado ao vivo (curl direto): GET /admin/encaminhamentos/export usava
        // findAll() puro (sem fetch join) e devolvia entidades com `mentorado` LAZY não
        // inicializado; EncaminhamentoCsvService.exportar() não roda numa transação, então ler
        // e.getMentorado().getNome() sempre lançava LazyInitializationException (500 em
        // produção). listarTodasComMentorado() faz FETCH JOIN em mentorado (e mentorado.usuario,
        // usado pro e-mail no CSV) — precisa continuar legível mesmo depois do clear() abaixo,
        // simulando o fim da transação da chamada ao repositório (mesmo raciocínio dos dois
        // testes acima).
        Mentorado mentorado = criarMentorado("3");
        encaminhamentoRepository.save(new Encaminhamento(mentorado, "Renegociar fornecedor",
                LocalDate.now().plusDays(10), Prioridade.ALTA, null));
        entityManager.flush();
        entityManager.clear();

        var lista = encaminhamentoRepository.listarTodasComMentorado();
        entityManager.clear();

        assertThat(lista).isNotEmpty();
        Encaminhamento encontrada = lista.stream()
                .filter(e -> e.getMentorado().getId().equals(mentorado.getId()))
                .findFirst().orElseThrow();
        assertThat(encontrada.getMentorado().getNome()).isEqualTo("Mentorado 3");
        assertThat(encontrada.getMentorado().getUsuario().getEmail()).isEqualTo("mentorado3@sawhub.com.br");
    }
}
