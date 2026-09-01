package com.chavescr.nexa.service;

import java.util.Collections;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.dto.InstitucionDTO;
import com.chavescr.nexa.dto.UsuarioDTO;
import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** El usuario autenticado (entidad completa), para páginas de cuenta personal como "Mi Perfil". */
    @Transactional(readOnly = true)
    public Usuario obtenerUsuarioActual() {
        String identifier = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByIdentifierWithInstituciones(identifier)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado"));
    }

    /** Actualiza los datos personales editables desde "Mi Perfil" (nombre y teléfono). */
    public void actualizarDatosPersonales(Long usuarioId, String nombre, String telefono) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        usuario.setNombre(nombre.trim());
        usuario.setTelefono(telefono != null && !telefono.isBlank() ? telefono.trim() : null);
        usuarioRepository.save(usuario);
    }

    /** Cambio de contraseña propio: exige conocer la contraseña actual. */
    public void cambiarPassword(Long usuarioId, String passwordActual, String passwordNueva) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta");
        }
        if (passwordNueva == null || passwordNueva.length() < 8) {
            throw new IllegalArgumentException("La nueva contraseña debe tener al menos 8 caracteres");
        }
        usuario.setPassword(passwordEncoder.encode(passwordNueva));
        usuarioRepository.save(usuario);
    }

    @Transactional(readOnly = true)
    public List<Usuario> obtenerTodos() {
        return usuarioRepository.findAll();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "usuarios", key = "'todos'")
    public List<UsuarioDTO> obtenerTodosDTO() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Usuario> buscarPorNombre(String filtro) {
        if (filtro == null || filtro.trim().isEmpty()) {
            return usuarioRepository.findAll();
        }
        return usuarioRepository.findByNombreOrEmail(filtro.trim().toLowerCase());
    }

    @Transactional(readOnly = true)
    public List<InstitucionDTO> obtenerInstitucionesDelUsuarioActual() {
        String identifier = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByIdentifierWithInstituciones(identifier)
                .map(usuario -> usuario.getInstituciones().stream()
                        .filter(Institucion::getActiva)
                        .map(InstitucionDTO::new)
                        .toList())
                .orElse(Collections.emptyList());
    }

    /** La última institución con la que trabajó el usuario autenticado, o null si nunca eligió una. */
    @Transactional(readOnly = true)
    public Long obtenerUltimaInstitucionIdDelUsuarioActual() {
        String identifier = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByIdentifier(identifier)
                .map(Usuario::getUltimaInstitucion)
                .map(Institucion::getId)
                .orElse(null);
    }

    /** Recuerda la institución elegida para que se auto-seleccione en el próximo login. */
    public void actualizarUltimaInstitucion(Long usuarioId, Institucion institucion) {
        usuarioRepository.findById(usuarioId).ifPresent(usuario -> {
            usuario.setUltimaInstitucion(institucion);
            usuarioRepository.save(usuario);
        });
    }

    @Transactional(readOnly = true)
    public List<UsuarioDTO> obtenerEstudiantesDelUsuarioActual() {
        String identifier = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByIdentifier(identifier)
                .map(padre -> usuarioRepository.findEstudiantesByPadreId(padre.getId()).stream()
                        .map(UsuarioDTO::new)
                        .toList())
                .orElse(Collections.emptyList());
    }

    @Transactional(readOnly = true)
    public Usuario findByUsername(String username) {
        return usuarioRepository.findByEmailWithInstituciones(username).orElse(null);
    }

    @CacheEvict(value = "usuarios", allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void evictAllCaches() {
    }
}
