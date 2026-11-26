package com.chavescr.nexa.service;

import java.text.Normalizer;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
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

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<Usuario> listarEstudiantes(Long direccionId, String filtro, String campo, Integer periodo) {
        List<Usuario> todos = usuarioRepository.findAllByDireccionIdAndRol(direccionId, "ROLE_ESTUDIANTE");
        String campoNormalizado = normalizarCampoEstudiante(campo);
        String f = filtro == null || filtro.isBlank() ? null : normalizar(filtro.trim());

        return todos.stream()
                .filter(u -> periodo == null || periodo.equals(u.getAnioMatricula()))
                .filter(u -> f == null || coincideBusquedaEstudiante(u, f, campoNormalizado))
                .toList();
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<Integer> listarPeriodosEstudiantes(Long direccionId) {
        List<Usuario> estudiantes = usuarioRepository.findAllByDireccionIdAndRol(direccionId, "ROLE_ESTUDIANTE");
        TreeSet<Integer> anios = new TreeSet<>(Comparator.reverseOrder());
        for (Usuario u : estudiantes) {
            if (u.getAnioMatricula() != null) {
                anios.add(u.getAnioMatricula());
            }
        }
        anios.add(Year.now().getValue());
        return new ArrayList<>(anios);
    }

    public static String normalizarCampoEstudiante(String campo) {
        if (campo == null || campo.isBlank()) {
            return "todas";
        }
        return switch (campo.trim().toLowerCase()) {
            case "nombre", "email", "cedula", "seccion", "periodo", "estado" -> campo.trim().toLowerCase();
            default -> "todas";
        };
    }

    private boolean coincideBusquedaEstudiante(Usuario u, String f, String campo) {
        return switch (campo) {
            case "nombre" -> contieneNormalizado(u.getNombre(), f);
            case "email" -> contieneNormalizado(u.getEmail(), f);
            case "cedula" -> coincideCedula(u.getCedula(), f);
            case "seccion" -> coincideSeccion(u.getNivelAcademico(), f);
            case "periodo" -> u.getAnioMatricula() != null && String.valueOf(u.getAnioMatricula()).contains(f);
            case "estado" -> coincideEstado(u.getActivo(), f, false);
            default -> contieneNormalizado(u.getNombre(), f)
                    || contieneNormalizado(u.getEmail(), f)
                    || coincideCedula(u.getCedula(), f)
                    || coincideSeccion(u.getNivelAcademico(), f)
                    || (u.getAnioMatricula() != null && String.valueOf(u.getAnioMatricula()).contains(f))
                    || coincideEstado(u.getActivo(), f, true);
        };
    }

    private boolean contieneNormalizado(String valor, String filtroNormalizado) {
        return valor != null && normalizar(valor).contains(filtroNormalizado);
    }

    private boolean coincideCedula(String cedula, String filtroNormalizado) {
        if (cedula == null) {
            return false;
        }
        if (normalizar(cedula).contains(filtroNormalizado)) {
            return true;
        }
        String filtroSinSeparadores = filtroNormalizado.replaceAll("[\\s-]", "");
        if (filtroSinSeparadores.isEmpty()) {
            return false;
        }
        String cedulaSinSeparadores = normalizar(cedula).replaceAll("[\\s-]", "");
        return cedulaSinSeparadores.contains(filtroSinSeparadores);
    }

    private boolean coincideSeccion(NivelAcademico nivel, String filtroNormalizado) {
        if (nivel == null) {
            return false;
        }
        String completo = normalizar(nivel.getNombreCompleto());
        if (completo.contains(filtroNormalizado)) {
            return true;
        }
        String compacto = completo.replaceAll("\\s+", "");
        String filtroCompacto = filtroNormalizado.replaceAll("\\s+", "");
        return !filtroCompacto.isEmpty() && compacto.contains(filtroCompacto);
    }

    private boolean coincideEstado(Boolean activo, String filtroNormalizado, boolean modoTodas) {
        if (modoTodas && filtroNormalizado.length() < 3) {
            return false;
        }
        String etiqueta = Boolean.TRUE.equals(activo) ? "activo" : "inactivo";
        return etiqueta.startsWith(filtroNormalizado);
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
        return guardar(direccionId, id, nombre, email, usuario, cedula, rawPassword, activo, rolIds, nivelId, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public Usuario guardar(Long direccionId, Long id, String nombre, String email,
                           String usuario, String cedula, String rawPassword,
                           boolean activo, List<Long> rolIds, Long nivelId, Integer anioMatricula) {
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

        if (esEstudiante(roles)) {
            if (anioMatricula != null) {
                if (anioMatricula < 1990 || anioMatricula > 2100) {
                    throw new IllegalArgumentException("El período debe ser un año válido entre 1990 y 2100.");
                }
                u.setAnioMatricula(anioMatricula);
            } else if (u.getAnioMatricula() == null) {
                u.setAnioMatricula(Year.now().getValue());
            }
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

    @Transactional(rollbackFor = Exception.class)
    public void desactivar(Long direccionId, Long id) {
        Usuario u = obtenerPorId(direccionId, id);
        if (Boolean.TRUE.equals(u.getActivo())) {
            u.setActivo(false);
            usuarioRepository.save(u);
            log.info("Estudiante desactivado (soft delete): id={}", id);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Usuario actualizarCampoEstudiante(Long direccionId, Long id, String campo, String valor) {
        Usuario u = obtenerPorId(direccionId, id);
        if (!esEstudiante(u.getRoles())) {
            throw new IllegalArgumentException("Estudiante no encontrado.");
        }
        String clave = campo == null ? "" : campo.trim();
        switch (clave) {
            case "nombre" -> {
                String limpio = valor == null ? "" : valor.trim();
                if (limpio.isEmpty()) {
                    throw new IllegalArgumentException("El nombre completo es obligatorio.");
                }
                if (limpio.length() > 100) {
                    throw new IllegalArgumentException("El nombre completo no puede superar los 100 caracteres.");
                }
                u.setNombre(limpio);
            }
            case "cedula" -> {
                String cedula = valor == null || valor.isBlank() ? null : valor.trim();
                if (cedula != null && cedula.length() > 30) {
                    throw new IllegalArgumentException("La cédula no puede superar los 30 caracteres.");
                }
                if (cedula != null && usuarioRepository.findByCedula(cedula)
                        .filter(otro -> !otro.getId().equals(id))
                        .isPresent()) {
                    throw new IllegalArgumentException("Ya existe otra persona registrada con esa cédula.");
                }
                u.setCedula(cedula);
            }
            case "email" -> {
                String email = valor == null ? "" : valor.trim().toLowerCase();
                if (email.isEmpty() || !email.contains("@")) {
                    throw new IllegalArgumentException("Ingresa un correo electrónico válido.");
                }
                if (email.length() > 150) {
                    throw new IllegalArgumentException("El correo electrónico no puede superar los 150 caracteres.");
                }
                if (usuarioRepository.findByEmail(email)
                        .filter(otro -> !otro.getId().equals(id))
                        .isPresent()) {
                    throw new IllegalArgumentException("Ya existe otra persona registrada con ese correo electrónico.");
                }
                u.setEmail(email);
            }
            case "usuario" -> {
                String handle = valor == null ? "" : valor.trim().toLowerCase();
                if (handle.isEmpty()) {
                    throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
                }
                if (handle.length() > 60) {
                    throw new IllegalArgumentException("El nombre de usuario no puede superar los 60 caracteres.");
                }
                if (usuarioRepository.findByUsuario(handle)
                        .filter(otro -> !otro.getId().equals(id))
                        .isPresent()) {
                    throw new IllegalArgumentException("Ese nombre de usuario ya está en uso.");
                }
                u.setUsuario(handle);
            }
            case "nivelId" -> {
                if (valor == null || valor.isBlank()) {
                    throw new IllegalArgumentException("Debes seleccionar una sección.");
                }
                Long nivelId;
                try {
                    nivelId = Long.valueOf(valor.trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("La sección seleccionada no es válida.");
                }
                NivelAcademico nivel = nivelAcademicoRepository.findByIdAndDireccionId(nivelId, direccionId)
                        .orElseThrow(() -> new IllegalArgumentException("Sección no encontrada."));
                u.setNivelAcademico(nivel);
            }
            case "anioMatricula" -> {
                if (valor == null || valor.isBlank()) {
                    throw new IllegalArgumentException("El período es obligatorio.");
                }
                int anio;
                try {
                    anio = Integer.parseInt(valor.trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("El período debe ser un año numérico válido.");
                }
                if (anio < 1990 || anio > 2100) {
                    throw new IllegalArgumentException("El período debe ser un año válido entre 1990 y 2100.");
                }
                u.setAnioMatricula(anio);
            }
            case "activo" -> {
                if (valor == null || (!"true".equalsIgnoreCase(valor.trim()) && !"false".equalsIgnoreCase(valor.trim()))) {
                    throw new IllegalArgumentException("El estado seleccionado no es válido.");
                }
                u.setActivo(Boolean.parseBoolean(valor.trim()));
            }
            case "password" -> {
                if (valor == null || valor.isBlank()) {
                    throw new IllegalArgumentException("La nueva contraseña no puede estar vacía.");
                }
                u.setPassword(passwordEncoder.encode(valor));
            }
            default -> throw new IllegalArgumentException("El campo seleccionado no se puede editar.");
        }
        Usuario guardado = usuarioRepository.save(u);
        log.info("Campo '{}' actualizado para estudiante id={}", clave, id);
        return guardado;
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
