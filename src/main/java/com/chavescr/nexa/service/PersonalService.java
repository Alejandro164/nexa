package com.chavescr.nexa.service;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.Rol;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.DocenteBloqueoLeccionRepository;
import com.chavescr.nexa.repository.DocenteGuiaRepository;
import com.chavescr.nexa.repository.DocenteMateriaRepository;
import com.chavescr.nexa.repository.DireccionRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.RolRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class PersonalService {

    private static final Logger log = LoggerFactory.getLogger(PersonalService.class);

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final DireccionRepository direccionRepository;
    private final NivelAcademicoRepository nivelAcademicoRepository;
    private final DocenteMateriaRepository docenteMateriaRepository;
    private final DocenteGuiaRepository docenteGuiaRepository;
    private final DocenteBloqueoLeccionRepository docenteBloqueoLeccionRepository;
    private final PasswordEncoder passwordEncoder;

    public PersonalService(UsuarioRepository usuarioRepository,
                           RolRepository rolRepository,
                           DireccionRepository direccionRepository,
                           NivelAcademicoRepository nivelAcademicoRepository,
                           DocenteMateriaRepository docenteMateriaRepository,
                           DocenteGuiaRepository docenteGuiaRepository,
                           DocenteBloqueoLeccionRepository docenteBloqueoLeccionRepository,
                           PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.direccionRepository = direccionRepository;
        this.nivelAcademicoRepository = nivelAcademicoRepository;
        this.docenteMateriaRepository = docenteMateriaRepository;
        this.docenteGuiaRepository = docenteGuiaRepository;
        this.docenteBloqueoLeccionRepository = docenteBloqueoLeccionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarTodos(Long direccionId) {
        return usuarioRepository.findAllByDireccionId(direccionId);
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarPorRol(Long direccionId, String rolNombre) {
        return usuarioRepository.findAllByDireccionIdAndRol(direccionId, rolNombre);
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarPorRol(Long direccionId, String rolNombre, String filtro) {
        List<Usuario> todos = usuarioRepository.findAllByDireccionIdAndRol(direccionId, rolNombre);
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
    public Usuario obtenerPorId(Long direccionId, Long id) {
        return usuarioRepository.findById(id)
                .filter(u -> u.getDirecciones().stream().anyMatch(i -> i.getId().equals(direccionId)))
                .orElseThrow(() -> new IllegalArgumentException("Funcionario no encontrado"));
    }

    public Usuario guardar(Long direccionId, Long id, String nombre, String email,
                           String usuario, String cedula, String rawPassword,
                           boolean activo, List<Long> rolIds) {
        return guardar(direccionId, id, nombre, email, usuario, cedula, rawPassword, activo, rolIds, null);
    }

    public Usuario guardar(Long direccionId, Long id, String nombre, String email,
                           String usuario, String cedula, String rawPassword,
                           boolean activo, List<Long> rolIds, Long nivelId) {
        if (direccionId == null) {
            throw new IllegalArgumentException("Seleccione una dirección antes de registrar personas.");
        }
        Direccion inst = direccionRepository.findById(direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Dirección no encontrada"));

        String cedulaNormalizada = cedula != null && !cedula.isBlank() ? cedula.trim() : null;
        String emailNormalizado = email.trim().toLowerCase();
        String usuarioNormalizado = usuario.trim().toLowerCase();

        Usuario u = resolverPersona(direccionId, id, cedulaNormalizada, emailNormalizado, usuarioNormalizado);
        boolean nuevo = u.getId() == null;
        if (!nuevo && id == null && u.getRoles().stream().anyMatch(r -> "ROLE_ESTUDIANTE".equals(r.getNombre()))
                && u.getDirecciones().stream().anyMatch(otra -> !otra.getId().equals(direccionId))) {
            throw new IllegalArgumentException("Este estudiante ya pertenece a otra dirección.");
        }
        if (!nuevo && id == null && u.getRoles().stream().noneMatch(r -> "ROLE_ESTUDIANTE".equals(r.getNombre()))
                && rolIds != null && rolIds.stream().anyMatch(rid -> {
                    Rol rol = rolRepository.findById(rid).orElse(null);
                    return rol != null && "ROLE_ESTUDIANTE".equals(rol.getNombre());
                })) {
            throw new IllegalArgumentException("Ya existe una persona con esa cédula. No se crea otro estudiante.");
        }

        if (nuevo && (rawPassword == null || rawPassword.isBlank())) {
            throw new IllegalArgumentException("La contraseña es obligatoria al crear una persona");
        }

        u.setNombre(nombre.trim());
        u.setEmail(emailNormalizado);
        u.setUsuario(usuarioNormalizado);
        u.setCedula(cedulaNormalizada);
        u.setActivo(activo);

        if (rawPassword != null && !rawPassword.isBlank()) {
            u.setPassword(passwordEncoder.encode(rawPassword));
        }

        Set<Rol> roles = rolIds == null ? Set.of() :
                rolIds.stream()
                      .map(rid -> rolRepository.findById(rid)
                              .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + rid)))
                      .collect(Collectors.toSet());
        if (id == null && !nuevo) {
            u.getRoles().addAll(roles);
            roles = u.getRoles();
        } else {
            u.setRoles(roles);
        }

        if (esEstudiante(roles)) {
            if (u.getDirecciones().stream().anyMatch(otra -> !otra.getId().equals(direccionId))) {
                throw new IllegalArgumentException("Este estudiante ya pertenece a otra dirección.");
            }
            u.setDirecciones(new java.util.HashSet<>(Set.of(inst)));
        } else {
            if (u.getDirecciones() == null) {
                u.setDirecciones(new java.util.HashSet<>());
            }
            u.getDirecciones().add(inst);
        }

        if (nivelId != null) {
            NivelAcademico nivel = nivelAcademicoRepository.findByIdAndDireccionId(nivelId, direccionId)
                    .orElseThrow(() -> new IllegalArgumentException("Sección no encontrada"));
            u.setNivelAcademico(nivel);
        } else if (esEstudiante(roles)) {
            u.setNivelAcademico(null);
        }

        Usuario guardado = usuarioRepository.save(u);
        log.info("Personal guardado: id={}, nombre={}", guardado.getId(), guardado.getNombre());
        return guardado;
    }

    public void eliminar(Long direccionId, Long id) {
        Usuario u = obtenerPorId(direccionId, id);
        docenteMateriaRepository.deleteByDocenteId(id);
        docenteGuiaRepository.deleteByDocenteId(id);
        docenteBloqueoLeccionRepository.deleteByDocenteId(id);
        usuarioRepository.delete(u);
        log.info("Personal eliminado: id={}", id);
    }

    public void toggleActivo(Long direccionId, Long id) {
        Usuario u = obtenerPorId(direccionId, id);
        u.setActivo(!u.getActivo());
        usuarioRepository.save(u);
    }

    // ─── VÍNCULO PADRE-ESTUDIANTE ────────────────────────────────
    // La relación es dueña del lado padre.estudiantes (@JoinTable en esa dirección) — vincular y
    // desvincular siempre mutan y guardan al padre, nunca al estudiante directamente.

    public Optional<Usuario> buscarPadrePorCedula(Long direccionId, String cedula) {
        if (direccionId == null || cedula == null || cedula.isBlank()) {
            return Optional.empty();
        }
        Optional<Usuario> padre = usuarioRepository.findByCedula(cedula.trim())
                .filter(u -> u.getRoles().stream().anyMatch(r -> "ROLE_PADRE".equals(r.getNombre())));
        padre.ifPresent(existente -> {
            boolean yaEsta = existente.getDirecciones().stream().anyMatch(i -> i.getId().equals(direccionId));
            if (!yaEsta) {
                Direccion inst = direccionRepository.findById(direccionId)
                        .orElseThrow(() -> new IllegalArgumentException("Dirección no encontrada"));
                existente.getDirecciones().add(inst);
                usuarioRepository.save(existente);
            }
        });
        return padre;
    }

    public void vincularPadre(Long direccionId, Long estudianteId, Long padreId) {
        Usuario estudiante = obtenerPorId(direccionId, estudianteId);
        Usuario padre = usuarioRepository.findById(padreId)
                .orElseThrow(() -> new IllegalArgumentException("Padre no encontrado"));
        if (padre.getRoles().stream().noneMatch(r -> "ROLE_PADRE".equals(r.getNombre()))) {
            throw new IllegalArgumentException("La persona indicada no es un padre.");
        }
        boolean yaEsta = padre.getDirecciones().stream().anyMatch(i -> i.getId().equals(direccionId));
        if (!yaEsta) {
            Direccion inst = direccionRepository.findById(direccionId)
                    .orElseThrow(() -> new IllegalArgumentException("Dirección no encontrada"));
            padre.getDirecciones().add(inst);
        }
        if (!usuarioRepository.existeVinculoPadreEstudiante(padre.getId(), estudiante.getId())) {
            padre.getEstudiantes().add(estudiante);
            usuarioRepository.save(padre);
            log.info("Vínculo padre-estudiante creado: padre={}, estudiante={}", padre.getId(), estudiante.getId());
        }
    }

    public void desvincularPadre(Long direccionId, Long estudianteId, Long padreId) {
        Usuario estudiante = obtenerPorId(direccionId, estudianteId);
        Usuario padre = obtenerPorId(direccionId, padreId);
        padre.getEstudiantes().remove(estudiante);
        usuarioRepository.save(padre);
        log.info("Vínculo padre-estudiante eliminado: padre={}, estudiante={}", padre.getId(), estudiante.getId());
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarPadresDe(Long direccionId, Long estudianteId) {
        obtenerPorId(direccionId, estudianteId);
        return usuarioRepository.findPadresByEstudianteId(estudianteId);
    }

    private Usuario resolverPersona(Long direccionId, Long id, String cedula, String email, String usuario) {
        if (id != null) {
            return obtenerPorId(direccionId, id);
        }
        Usuario porCedula = cedula == null ? null : usuarioRepository.findByCedula(cedula).orElse(null);
        Usuario porEmail = usuarioRepository.findByEmail(email).orElse(null);
        Usuario porUsuario = usuarioRepository.findByUsuario(usuario).orElse(null);
        Usuario existente = primero(porCedula, porEmail, porUsuario);
        if (existente == null) {
            return new Usuario();
        }
        if (porCedula != null && porEmail != null && !porCedula.getId().equals(porEmail.getId())) {
            throw new IllegalArgumentException("La cédula y el correo pertenecen a personas distintas.");
        }
        if (porUsuario != null && !porUsuario.getId().equals(existente.getId())) {
            throw new IllegalArgumentException("Ese usuario ya pertenece a otra persona.");
        }
        return existente;
    }

    private static Usuario primero(Usuario... candidatos) {
        for (Usuario candidato : candidatos) {
            if (candidato != null) {
                return candidato;
            }
        }
        return null;
    }

    private static boolean esEstudiante(Set<Rol> roles) {
        return roles.stream().anyMatch(r -> "ROLE_ESTUDIANTE".equals(r.getNombre()));
    }
}
