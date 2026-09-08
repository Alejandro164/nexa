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
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.Rol;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.InstitucionRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.RolRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class PersonalService {

    private static final Logger log = LoggerFactory.getLogger(PersonalService.class);

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final InstitucionRepository institucionRepository;
    private final NivelAcademicoRepository nivelAcademicoRepository;
    private final PasswordEncoder passwordEncoder;

    public PersonalService(UsuarioRepository usuarioRepository,
                           RolRepository rolRepository,
                           InstitucionRepository institucionRepository,
                           NivelAcademicoRepository nivelAcademicoRepository,
                           PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.institucionRepository = institucionRepository;
        this.nivelAcademicoRepository = nivelAcademicoRepository;
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
        return guardar(institucionId, id, nombre, email, usuario, cedula, rawPassword, activo, rolIds, null);
    }

    public Usuario guardar(Long institucionId, Long id, String nombre, String email,
                           String usuario, String cedula, String rawPassword,
                           boolean activo, List<Long> rolIds, Long nivelId) {
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

        if (nivelId != null) {
            NivelAcademico nivel = nivelAcademicoRepository.findByIdAndInstitucionId(nivelId, institucionId)
                    .orElseThrow(() -> new IllegalArgumentException("Sección no encontrada"));
            u.setNivelAcademico(nivel);
        } else {
            u.setNivelAcademico(null);
        }

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

    // ─── VÍNCULO PADRE-ESTUDIANTE ────────────────────────────────
    // La relación es dueña del lado padre.estudiantes (@JoinTable en esa dirección) — vincular y
    // desvincular siempre mutan y guardan al padre, nunca al estudiante directamente.

    public void vincularPadre(Long institucionId, Long estudianteId, Long padreId) {
        Usuario estudiante = obtenerPorId(institucionId, estudianteId);
        Usuario padre = obtenerPorId(institucionId, padreId);
        if (!usuarioRepository.existeVinculoPadreEstudiante(padre.getId(), estudiante.getId())) {
            padre.getEstudiantes().add(estudiante);
            usuarioRepository.save(padre);
            log.info("Vínculo padre-estudiante creado: padre={}, estudiante={}", padre.getId(), estudiante.getId());
        }
    }

    public void desvincularPadre(Long institucionId, Long estudianteId, Long padreId) {
        Usuario estudiante = obtenerPorId(institucionId, estudianteId);
        Usuario padre = obtenerPorId(institucionId, padreId);
        padre.getEstudiantes().remove(estudiante);
        usuarioRepository.save(padre);
        log.info("Vínculo padre-estudiante eliminado: padre={}, estudiante={}", padre.getId(), estudiante.getId());
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarPadresDe(Long institucionId, Long estudianteId) {
        obtenerPorId(institucionId, estudianteId);
        return usuarioRepository.findPadresByEstudianteId(estudianteId);
    }
}
