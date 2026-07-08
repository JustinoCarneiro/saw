package com.sawhub.hub.security;

import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.team.AreaModuloMatrix;
import com.sawhub.hub.team.Colaborador;
import com.sawhub.hub.team.ColaboradorRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final ColaboradorRepository colaboradorRepository;
    private final MentoradoRepository mentoradoRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository,
                                     ColaboradorRepository colaboradorRepository,
                                     MentoradoRepository mentoradoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.colaboradorRepository = colaboradorRepository;
        this.mentoradoRepository = mentoradoRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));

        List<String> authorities = new ArrayList<>();
        String nome;
        com.sawhub.hub.team.Area area = null;

        if (usuario.getPerfil() == Perfil.ADMIN) {
            Colaborador colaborador = colaboradorRepository.findByUsuario(usuario)
                    .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));
            nome = colaborador.getNome();
            area = colaborador.getArea();
            authorities.add("ROLE_ADMIN");
            AreaModuloMatrix.allowedModulos(area).forEach(m -> authorities.add("MODULO_" + m.name()));
        } else {
            Mentorado mentorado = mentoradoRepository.findByUsuario(usuario)
                    .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));
            nome = mentorado.getNome();
            authorities.add("ROLE_MENTORADO");
        }

        return new AppUserPrincipal(usuario.getId(), usuario.getEmail(), usuario.getPasswordHash(),
                nome, usuario.getPerfil(), area, authorities);
    }
}
