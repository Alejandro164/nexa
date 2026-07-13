package com.chavescr.nexa.service;

import java.text.Normalizer;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.entity.Rol;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.InstitucionRepository;
import com.chavescr.nexa.repository.RolRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class PersonalService {

    private static final Logger log = LoggerFactory.getLogger(PersonalService.class);

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final InstitucionRepository institucionRepository;
    private final PasswordEncoder passwordEncoder;

    public PersonalService(UsuarioRepository usuarioRepository,
                           RolRepository rolRepository,
                           InstitucionRepository institucionRepository,
                           PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.institucionRepository = institucionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarTodos(Long institucionId) {
        return usuarioRepository.findAllByInstitucionId(institucionId);
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarPorRol(Long institucionId, String rolNombre) {
        return usuarioRepository.findAllByInstitucionIdAndRol(institucionId, rolNombre);
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarPorRol(Long institucionId, String rolNombre, String filtro) {
        List<Usuario> todos = usuarioRepository.findAllByInstitucionIdAndRol(institucionId, rolNombre);
        if (filtro == null || filtro.isBlank()) {
            return todos;
        }
        String f = normalizar(filtro.trim());
        return todos.stream()
                .filter(u -> normalizar(u.getNombre()).contains(f)
                        || (u.getCedula() != null && normalizar(u.getCedula()).contains(f)))
                .toList();
    }

    private String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase();
    }

    @Transactional(readOnly = true)
    public List<Rol> listarRoles() {
        return rolRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Rol obtenerRolPorNombre(String nombre) {
        return rolRepository.findByNombre(nombre)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + nombre));
    }

    @Transactional(readOnly = true)
    public Usuario obtenerPorId(Long institucionId, Long id) {
        return usuarioRepository.findById(id)
                .filter(u -> u.getInstituciones().stream().anyMatch(i -> i.getId().equals(institucionId)))
                .orElseThrow(() -> new IllegalArgumentException("Funcionario no encontrado"));
    }

    public Usuario guardar(Long institucionId, Long id, String nombre, String email,
                           String usuario, String cedula, String rawPassword,
                           boolean activo, List<Long> rolIds) {
        Usuario u;
        if (id == null) {
            if (rawPassword == null || rawPassword.isBlank()) {
                throw new IllegalArgumentException("La contraseña es obligatoria al crear un funcionario");
            }
            u = new Usuario();
        } else {
            u = obtenerPorId(institucionId, id);
        }

        u.setNombre(nombre.trim());
        u.setEmail(email.trim().toLowerCase());
        u.setUsuario(usuario.trim().toLowerCase());
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

        Institucion inst = institucionRepository.findById(institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Institución no encontrada"));
        u.getInstituciones().add(inst);

        Usuario guardado = usuarioRepository.save(u);
        log.info("Personal guardado: id={}, nombre={}", guardado.getId(), guardado.getNombre());
        return guardado;
    }

    public void eliminar(Long institucionId, Long id) {
        Usuario u = obtenerPorId(institucionId, id);
        usuarioRepository.delete(u);
        log.info("Personal eliminado: id={}", id);
    }

    public void toggleActivo(Long institucionId, Long id) {
        Usuario u = obtenerPorId(institucionId, id);
        u.setActivo(!u.getActivo());
        usuarioRepository.save(u);
    }
}
