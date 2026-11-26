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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.EstadoMatricula;
import com.chavescr.nexa.entity.MatriculaEstudiante;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.Rol;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.DireccionRepository;
import com.chavescr.nexa.repository.MatriculaEstudianteRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.RolRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
public class MatriculaEstudianteService {

    private static final Logger log = LoggerFactory.getLogger(MatriculaEstudianteService.class);
    private static final String ROL_ESTUDIANTE = "ROLE_ESTUDIANTE";
    private static final List<EstadoMatricula> ESTADOS_ABIERTOS = List.of(
            EstadoMatricula.PREMATRICULA, EstadoMatricula.BORRADOR);

    private final MatriculaEstudianteRepository matriculaRepository;
    private final DireccionRepository direccionRepository;
    private final NivelAcademicoRepository nivelAcademicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public MatriculaEstudianteService(
            MatriculaEstudianteRepository matriculaRepository,
            DireccionRepository direccionRepository,
            NivelAcademicoRepository nivelAcademicoRepository,
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder) {
        this.matriculaRepository = matriculaRepository;
        this.direccionRepository = direccionRepository;
        this.nivelAcademicoRepository = nivelAcademicoRepository;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<MatriculaEstudiante> listar(Long direccionId, EstadoMatricula estado,
            String filtro, String campo, Integer periodo) {
        List<MatriculaEstudiante> todos = matriculaRepository.findAllByDireccionIdAndEstado(direccionId, estado);
        String campoNormalizado = PersonalService.normalizarCampoEstudiante(campo);
        String f = filtro == null || filtro.isBlank() ? null : normalizar(filtro.trim());

        return todos.stream()
                .filter(m -> periodo == null || periodo.equals(m.getAnioMatricula()))
                .filter(m -> f == null || coincideBusqueda(m, f, campoNormalizado))
                .toList();
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<Integer> listarPeriodos(Long direccionId, EstadoMatricula estado) {
        List<MatriculaEstudiante> registros = matriculaRepository.findAllByDireccionIdAndEstado(direccionId, estado);
        TreeSet<Integer> anios = new TreeSet<>(Comparator.reverseOrder());
        for (MatriculaEstudiante m : registros) {
            if (m.getAnioMatricula() != null) {
                anios.add(m.getAnioMatricula());
            }
        }
        anios.add(Year.now().getValue());
        return new ArrayList<>(anios);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public MatriculaEstudiante obtener(Long direccionId, Long id, EstadoMatricula estadoEsperado) {
        MatriculaEstudiante m = matriculaRepository.findByIdAndDireccionId(id, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Registro no encontrado."));
        if (estadoEsperado != null && m.getEstado() != estadoEsperado) {
            throw new IllegalArgumentException("El registro ya no se encuentra en esta etapa.");
        }
        return m;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Optional<MatriculaEstudiante> buscarPrematriculaPorCedula(Long direccionId, String cedula) {
        if (direccionId == null || cedula == null || cedula.isBlank()) {
            return Optional.empty();
        }
        String buscadaCompacta = limpiarSeparadores(normalizar(cedula.trim()));
        if (buscadaCompacta.isEmpty()) {
            return Optional.empty();
        }
        return matriculaRepository.findAllByDireccionIdAndEstado(direccionId, EstadoMatricula.PREMATRICULA)
                .stream()
                .filter(m -> Boolean.TRUE.equals(m.getActivo()))
                .filter(m -> m.getCedula() != null
                        && limpiarSeparadores(normalizar(m.getCedula())).equals(buscadaCompacta))
                .findFirst();
    }

    @Transactional(rollbackFor = Exception.class)
    public MatriculaEstudiante crearPrematricula(Long direccionId, String nombre, String cedula,
            String email, String usuario, String rawPassword, Long nivelId, Integer anioMatricula,
            boolean activo, List<Long> padreIds) {
        Direccion direccion = obtenerDireccion(direccionId);
        MatriculaEstudiante m = new MatriculaEstudiante();
        m.setDireccion(direccion);
        m.setEstado(EstadoMatricula.PREMATRICULA);
        aplicarDatosFormulario(direccionId, m, nombre, cedula, email, usuario, rawPassword, nivelId, anioMatricula, activo);
        sincronizarPadres(direccion, m, padreIds);
        MatriculaEstudiante guardado = matriculaRepository.save(m);
        log.info("Prematrícula creada: id={}, nombre={}", guardado.getId(), guardado.getNombre());
        return guardado;
    }

    @Transactional(rollbackFor = Exception.class)
    public MatriculaEstudiante guardarBorrador(Long direccionId, Long prematriculaId, String nombre, String cedula,
            String email, String usuario, String rawPassword, Long nivelId, Integer anioMatricula,
            boolean activo, List<Long> padreIds) {
        Direccion direccion = obtenerDireccion(direccionId);
        MatriculaEstudiante m;
        if (prematriculaId != null) {
            m = obtener(direccionId, prematriculaId, EstadoMatricula.PREMATRICULA);
        } else {
            m = new MatriculaEstudiante();
            m.setDireccion(direccion);
        }
        m.setEstado(EstadoMatricula.BORRADOR);
        aplicarDatosFormulario(direccionId, m, nombre, cedula, email, usuario, rawPassword, nivelId, anioMatricula, activo);
        if (padreIds != null) {
            sincronizarPadres(direccion, m, padreIds);
        }
        MatriculaEstudiante guardado = matriculaRepository.save(m);
        log.info("Borrador de matrícula guardado: id={}, nombre={}", guardado.getId(), guardado.getNombre());
        return guardado;
    }

    @Transactional(rollbackFor = Exception.class)
    public Usuario completarDesdeFormulario(Long direccionId, Long prematriculaId, String nombre, String cedula,
            String email, String usuario, String rawPassword, Long nivelId, Integer anioMatricula,
            boolean activo, List<Long> padreIds) {
        Direccion direccion = obtenerDireccion(direccionId);
        MatriculaEstudiante m;
        if (prematriculaId != null) {
            m = obtener(direccionId, prematriculaId, EstadoMatricula.PREMATRICULA);
        } else {
            m = new MatriculaEstudiante();
            m.setDireccion(direccion);
            m.setEstado(EstadoMatricula.BORRADOR);
        }
        aplicarDatosFormulario(direccionId, m, nombre, cedula, email, usuario, rawPassword, nivelId, anioMatricula, activo);
        if (padreIds != null) {
            sincronizarPadres(direccion, m, padreIds);
        }
        return finalizarMatricula(direccion, m);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<Usuario> listarPadresDe(Long direccionId, Long id, EstadoMatricula estadoEsperado) {
        MatriculaEstudiante m = obtener(direccionId, id, estadoEsperado);
        return m.getPadres().stream()
                .sorted(Comparator.comparing(Usuario::getNombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void vincularPadre(Long direccionId, Long id, EstadoMatricula estadoEsperado, Long padreId) {
        Direccion direccion = obtenerDireccion(direccionId);
        MatriculaEstudiante m = obtener(direccionId, id, estadoEsperado);
        Usuario padre = obtenerPadreValido(direccion, padreId);
        if (m.getPadres().stream().noneMatch(p -> p.getId().equals(padre.getId()))) {
            m.getPadres().add(padre);
            matriculaRepository.save(m);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void desvincularPadre(Long direccionId, Long id, EstadoMatricula estadoEsperado, Long padreId) {
        MatriculaEstudiante m = obtener(direccionId, id, estadoEsperado);
        m.getPadres().removeIf(p -> p.getId().equals(padreId));
        matriculaRepository.save(m);
    }

    @Transactional(rollbackFor = Exception.class)
    public Usuario completarBorrador(Long direccionId, Long id) {
        Direccion direccion = obtenerDireccion(direccionId);
        MatriculaEstudiante m = obtener(direccionId, id, EstadoMatricula.BORRADOR);
        if (!Boolean.TRUE.equals(m.getActivo())) {
            throw new IllegalArgumentException("No puedes completar un borrador desactivado. Actívalo primero.");
        }
        return finalizarMatricula(direccion, m);
    }

    @Transactional(rollbackFor = Exception.class)
    public MatriculaEstudiante actualizarCampo(Long direccionId, Long id, EstadoMatricula estadoEsperado,
            String campo, String valor) {
        MatriculaEstudiante m = obtener(direccionId, id, estadoEsperado);
        String clave = campo == null ? "" : campo.trim();
        switch (clave) {
            case "nombre" -> m.setNombre(validarNombre(valor));
            case "cedula" -> {
                String cedula = normalizarOpcional(valor, 30, "La cédula no puede superar los 30 caracteres.");
                if (cedula != null) {
                    validarCedulaDisponible(direccionId, cedula, m.getId());
                }
                m.setCedula(cedula);
            }
            case "email" -> {
                String email = normalizarEmailOpcional(valor);
                if (email != null) {
                    validarEmailDisponible(direccionId, email, m.getId());
                }
                m.setEmail(email);
            }
            case "usuario" -> {
                String handle = normalizarUsuarioOpcional(valor);
                if (handle != null) {
                    validarUsuarioDisponible(direccionId, handle, m.getId());
                }
                m.setUsuario(handle);
            }
            case "nivelId" -> {
                if (valor == null || valor.isBlank()) {
                    m.setNivelAcademico(null);
                } else {
                    Long nivelId;
                    try {
                        nivelId = Long.valueOf(valor.trim());
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("La sección seleccionada no es válida.");
                    }
                    NivelAcademico nivel = nivelAcademicoRepository.findByIdAndDireccionId(nivelId, direccionId)
                            .orElseThrow(() -> new IllegalArgumentException("Sección no encontrada."));
                    m.setNivelAcademico(nivel);
                }
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
                m.setAnioMatricula(validarAnio(anio));
            }
            case "activo" -> {
                if (valor == null || (!"true".equalsIgnoreCase(valor.trim()) && !"false".equalsIgnoreCase(valor.trim()))) {
                    throw new IllegalArgumentException("El estado seleccionado no es válido.");
                }
                m.setActivo(Boolean.parseBoolean(valor.trim()));
            }
            case "password" -> {
                if (valor == null || valor.isBlank()) {
                    throw new IllegalArgumentException("La contraseña no puede estar vacía.");
                }
                m.setPasswordHash(passwordEncoder.encode(valor));
            }
            default -> throw new IllegalArgumentException("El campo seleccionado no se puede editar.");
        }
        return matriculaRepository.save(m);
    }

    @Transactional(rollbackFor = Exception.class)
    public void desactivar(Long direccionId, Long id, EstadoMatricula estadoEsperado) {
        MatriculaEstudiante m = obtener(direccionId, id, estadoEsperado);
        if (Boolean.TRUE.equals(m.getActivo())) {
            m.setActivo(false);
            matriculaRepository.save(m);
            log.info("Registro {} desactivado (soft delete): id={}", estadoEsperado, id);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void toggleActivo(Long direccionId, Long id, EstadoMatricula estadoEsperado) {
        MatriculaEstudiante m = obtener(direccionId, id, estadoEsperado);
        m.setActivo(!Boolean.TRUE.equals(m.getActivo()));
        matriculaRepository.save(m);
    }

    // ─── Internos ──────────────────────────────────────────────────────────────

    private Usuario finalizarMatricula(Direccion direccion, MatriculaEstudiante m) {
        List<String> faltantes = new ArrayList<>();
        if (m.getNombre() == null || m.getNombre().isBlank()) {
            faltantes.add("Nombre completo");
        }
        if (m.getAnioMatricula() == null) {
            faltantes.add("Período");
        }
        if (m.getEmail() == null || m.getEmail().isBlank()) {
            faltantes.add("Correo electrónico");
        }
        if (m.getUsuario() == null || m.getUsuario().isBlank()) {
            faltantes.add("Usuario");
        }
        if (m.getNivelAcademico() == null) {
            faltantes.add("Sección");
        }
        if (!m.tienePassword()) {
            faltantes.add("Contraseña");
        }
        if (!faltantes.isEmpty()) {
            throw new IllegalArgumentException("Para completar la matrícula faltan: " + String.join(", ", faltantes) + ".");
        }

        if (m.getCedula() != null) {
            validarCedulaDisponible(direccion.getId(), m.getCedula(), m.getId());
        }
        validarEmailDisponible(direccion.getId(), m.getEmail(), m.getId());
        validarUsuarioDisponible(direccion.getId(), m.getUsuario(), m.getId());

        Rol rolEstudiante = rolRepository.findByNombre(ROL_ESTUDIANTE)
                .orElseThrow(() -> new IllegalArgumentException("Rol de estudiante no configurado."));

        Usuario estudiante = new Usuario();
        estudiante.setNombre(m.getNombre());
        estudiante.setCedula(m.getCedula());
        estudiante.setEmail(m.getEmail());
        estudiante.setUsuario(m.getUsuario());
        estudiante.setPassword(m.getPasswordHash());
        estudiante.setNivelAcademico(m.getNivelAcademico());
        estudiante.setAnioMatricula(m.getAnioMatricula());
        estudiante.setActivo(Boolean.TRUE.equals(m.getActivo()));
        estudiante.setRoles(new HashSet<>(Set.of(rolEstudiante)));
        estudiante.setDirecciones(new HashSet<>(Set.of(direccion)));

        Usuario guardado = usuarioRepository.save(estudiante);
        for (Usuario padre : m.getPadres()) {
            boolean yaEnDireccion = padre.getDirecciones().stream()
                    .anyMatch(d -> d.getId().equals(direccion.getId()));
            if (!yaEnDireccion) {
                padre.getDirecciones().add(direccion);
            }
            if (!usuarioRepository.existeVinculoPadreEstudiante(padre.getId(), guardado.getId())) {
                padre.getEstudiantes().add(guardado);
                usuarioRepository.save(padre);
            }
        }
        m.setEstudiante(guardado);
        m.setEstado(EstadoMatricula.COMPLETADA);
        matriculaRepository.save(m);

        log.info("Matrícula completada: matriculaId={}, estudianteId={}, nombre={}",
                m.getId(), guardado.getId(), guardado.getNombre());
        return guardado;
    }

    private void sincronizarPadres(Direccion direccion, MatriculaEstudiante m, List<Long> padreIds) {
        if (padreIds == null) {
            return;
        }
        Set<Usuario> nuevos = new HashSet<>();
        for (Long padreId : padreIds) {
            if (padreId != null) {
                nuevos.add(obtenerPadreValido(direccion, padreId));
            }
        }
        m.setPadres(nuevos);
    }

    private Usuario obtenerPadreValido(Direccion direccion, Long padreId) {
        Usuario padre = usuarioRepository.findById(padreId)
                .orElseThrow(() -> new IllegalArgumentException("Padre o encargado no encontrado."));
        if (padre.getRoles().stream().noneMatch(r -> "ROLE_PADRE".equals(r.getNombre()))) {
            throw new IllegalArgumentException("La persona indicada no tiene rol de padre o encargado.");
        }
        boolean yaEnDireccion = padre.getDirecciones().stream()
                .anyMatch(d -> d.getId().equals(direccion.getId()));
        if (!yaEnDireccion) {
            padre.getDirecciones().add(direccion);
            usuarioRepository.save(padre);
        }
        return padre;
    }

    private void aplicarDatosFormulario(Long direccionId, MatriculaEstudiante m, String nombre, String cedula,
            String email, String usuario, String rawPassword, Long nivelId, Integer anioMatricula, boolean activo) {
        String nombreLimpio = validarNombre(nombre);
        Integer anioValidado = validarAnio(anioMatricula);
        String cedulaLimpia = normalizarOpcional(cedula, 30, "La cédula no puede superar los 30 caracteres.");
        String emailLimpio = normalizarEmailOpcional(email);
        String usuarioLimpio = normalizarUsuarioOpcional(usuario);

        if (cedulaLimpia != null) {
            validarCedulaDisponible(direccionId, cedulaLimpia, m.getId());
        }
        if (emailLimpio != null) {
            validarEmailDisponible(direccionId, emailLimpio, m.getId());
        }
        if (usuarioLimpio != null) {
            validarUsuarioDisponible(direccionId, usuarioLimpio, m.getId());
        }

        NivelAcademico nivel = null;
        if (nivelId != null) {
            nivel = nivelAcademicoRepository.findByIdAndDireccionId(nivelId, direccionId)
                    .orElseThrow(() -> new IllegalArgumentException("Sección no encontrada."));
        }

        m.setNombre(nombreLimpio);
        m.setCedula(cedulaLimpia);
        m.setEmail(emailLimpio);
        m.setUsuario(usuarioLimpio);
        m.setNivelAcademico(nivel);
        m.setAnioMatricula(anioValidado);
        m.setActivo(activo);
        if (rawPassword != null && !rawPassword.isBlank()) {
            m.setPasswordHash(passwordEncoder.encode(rawPassword));
        }
    }

    private Direccion obtenerDireccion(Long direccionId) {
        return direccionRepository.findById(direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Dirección no encontrada."));
    }

    private String validarNombre(String nombre) {
        String limpio = nombre == null ? "" : nombre.trim();
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("El nombre completo es obligatorio.");
        }
        if (limpio.length() > 100) {
            throw new IllegalArgumentException("El nombre completo no puede superar los 100 caracteres.");
        }
        return limpio;
    }

    private Integer validarAnio(Integer anio) {
        if (anio == null) {
            throw new IllegalArgumentException("El período es obligatorio.");
        }
        if (anio < 1990 || anio > 2100) {
            throw new IllegalArgumentException("El período debe ser un año válido entre 1990 y 2100.");
        }
        return anio;
    }

    private String normalizarOpcional(String valor, int max, String msgLargo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = valor.trim();
        if (limpio.length() > max) {
            throw new IllegalArgumentException(msgLargo);
        }
        return limpio;
    }

    private String normalizarEmailOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String email = valor.trim().toLowerCase();
        if (!email.contains("@")) {
            throw new IllegalArgumentException("Ingresa un correo electrónico válido.");
        }
        if (email.length() > 150) {
            throw new IllegalArgumentException("El correo electrónico no puede superar los 150 caracteres.");
        }
        return email;
    }

    private String normalizarUsuarioOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String handle = valor.trim().toLowerCase();
        if (handle.length() > 60) {
            throw new IllegalArgumentException("El nombre de usuario no puede superar los 60 caracteres.");
        }
        return handle;
    }

    private void validarCedulaDisponible(Long direccionId, String cedula, Long excluirMatriculaId) {
        if (usuarioRepository.findByCedula(cedula).isPresent()) {
            throw new IllegalArgumentException("Ya existe un estudiante o usuario registrado con esa cédula.");
        }
        String compacta = limpiarSeparadores(normalizar(cedula));
        boolean duplicada = matriculaRepository.findAllByDireccionIdAndEstadoIn(direccionId, ESTADOS_ABIERTOS)
                .stream()
                .filter(otra -> excluirMatriculaId == null || !otra.getId().equals(excluirMatriculaId))
                .anyMatch(otra -> otra.getCedula() != null
                        && limpiarSeparadores(normalizar(otra.getCedula())).equals(compacta));
        if (duplicada) {
            throw new IllegalArgumentException("Ya existe una prematrícula o borrador registrado con esa cédula.");
        }
    }

    private void validarEmailDisponible(Long direccionId, String email, Long excluirMatriculaId) {
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Ya existe una cuenta registrada con ese correo electrónico.");
        }
        boolean duplicado = matriculaRepository.findAllByDireccionIdAndEstadoIn(direccionId, ESTADOS_ABIERTOS)
                .stream()
                .filter(otra -> excluirMatriculaId == null || !otra.getId().equals(excluirMatriculaId))
                .anyMatch(otra -> email.equalsIgnoreCase(otra.getEmail()));
        if (duplicado) {
            throw new IllegalArgumentException("Ya existe una prematrícula o borrador con ese correo electrónico.");
        }
    }

    private void validarUsuarioDisponible(Long direccionId, String handle, Long excluirMatriculaId) {
        if (usuarioRepository.findByUsuario(handle).isPresent()) {
            throw new IllegalArgumentException("Ese nombre de usuario ya está en uso.");
        }
        boolean duplicado = matriculaRepository.findAllByDireccionIdAndEstadoIn(direccionId, ESTADOS_ABIERTOS)
                .stream()
                .filter(otra -> excluirMatriculaId == null || !otra.getId().equals(excluirMatriculaId))
                .anyMatch(otra -> handle.equalsIgnoreCase(otra.getUsuario()));
        if (duplicado) {
            throw new IllegalArgumentException("Ese nombre de usuario ya está asignado a otra prematrícula o borrador.");
        }
    }

    private boolean coincideBusqueda(MatriculaEstudiante m, String f, String campo) {
        return switch (campo) {
            case "nombre" -> contieneNormalizado(m.getNombre(), f);
            case "email" -> contieneNormalizado(m.getEmail(), f);
            case "cedula" -> coincideCedula(m.getCedula(), f);
            case "seccion" -> coincideSeccion(m.getNivelAcademico(), f);
            case "periodo" -> m.getAnioMatricula() != null && String.valueOf(m.getAnioMatricula()).contains(f);
            case "estado" -> coincideEstado(m.getActivo(), f, false);
            default -> contieneNormalizado(m.getNombre(), f)
                    || contieneNormalizado(m.getEmail(), f)
                    || coincideCedula(m.getCedula(), f)
                    || coincideSeccion(m.getNivelAcademico(), f)
                    || (m.getAnioMatricula() != null && String.valueOf(m.getAnioMatricula()).contains(f))
                    || coincideEstado(m.getActivo(), f, true);
        };
    }

    private boolean contieneNormalizado(String valor, String f) {
        return valor != null && normalizar(valor).contains(f);
    }

    private boolean coincideCedula(String cedula, String f) {
        if (cedula == null) {
            return false;
        }
        if (normalizar(cedula).contains(f)) {
            return true;
        }
        String fCompacto = limpiarSeparadores(f);
        return !fCompacto.isEmpty() && limpiarSeparadores(normalizar(cedula)).contains(fCompacto);
    }

    private boolean coincideSeccion(NivelAcademico nivel, String f) {
        if (nivel == null) {
            return false;
        }
        String completo = normalizar(nivel.getNombreCompleto());
        if (completo.contains(f)) {
            return true;
        }
        String compacto = completo.replaceAll("\\s+", "");
        String fCompacto = f.replaceAll("\\s+", "");
        return !fCompacto.isEmpty() && compacto.contains(fCompacto);
    }

    private boolean coincideEstado(Boolean activo, String f, boolean modoTodas) {
        if (modoTodas && f.length() < 3) {
            return false;
        }
        String etiqueta = Boolean.TRUE.equals(activo) ? "activo" : "inactivo";
        return etiqueta.startsWith(f);
    }

    private static String limpiarSeparadores(String texto) {
        return texto.replaceAll("[\\s-]", "");
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase();
    }
}
