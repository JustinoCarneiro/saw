package com.sawhub.hub.team;

import com.sawhub.hub.security.Perfil;
import com.sawhub.hub.security.Usuario;
import com.sawhub.hub.security.UsuarioRepository;
import com.sawhub.hub.team.dto.CriarColaboradorRequest;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeamService {

    private final UsuarioRepository usuarioRepository;
    private final ColaboradorRepository colaboradorRepository;
    private final PasswordEncoder passwordEncoder;

    public TeamService(UsuarioRepository usuarioRepository, ColaboradorRepository colaboradorRepository,
                        PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.colaboradorRepository = colaboradorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Colaborador> listar() {
        return colaboradorRepository.findAllByOrderByNomeAsc();
    }

    @Transactional
    public Colaborador criar(CriarColaboradorRequest request) {
        if (usuarioRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Já existe uma conta com este e-mail.");
        }
        Usuario usuario = usuarioRepository.save(
                new Usuario(request.email(), passwordEncoder.encode(request.senha()), Perfil.ADMIN));
        return colaboradorRepository.save(new Colaborador(usuario, request.nome(), request.area(), null, null));
    }
}
