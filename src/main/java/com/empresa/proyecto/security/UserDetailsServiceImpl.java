package com.empresa.proyecto.security;

import com.empresa.proyecto.entity.Usuario;
import com.empresa.proyecto.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByIdentifier(identifier)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "No existe usuario con email, usuario o cédula: " + identifier));
        return new CustomUserDetails(usuario);
    }
}
