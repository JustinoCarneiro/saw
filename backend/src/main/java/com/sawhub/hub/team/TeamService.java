package com.sawhub.hub.team;

import com.sawhub.hub.mentoria.MentoriaRepository;
import com.sawhub.hub.security.Perfil;
import com.sawhub.hub.security.Usuario;
import com.sawhub.hub.security.UsuarioRepository;
import com.sawhub.hub.team.dto.ColaboradorResponse;
import com.sawhub.hub.team.dto.CriarColaboradorRequest;
import java.util.Comparator;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeamService {

    private final UsuarioRepository usuarioRepository;
    private final ColaboradorRepository colaboradorRepository;
    private final MentoriaRepository mentoriaRepository;
    private final PasswordEncoder passwordEncoder;

    public TeamService(UsuarioRepository usuarioRepository, ColaboradorRepository colaboradorRepository,
                        MentoriaRepository mentoriaRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.colaboradorRepository = colaboradorRepository;
        this.mentoriaRepository = mentoriaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // H15.6 (M20) — carteira computada por leitura (contagem de Mentorado distintos entre as
    // Mentorias em que o colaborador é mentor), nunca armazenada.
    public List<ColaboradorResponse> listar() {
        return colaboradorRepository.findAll().stream()
                .sorted(Comparator.comparing(Colaborador::getNome))
                .map(c -> ColaboradorResponse.from(c, carteiraDe(c)))
                .toList();
    }

    long carteiraDe(Colaborador colaborador) {
        return mentoriaRepository.buscarPorMentor(colaborador).stream()
                .flatMap(m -> m.getMentorados().stream())
                .map(mentorado -> mentorado.getId())
                .distinct()
                .count();
    }

    @Transactional
    public Colaborador criar(CriarColaboradorRequest request) {
        if (usuarioRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Já existe uma conta com este e-mail.");
        }
        Usuario usuario = usuarioRepository.save(
                new Usuario(request.email(), passwordEncoder.encode(request.senha()), Perfil.ADMIN));
        return colaboradorRepository.save(new Colaborador(usuario, request.nome(), request.area()));
    }
}
