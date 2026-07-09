package com.sawhub.hub.mentorado;

import com.sawhub.hub.comercial.Lead;
import com.sawhub.hub.comercial.LeadRepository;
import com.sawhub.hub.comercial.StatusLead;
import com.sawhub.hub.mentorado.dto.AtualizarMentoradoRequest;
import com.sawhub.hub.security.Perfil;
import com.sawhub.hub.security.Usuario;
import com.sawhub.hub.security.UsuarioRepository;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** H11.1 — CRUD administrativo de mentorados; fecha a pendência deixada pelo M05 (Lead FECHADO
 * não cria conta de login sozinho). */
@Service
public class MentoradoAdminService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final MentoradoRepository mentoradoRepository;
    private final UsuarioRepository usuarioRepository;
    private final LeadRepository leadRepository;
    private final PasswordEncoder passwordEncoder;

    public MentoradoAdminService(MentoradoRepository mentoradoRepository, UsuarioRepository usuarioRepository,
                                  LeadRepository leadRepository, PasswordEncoder passwordEncoder) {
        this.mentoradoRepository = mentoradoRepository;
        this.usuarioRepository = usuarioRepository;
        this.leadRepository = leadRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Mentorado> listar(Plano plano, StatusMentorado status, String busca) {
        return mentoradoRepository.buscarComFiltro(plano, status, busca);
    }

    @Transactional
    public Mentorado atualizar(UUID id, AtualizarMentoradoRequest request) {
        Mentorado mentorado = buscar(id);
        mentorado.atualizar(request.nome(), request.negocio(), request.plano());
        return mentoradoRepository.save(mentorado);
    }

    @Transactional
    public Mentorado ativar(UUID id) {
        Mentorado mentorado = buscar(id);
        mentorado.ativar();
        return mentoradoRepository.save(mentorado);
    }

    @Transactional
    public Mentorado desativar(UUID id) {
        Mentorado mentorado = buscar(id);
        mentorado.desativar();
        return mentoradoRepository.save(mentorado);
    }

    /** Único endpoint que efetivamente cria a conta de login do mentorado — nasce sempre de um
     * lead FECHADO (H11.1 fechando a pendência do M05), nunca de um cadastro avulso nesta leva. */
    @Transactional
    public MentoradoCriado criarAPartirDeLead(UUID leadId) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new IllegalArgumentException("Lead não encontrado."));
        if (lead.getStatus() != StatusLead.FECHADO) {
            throw new IllegalStateException("Só é possível criar mentorado a partir de um lead Fechado.");
        }
        if (lead.getMentorado() != null) {
            throw new IllegalStateException("Este lead já tem um mentorado vinculado.");
        }
        if (usuarioRepository.findByEmail(lead.getEmail()).isPresent()) {
            throw new IllegalStateException("Já existe uma conta com este e-mail.");
        }

        String senhaTemporaria = gerarSenhaTemporaria();
        Usuario usuario = usuarioRepository.save(
                new Usuario(lead.getEmail(), passwordEncoder.encode(senhaTemporaria), Perfil.MENTORADO));
        Plano plano = lead.getPlanoFechado() != null ? lead.getPlanoFechado() : Plano.GRATUITO;
        Mentorado mentorado = mentoradoRepository.save(
                new Mentorado(usuario, lead.getNome(), null, plano, BigDecimal.ZERO, 0, 0));

        lead.vincularMentorado(mentorado);
        leadRepository.save(lead);

        return new MentoradoCriado(mentorado, senhaTemporaria);
    }

    private Mentorado buscar(UUID id) {
        return mentoradoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mentorado não encontrado."));
    }

    private static String gerarSenhaTemporaria() {
        byte[] bytes = new byte[18];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record MentoradoCriado(Mentorado mentorado, String senhaTemporaria) {
    }
}
