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

import com.chavescr.nexa.dto.DireccionDTO;
import com.chavescr.nexa.dto.UsuarioDTO;
import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.Rol;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.DireccionRepository;
import com.chavescr.nexa.repository.RolRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final DireccionRepository direccionRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
            DireccionRepository direccionRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.direccionRepository = direccionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** El usuario autenticado (entidad completa), para páginas de cuenta personal como "Mi Perfil". */
    @Transactional(readOnly = true)
    public Usuario obtenerUsuarioActual() {
        String identifier = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByIdentifierWithDirecciones(identifier)
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

    /** Cantidad de usuarios activos de un rol específico (ej. ROLE_ESTUDIANTE) en una dirección, para el dashboard. */
    @Transactional(readOnly = true)
    public long contarActivosPorDireccionYRol(Long direccionId, String rolNombre) {
        return usuarioRepository.findActivosByDireccionIdAndRol(direccionId, rolNombre).size();
    }

    /** Personal (admin/director/docente) activo de una dirección, para selectores como el de Control de Acceso. */
    @Transactional(readOnly = true)
    public List<Usuario> obtenerPersonalActivoPorDireccion(Long direccionId) {
        return usuarioRepository.findActivosByDireccionIdAndRolIn(direccionId,
                List.of("ROLE_ADMIN", "ROLE_DIRECTOR", "ROLE_DOCENTE"));
    }

    @Transactional(readOnly = true)
    public List<DireccionDTO> obtenerDireccionesDelUsuarioActual() {
        String identifier = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByIdentifierWithDirecciones(identifier)
                .map(usuario -> usuario.getDirecciones().stream()
                        .filter(Direccion::getActiva)
                        .map(DireccionDTO::new)
                        .toList())
                .orElse(Collections.emptyList());
    }

    /** La última dirección con la que trabajó el usuario autenticado, o null si nunca eligió una. */
    @Transactional(readOnly = true)
    public Long obtenerUltimaDireccionIdDelUsuarioActual() {
        String identifier = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByIdentifier(identifier)
                .map(Usuario::getUltimaDireccion)
                .map(Direccion::getId)
                .orElse(null);
    }

    /** Recuerda la dirección elegida para que se auto-seleccione en el próximo login. */
    public void actualizarUltimaDireccion(Long usuarioId, Direccion direccion) {
        usuarioRepository.findById(usuarioId).ifPresent(usuario -> {
            usuario.setUltimaDireccion(direccion);
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
        return usuarioRepository.findByEmailWithDirecciones(username).orElse(null);
    }

    @CacheEvict(value = "usuarios", allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void evictAllCaches() {
    }

    // ── CRUD global de Usuarios (pantalla /usuarios, admin sin dirección seleccionada) ──────────

    @Transactional(readOnly = true)
    public List<Usuario> listarTodosConDirecciones(String filtro) {
        List<Usuario> todos = usuarioRepository.findAllWithDirecciones();
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
        return usuarioRepository.findByIdWithDirecciones(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    public Usuario guardar(Long id, String nombre, String email, String usuarioLogin, String cedula,
            String rawPassword, boolean activo, List<Long> rolIds, List<Long> direccionIds) {
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

        Set<Direccion> direcciones = direccionIds == null ? Set.of() :
                direccionIds.stream()
                        .map(iid -> direccionRepository.findById(iid)
                                .orElseThrow(() -> new IllegalArgumentException("Dirección no encontrada: " + iid)))
                        .collect(Collectors.toSet());
        boolean estudiante = roles.stream().anyMatch(r -> "ROLE_ESTUDIANTE".equals(r.getNombre()));
        if (estudiante && direcciones.size() != 1) {
            throw new IllegalArgumentException("Un estudiante pertenece a una sola dirección.");
        }
        u.setDirecciones(direcciones);

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
