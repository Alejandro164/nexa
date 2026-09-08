package com.chavescr.nexa.service;

import java.text.Normalizer;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.dto.InstitucionDTO;
import com.chavescr.nexa.dto.UsuarioDTO;
import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.entity.Rol;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.InstitucionRepository;
import com.chavescr.nexa.repository.RolRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final InstitucionRepository institucionRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
            InstitucionRepository institucionRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.institucionRepository = institucionRepository;
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

    /** Cantidad de usuarios activos de un rol específico (ej. ROLE_ESTUDIANTE) en una institución, para el dashboard. */
    @Transactional(readOnly = true)
    public long contarActivosPorInstitucionYRol(Long institucionId, String rolNombre) {
        return usuarioRepository.findActivosByInstitucionIdAndRol(institucionId, rolNombre).size();
    }

    /** Personal (admin/director/docente) activo de una institución, para selectores como el de Control de Acceso. */
    @Transactional(readOnly = true)
    public List<Usuario> obtenerPersonalActivoPorInstitucion(Long institucionId) {
        return usuarioRepository.findActivosByInstitucionIdAndRolIn(institucionId,
                List.of("ROLE_ADMIN", "ROLE_DIRECTOR", "ROLE_DOCENTE"));
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

    // ── CRUD global de Usuarios (pantalla /usuarios, admin sin institución seleccionada) ──────────

    @Transactional(readOnly = true)
    public List<Usuario> listarTodosConInstituciones(String filtro) {
        List<Usuario> todos = usuarioRepository.findAllWithInstituciones();
        if (filtro == null || filtro.isBlank()) {
            return todos;
        }
        String f = normalizar(filtro.trim());
        return todos.stream()
                .filter(u -> normalizar(u.getNombre()).contains(f)
                        || normalizar(u.getEmail()).contains(f)
                        || (u.getUsuario() != null && normalizar(u.getUsuario()).contains(f))
                        || (u.getCedula() != null && normalizar(u.getCedula()).contains(f)))
                .toList();
    }

    private String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase();
    }

    @Transactional(readOnly = true)
    public Usuario obtenerPorId(Long id) {
        return usuarioRepository.findByIdWithInstituciones(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    public Usuario guardar(Long id, String nombre, String email, String usuarioLogin, String cedula,
            String rawPassword, boolean activo, List<Long> rolIds, List<Long> institucionIds) {
        Usuario u;
        if (id == null) {
            if (rawPassword == null || rawPassword.isBlank()) {
                throw new IllegalArgumentException("La contraseña es obligatoria al crear un usuario");
            }
            u = new Usuario();
        } else {
            u = obtenerPorId(id);
        }

        u.setNombre(nombre.trim());
        u.setEmail(email.trim().toLowerCase());
        u.setUsuario(usuarioLogin.trim().toLowerCase());
        u.setCedula(cedula != null && !cedula.isBlank() ? cedula.trim() : null);
        u.setActivo(activo);

        if (rawPassword != null && !rawPassword.isBlank()) {
            u.setPassword(passwordEncoder.encode(rawPassword));
        }

        Set<Rol> roles = rolIds == null ? Set.of() :
                rolIds.stream()
                        .map(rid -> rolRepository.findById(rid)
                                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + rid)))
                        .collect(Collectors.toSet());
        u.setRoles(roles);

        Set<Institucion> instituciones = institucionIds == null ? Set.of() :
                institucionIds.stream()
                        .map(iid -> institucionRepository.findById(iid)
                                .orElseThrow(() -> new IllegalArgumentException("Institución no encontrada: " + iid)))
                        .collect(Collectors.toSet());
        u.setInstituciones(instituciones);

        Usuario guardado = usuarioRepository.save(u);
        evictAllCaches();
        return guardado;
    }

    public void eliminar(Long id) {
        String identifier = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario actual = usuarioRepository.findByIdentifier(identifier).orElse(null);
        if (actual != null && actual.getId().equals(id)) {
            throw new IllegalArgumentException("No puedes eliminar tu propia cuenta");
        }
        Usuario u = obtenerPorId(id);
        usuarioRepository.delete(u);
        evictAllCaches();
    }

    public void toggleActivo(Long id) {
        Usuario u = obtenerPorId(id);
        u.setActivo(!u.getActivo());
        usuarioRepository.save(u);
        evictAllCaches();
    }
}
