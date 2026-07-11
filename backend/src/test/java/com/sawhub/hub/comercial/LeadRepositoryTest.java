package com.sawhub.hub.comercial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sawhub.hub.mentorado.Plano;
import com.sawhub.hub.security.Perfil;
import com.sawhub.hub.security.Usuario;
import com.sawhub.hub.security.UsuarioRepository;
import com.sawhub.hub.team.Area;
import com.sawhub.hub.team.Colaborador;
import com.sawhub.hub.team.ColaboradorRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/** Bug achado ao vivo durante a verificação de M05 via curl: {@code LeadService.avancar()} usava
 * {@code findById()} puro, que devolve {@code vendedor} como proxy LAZY. Qualquer transição além
 * da própria atribuição de vendedor (ex.: EM_CONTATO -&gt; PROPOSTA) explodia com
 * LazyInitializationException ao montar o {@code LeadResponse} fora da transação
 * (open-in-view=false). Usa @DataJpaTest (sessão real do Hibernate) de propósito — um teste
 * baseado em mock nunca reproduziria isso, um mock nunca é um proxy LAZY. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class LeadRepositoryTest {

    @Autowired
    private LeadRepository leadRepository;
    @Autowired
    private ColaboradorRepository colaboradorRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private EntityManager entityManager;

    private Colaborador criarVendedor(String sufixo) {
        Usuario usuario = usuarioRepository.save(
                new Usuario("vendedor" + sufixo + "@sawhub.com.br", "hash", Perfil.ADMIN));
        return colaboradorRepository.save(new Colaborador(usuario, "Paula", Area.COMERCIAL));
    }

    @Test
    void findByIdPuroMantemVendedorComoProxyLazyNaoInicializado() {
        // RED — documenta o bug: findById() puro é a causa raiz, não usar mais em avancar().
        Colaborador vendedor = criarVendedor("1");
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null, Plano.ESSENCIAL);
        lead.moverParaEmContato(vendedor);
        Lead salvo = leadRepository.save(lead);
        entityManager.flush();
        entityManager.clear(); // simula o fim da transação anterior — próxima leitura é um objeto novo

        Lead recarregado = leadRepository.findById(salvo.getId()).orElseThrow();
        entityManager.clear(); // vendedor.nome só existiria se já tivesse sido tocado dentro da sessão

        assertThatThrownBy(() -> recarregado.getVendedor().getNome())
                .isInstanceOf(LazyInitializationException.class);
    }

    @Test
    void buscarPorIdComVendedorInicializaVendedorMesmoForaDaTransacaoOriginal() {
        Colaborador vendedor = criarVendedor("2");
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null, Plano.ESSENCIAL);
        lead.moverParaEmContato(vendedor);
        Lead salvo = leadRepository.save(lead);
        entityManager.flush();
        entityManager.clear();

        Lead recarregado = leadRepository.buscarPorIdComVendedor(salvo.getId()).orElseThrow();
        entityManager.clear();

        assertThat(recarregado.getVendedor().getNome()).isEqualTo("Paula");
    }
}
