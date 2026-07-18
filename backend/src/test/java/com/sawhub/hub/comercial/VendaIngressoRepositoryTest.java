package com.sawhub.hub.comercial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sawhub.hub.evento.Evento;
import com.sawhub.hub.evento.EventoRepository;
import com.sawhub.hub.evento.TipoEvento;
import com.sawhub.hub.security.Perfil;
import com.sawhub.hub.security.Usuario;
import com.sawhub.hub.security.UsuarioRepository;
import com.sawhub.hub.team.Area;
import com.sawhub.hub.team.Colaborador;
import com.sawhub.hub.team.ColaboradorRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/** M25 — achado do revisor-seguranca: ComercialDashboardService.resumoVendaIngresso() lê
 * venda.getLead().getValorTotalVenda() fora de transação (open-in-view=false) — sem
 * LEFT JOIN FETCH, `lead` é um proxy LAZY não inicializado e estoura LazyInitializationException
 * em produção assim que existir venda de ingresso no período consultado. @DataJpaTest de
 * propósito (sessão real do Hibernate) — mesmo raciocínio de LeadRepositoryTest, um mock nunca
 * reproduziria isso. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class VendaIngressoRepositoryTest {

    @Autowired
    private VendaIngressoRepository vendaIngressoRepository;
    @Autowired
    private LeadRepository leadRepository;
    @Autowired
    private EventoRepository eventoRepository;
    @Autowired
    private ColaboradorRepository colaboradorRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private EntityManager entityManager;

    private Lead leadFechadoComIngresso(String sufixo) {
        Usuario usuario = usuarioRepository.save(new Usuario("vendedor" + sufixo + "@sawhub.com.br", "hash", Perfil.ADMIN));
        Colaborador vendedor = colaboradorRepository.save(new Colaborador(usuario, "Paula" + sufixo, Area.COMERCIAL));
        Lead lead = new Lead("Comprador " + sufixo, "comprador" + sufixo + "@example.com", null, null, null);
        lead.moverParaEmContato(vendedor);
        lead.moverParaProposta();
        lead.fecharVenda(ProdutoVenda.INGRESSO_EVENTO, OrigemVenda.DIRETA, new BigDecimal("300.00"),
                new BigDecimal("300.00"), FormaPagamento.PIX);
        return leadRepository.save(lead);
    }

    @Test
    void findByEventoIdPuroMantemLeadComoProxyLazyNaoInicializado() {
        // RED — documenta o bug: findByEventoId() (derivado, sem JOIN FETCH) é a causa raiz.
        Evento evento = eventoRepository.save(new Evento("Evento E2E", TipoEvento.PRESENCIAL, null,
                Instant.now(), "Recife", null, 100));
        Lead lead = leadFechadoComIngresso("1");
        vendaIngressoRepository.save(new VendaIngresso(lead, evento, CategoriaIngresso.VIP, "Convidado", null, false));
        entityManager.flush();
        entityManager.clear();

        VendaIngresso recarregada = vendaIngressoRepository.findByLeadId(lead.getId()).get(0);
        entityManager.clear();

        assertThatThrownBy(() -> recarregada.getLead().getValorTotalVenda())
                .isInstanceOf(LazyInitializationException.class);
    }

    @Test
    void buscarPorEventoIdComLeadInicializaLeadMesmoForaDaTransacaoOriginal() {
        Evento evento = eventoRepository.save(new Evento("Evento E2E", TipoEvento.PRESENCIAL, null,
                Instant.now(), "Recife", null, 100));
        Lead lead = leadFechadoComIngresso("2");
        vendaIngressoRepository.save(new VendaIngresso(lead, evento, CategoriaIngresso.VIP, "Convidado", null, false));
        entityManager.flush();
        entityManager.clear();

        var vendas = vendaIngressoRepository.buscarPorEventoIdComLead(evento.getId());
        entityManager.clear();

        assertThat(vendas).hasSize(1);
        assertThat(vendas.get(0).getLead().getValorTotalVenda()).isEqualByComparingTo("300.00");
    }
}
