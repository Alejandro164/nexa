package com.chavescr.nexa.config;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.CicloEducativo;
import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.OfertaEducativa;
import com.chavescr.nexa.entity.Rol;
import com.chavescr.nexa.entity.TipoMateria;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.DireccionRepository;
import com.chavescr.nexa.repository.InstitucionRepository;
import com.chavescr.nexa.repository.MateriaRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.RolRepository;
import com.chavescr.nexa.repository.TipoMateriaRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

/**
 * Inicializa datos de ejemplo al arrancar en perfil "dev".
 * Es idempotente: verifica la existencia antes de insertar,
 * por lo que es seguro reiniciar la aplicación sin duplicar datos.
 * 
 * docker exec -i nexa_db_dev psql -U postgres -d nexa -c "
 * TRUNCATE TABLE usuario_roles, usuario_direcciones, usuarios RESTART
 * IDENTITY CASCADE;
 * TRUNCATE TABLE roles RESTART IDENTITY CASCADE;
 * TRUNCATE TABLE direcciones RESTART IDENTITY CASCADE;
 * "
 */
@Component
@Profile("dev")
@Order(10)
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String[] NOMBRES_PERSONAS = {
            "Carlos", "Luis", "José", "Juan", "Pedro", "Miguel", "Fernando", "Ricardo", "Eduardo", "Alberto",
            "Roberto", "Manuel", "Francisco", "Gustavo", "Rodrigo", "Sergio", "Mario", "Víctor", "Óscar", "Raúl",
            "María", "Ana", "Laura", "Carmen", "Patricia", "Silvia", "Mónica", "Adriana", "Gabriela", "Daniela",
            "Andrea", "Paola", "Karla", "Vanessa", "Melissa", "Tatiana", "Ivannia", "Yendry", "Priscilla", "Natalia"
    };

    private static final String[] APELLIDOS_PERSONAS = {
            "Rodríguez", "González", "Hernández", "Pérez", "Sánchez", "Ramírez", "Torres", "Flores", "Rivera", "Gómez",
            "Díaz", "Reyes", "Morales", "Cruz", "Ortiz", "Gutiérrez", "Chavarría", "Solís", "Barrantes", "Mora",
            "Vargas", "Castro", "Jiménez", "Rojas", "Alvarado", "Segura", "Zamora", "Quesada", "Brenes", "Araya"
    };

    private final RolRepository rolRepository;
    private final DireccionRepository direccionRepository;
    private final UsuarioRepository usuarioRepository;
    private final NivelAcademicoRepository nivelAcademicoRepository;
    private final InstitucionRepository institucionRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RolRepository rolRepository,
            DireccionRepository direccionRepository,
            UsuarioRepository usuarioRepository,
            NivelAcademicoRepository nivelAcademicoRepository,
            InstitucionRepository institucionRepository,
            PasswordEncoder passwordEncoder) {
        this.rolRepository = rolRepository;
        this.direccionRepository = direccionRepository;
        this.usuarioRepository = usuarioRepository;
        this.nivelAcademicoRepository = nivelAcademicoRepository;
        this.institucionRepository = institucionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("=== [DataInitializer] Verificando datos iniciales ===");

        // ── 1. Roles ──────────────────────────────────────────────────────────
        Rol rolAdmin = crearRolSiNoExiste("ROLE_ADMIN");
        Rol rolDirector = crearRolSiNoExiste("ROLE_DIRECTOR");
        Rol rolDocente = crearRolSiNoExiste("ROLE_DOCENTE");
        Rol rolPadre = crearRolSiNoExiste("ROLE_PADRE");
        Rol rolEstudiante = crearRolSiNoExiste("ROLE_ESTUDIANTE");
        Rol rolSystemConfig = crearRolSiNoExiste("ROLE_SYSTEM_CONFIG");

        // ── 2. Direcciones ──────────────────────────────────────────────────
        // El código (presupuestario) es la llave de idempotencia; la cédula jurídica es la
        // que usa Nube Nexa para nombrar la carpeta de archivos de la institución.
        Direccion instAlpha = crearDireccionSiNoExiste("Liceo Alpha", "1790012301001", "3-007-045001",
                "Av. Principal 100");
        Direccion instBeta = crearDireccionSiNoExiste("Colegio Beta", "1790098765001", "3-007-045002",
                "Calle Secundaria 200");
        Direccion instGamma = crearDireccionSiNoExiste("Escuela Gamma", "9000123456-1", "3-007-045003",
                "Zona Industrial 300");

        // ── 3. Usuarios ───────────────────────────────────────────────────────
        Usuario admin = crearUsuarioSiNoExiste(
                "Alejandro Chaves", "admin@empresa.com", "admin",
                "1-2345-6789", "admin", true,
                Set.of(rolAdmin, rolSystemConfig), Set.of(instAlpha, instBeta, instGamma));
        asegurarRol(admin, rolSystemConfig);

        Usuario aaron = crearUsuarioSiNoExiste(
                "Aaron Alfaro Zamora", "aaronalfarozamora@gmail.com", "aaron",
                "1-1111-1111", "aaron1234", true,
                Set.of(rolAdmin, rolSystemConfig), Set.of(instAlpha, instBeta, instGamma));
        asegurarRol(aaron, rolAdmin);
        asegurarRol(aaron, rolSystemConfig);

        Usuario usuarioDirector = crearUsuarioSiNoExiste(
                "María González", "maria@empresa.com", "director",
                "2-3456-7890", "123", true,
                Set.of(rolDirector), Set.of(instAlpha));
        actualizarCredenciales(usuarioDirector, "director", "123");

        crearUsuarioSiNoExiste(
                "Carlos López", "carlos@empresa.com", "carlos.lopez",
                "3-4567-8901", "user1234", true,
                Set.of(rolDocente), Set.of(instBeta));

        crearUsuarioSiNoExiste(
                "Ana Rodríguez", "ana@empresa.com", "ana.rodriguez",
                "4-5678-9012", "user1234", true,
                Set.of(rolDirector, rolDocente), Set.of(instAlpha, instGamma));

        crearUsuarioSiNoExiste(
                "Luis Pérez", "luis@empresa.com", "luis.perez",
                "5-6789-0123", "user1234", false, // inactivo
                Set.of(rolDocente), Set.of(instGamma));

        Usuario rosa = crearUsuarioSiNoExiste(
                "Rosa Jiménez", "rosa@empresa.com", "rosa.jimenez",
                "6-7890-1234", "user1234", true,
                Set.of(rolPadre), Set.of(instAlpha));

        Usuario manuel = crearUsuarioSiNoExiste(
                "Manuel Alpízar", "manuel.alpizar@empresa.com", "manuel.alpizar",
                "9-6789-0123", "user1234", true,
                Set.of(rolPadre), Set.of(instAlpha));

        Usuario jorge = crearUsuarioSiNoExiste(
                "Jorge Salas", "jorge@empresa.com", "jorge.salas",
                "7-8901-2345", "user1234", true,
                Set.of(rolPadre), Set.of(instBeta));

        Usuario marcela = crearUsuarioSiNoExiste(
                "Marcela Vindas", "marcela@empresa.com", "marcela.vindas",
                "8-9012-3456", "user1234", true,
                Set.of(rolPadre), Set.of(instGamma));

        Usuario sofia = crearUsuarioSiNoExiste(
                "Sofía Alpízar Soto", "sofia.alpizar@empresa.com", "sofia.alpizar",
                "9-0123-4567", "user1234", true,
                Set.of(rolEstudiante), Set.of(instAlpha));

        Usuario diego = crearUsuarioSiNoExiste(
                "Diego Ramírez Vargas", "diego.ramirez@empresa.com", "diego.ramirez",
                "9-1234-5678", "user1234", true,
                Set.of(rolEstudiante), Set.of(instAlpha));

        Usuario valeria = crearUsuarioSiNoExiste(
                "Valeria Solano Mora", "valeria.solano@empresa.com", "valeria.solano",
                "9-2345-6789", "user1234", true,
                Set.of(rolEstudiante), Set.of(instBeta));

        Usuario kevin = crearUsuarioSiNoExiste(
                "Kevin Araya Castro", "kevin.araya@empresa.com", "kevin.araya",
                "9-3456-7890", "user1234", true,
                Set.of(rolEstudiante), Set.of(instBeta));

        Usuario fernanda = crearUsuarioSiNoExiste(
                "Fernanda Quesada Brenes", "fernanda.quesada@empresa.com", "fernanda.quesada",
                "9-4567-8901", "user1234", true,
                Set.of(rolEstudiante), Set.of(instGamma));

        Usuario andres = crearUsuarioSiNoExiste(
                "Andrés Mata Rojas", "andres.mata@empresa.com", "andres.mata",
                "9-5678-9012", "user1234", true,
                Set.of(rolEstudiante), Set.of(instGamma));

        // ── 3.1 Secciones de los estudiantes creados a mano ───────────────────
        List<NivelAcademico> nivelesAlpha = nivelAcademicoRepository
                .findByDireccionIdAndActivoTrueOrderByGradoAscSeccionAsc(instAlpha.getId());
        asignarSeccionFaltante(sofia, nivelesAlpha, 0);
        asignarSeccionFaltante(diego, nivelesAlpha, 1);
        List<NivelAcademico> nivelesBeta = nivelAcademicoRepository
                .findByDireccionIdAndActivoTrueOrderByGradoAscSeccionAsc(instBeta.getId());
        asignarSeccionFaltante(valeria, nivelesBeta, 0);
        asignarSeccionFaltante(kevin, nivelesBeta, 1);
        List<NivelAcademico> nivelesGamma = nivelAcademicoRepository
                .findByDireccionIdAndActivoTrueOrderByGradoAscSeccionAsc(instGamma.getId());
        asignarSeccionFaltante(fernanda, nivelesGamma, 0);
        asignarSeccionFaltante(andres, nivelesGamma, 1);

        // ── 3.2 Docentes con más de una dirección asociada ──────────────────
        // Un docente puede impartir materias en varios colegios a la vez.
        // Se calculan los tres grupos de candidatos ANTES de guardar ninguno: si se
        // guardara uno por uno, un docente recién vinculado a "destino" podría colarse
        // como candidato de la siguiente llamada (cuyo "origen" es ese mismo "destino").
        List<Usuario> paraBeta = docentesElegiblesParaSegundaDireccion(rolDocente, instAlpha, instBeta, 2);
        List<Usuario> paraGamma = docentesElegiblesParaSegundaDireccion(rolDocente, instBeta, instGamma, 2);
        List<Usuario> paraAlpha = docentesElegiblesParaSegundaDireccion(rolDocente, instGamma, instAlpha, 2);
        agregarDireccionADocentes(paraBeta, instBeta);
        agregarDireccionADocentes(paraGamma, instGamma);
        agregarDireccionADocentes(paraAlpha, instAlpha);

        // ── 4. Relación Padres-Estudiantes ───────────────────────────────────
        // Rosa tiene dos estudiantes; Sofía tiene dos padres (Rosa y Manuel).
        vincularPadreEstudiante(rosa, sofia);
        vincularPadreEstudiante(rosa, diego);
        vincularPadreEstudiante(manuel, sofia);
        vincularPadreEstudiante(jorge, valeria);
        vincularPadreEstudiante(jorge, kevin);
        vincularPadreEstudiante(marcela, fernanda);
        vincularPadreEstudiante(marcela, andres);

        // ── 5. Estudiantes adicionales (mínimo 30 por dirección) ───────────
        List<Usuario> estudiantesGenerados = new ArrayList<>();
        estudiantesGenerados.addAll(generarEstudiantes(instAlpha, rolEstudiante, 28, 0));
        estudiantesGenerados.addAll(generarEstudiantes(instBeta, rolEstudiante, 28, 28));
        estudiantesGenerados.addAll(generarEstudiantes(instGamma, rolEstudiante, 28, 56));

        // ── 6. Padres adicionales (mínimo 50 en total) ────────────────────────
        List<Usuario> padresGenerados = generarPadres(rolPadre, 56, 500);

        // ── 7. Asignar 1 o 2 padres a cada estudiante que aún no tenga ────────
        // Los padres se comparten en un mismo pool entre direcciones, así que un
        // padre puede terminar con hijos en más de una dirección.
        List<Usuario> todosLosPadres = new ArrayList<>(List.of(rosa, manuel, jorge, marcela));
        todosLosPadres.addAll(padresGenerados);
        List<Usuario> todosLosEstudiantes = new ArrayList<>(List.of(sofia, diego, valeria, kevin, fernanda, andres));
        todosLosEstudiantes.addAll(estudiantesGenerados);
        asignarPadresFaltantes(todosLosEstudiantes, todosLosPadres);

        sembrarInstitucionesCostaRica(rolAdmin, rolSystemConfig, rolDirector, rolDocente, rolPadre, rolEstudiante);

        log.info("=== [DataInitializer] Datos iniciales listos ===");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Rol crearRolSiNoExiste(String nombre) {
        return rolRepository.findByNombre(nombre).orElseGet(() -> {
            Rol rol = new Rol(nombre);
            rolRepository.save(rol);
            log.info("  [ROL creado] {}", nombre);
            return rol;
        });
    }

    private Direccion crearDireccionSiNoExiste(String nombre, String codigo, String cedula, String direccion) {
        Direccion registro = direccionRepository.findByCodigo(codigo)
                .or(() -> direccionRepository.findByCedula(cedula))
                .orElseGet(() -> {
                    Direccion inst = new Direccion(nombre, codigo);
                    inst.setCedula(cedula);
                    inst.setDireccion(direccion);
                    direccionRepository.save(inst);
                    log.info("  [DIRECCION creada] {}", nombre);
                    return inst;
                });
        asegurarCedula(registro, cedula);
        return registro;
    }

    /**
     * Las BD de desarrollo sembradas antes de que Nube Nexa usara la cédula quedaron sin ella
     * (en la dirección y en la institución que se generó a partir de esta). Se completa solo
     * si falta y si ningún otro registro la usa ya, para no chocar con la restricción unique.
     */
    private void asegurarCedula(Direccion direccion, String cedula) {
        if (direccion.getCedula() == null && direccionRepository.findByCedula(cedula).isEmpty()) {
            direccion.setCedula(cedula);
            direccionRepository.save(direccion);
            log.info("  [DIRECCION cédula asignada] {} -> {}", direccion.getNombre(), cedula);
        }
        Institucion institucion = direccion.getInstitucion();
        if (institucion != null && institucion.getCedula() == null
                && institucionRepository.findByCedula(cedula).isEmpty()) {
            institucion.setCedula(cedula);
            institucionRepository.save(institucion);
            log.info("  [INSTITUCION cédula asignada] {} -> {}", institucion.getNombre(), cedula);
        }
    }

    private Usuario crearUsuarioSiNoExiste(String nombre, String email, String usuario,
            String cedula, String rawPassword, boolean activo,
            Set<Rol> roles, Set<Direccion> direcciones) {
        Optional<Usuario> existente = usuarioRepository.findByEmail(email);
        if (existente.isPresent()) {
            log.info("  [USUARIO ya existe] {}", email);
            return existente.get();
        }
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setEmail(email);
        u.setUsuario(usuario);
        u.setCedula(cedula);
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setActivo(activo);
        // Copias mutables: los llamadores suelen pasar Set.of(...) (inmutable), y más adelante en el
        // seeding se hace .add() sobre estas mismas colecciones (ej. agregarDireccionADocentes,
        // asignarPadresFaltantes) — asignar el Set.of(...) tal cual revienta con
        // UnsupportedOperationException la primera vez que la base arranca realmente vacía.
        u.setRoles(new HashSet<>(roles));
        u.setDirecciones(new HashSet<>(direcciones));
        usuarioRepository.save(u);
        log.info("  [USUARIO creado] {} / {} ({})", email, usuario, activo ? "activo" : "inactivo");
        return u;
    }

    /** El usuario ya creado no recibe roles nuevos; este paso los agrega sin quitar los que tiene. */
    private void asegurarRol(Usuario usuario, Rol rol) {
        if (usuario.getRoles().stream().anyMatch(r -> rol.getNombre().equals(r.getNombre()))) {
            return;
        }
        usuario.getRoles().add(rol);
        usuarioRepository.save(usuario);
        log.info("  [ROL asignado] {} -> {}", usuario.getEmail(), rol.getNombre());
    }

    private void actualizarCredenciales(Usuario usuario, String handle, String rawPassword) {
        if (handle.equals(usuario.getUsuario()) && passwordEncoder.matches(rawPassword, usuario.getPassword())) {
            return;
        }
        usuario.setUsuario(handle);
        usuario.setPassword(passwordEncoder.encode(rawPassword));
        usuarioRepository.save(usuario);
        log.info("  [USUARIO actualizado] {} / {}", usuario.getEmail(), handle);
    }

    private void vincularPadreEstudiante(Usuario padre, Usuario estudiante) {
        if (usuarioRepository.existeVinculoPadreEstudiante(padre.getId(), estudiante.getId())) {
            return;
        }
        padre.getEstudiantes().add(estudiante);
        usuarioRepository.save(padre);
        log.info("  [VINCULO creado] {} (padre) -> {} (estudiante)", padre.getEmail(), estudiante.getEmail());
    }

    /**
     * Genera {@code cantidad} estudiantes de relleno para una dirección, combinando
     * nombres/apellidos de ejemplo. {@code offset} debe ser distinto por dirección
     * (y del rango usado por {@link #generarPadres}) para que los correos/cédulas
     * generados no colisionen entre sí.
     */
    private List<Usuario> generarEstudiantes(Direccion direccion, Rol rolEstudiante, int cantidad, int offset) {
        List<NivelAcademico> niveles = nivelAcademicoRepository
                .findByDireccionIdAndActivoTrueOrderByGradoAscSeccionAsc(direccion.getId());
        List<Usuario> generados = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            Usuario estudiante = generarPersona(offset + i, rolEstudiante, Set.of(direccion));
            asignarSeccionFaltante(estudiante, niveles, i);
            generados.add(estudiante);
        }
        return generados;
    }

    /**
     * Asigna una sección (grado + sección) activa de la dirección a un estudiante
     * que aún no tenga una, repartiéndolos de forma rotativa entre las secciones
     * disponibles. Si la dirección no tiene secciones configuradas, no hace nada.
     */
    private void asignarSeccionFaltante(Usuario estudiante, List<NivelAcademico> niveles, int indice) {
        if (estudiante.getNivelAcademico() != null || niveles.isEmpty()) {
            return;
        }
        estudiante.setNivelAcademico(niveles.get(indice % niveles.size()));
        usuarioRepository.save(estudiante);
    }

    /**
     * Elige hasta {@code cantidad} docentes activos de {@code origen} que aún no
     * pertenezcan a {@code destino}, para reflejar el caso de uso de un docente que
     * imparte materias en más de un colegio. No guarda nada todavía (ver
     * {@link #agregarDireccionADocentes}).
     */
    private List<Usuario> docentesElegiblesParaSegundaDireccion(Rol rolDocente, Direccion origen,
            Direccion destino, int cantidad) {
        return usuarioRepository
                .findActivosByDireccionIdAndRol(origen.getId(), rolDocente.getNombre())
                .stream()
                .filter(docente -> !docente.getDirecciones().contains(destino))
                .limit(cantidad)
                .toList();
    }

    /** Idempotente: si el docente ya pertenece a la dirección, no duplica nada. */
    private void agregarDireccionADocentes(List<Usuario> docentes, Direccion direccion) {
        for (Usuario docente : docentes) {
            if (docente.getDirecciones().add(direccion)) {
                usuarioRepository.save(docente);
                log.info("  [DOCENTE multi-dirección] {} ahora también en {}", docente.getNombre(),
                        direccion.getNombre());
            }
        }
    }

    /**
     * Genera {@code cantidad} padres de relleno, sin dirección asignada de entrada.
     * Su(s) dirección(es) se completan luego en {@link #asignarPadresFaltantes},
     * a medida que se les vincula con estudiantes de una u otra dirección.
     */
    private List<Usuario> generarPadres(Rol rolPadre, int cantidad, int offset) {
        List<Usuario> generados = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            generados.add(generarPersona(offset + i, rolPadre, new HashSet<>()));
        }
        return generados;
    }

    private Usuario generarPersona(int idx, Rol rol, Set<Direccion> direcciones) {
        String nombre = NOMBRES_PERSONAS[idx % NOMBRES_PERSONAS.length];
        String apellido1 = APELLIDOS_PERSONAS[idx % APELLIDOS_PERSONAS.length];
        String apellido2 = APELLIDOS_PERSONAS[(idx + 11) % APELLIDOS_PERSONAS.length];
        String nombreCompleto = nombre + " " + apellido1 + " " + apellido2;
        String usuarioHandle = normalizarParaHandle(nombre) + "." + normalizarParaHandle(apellido1) + idx;
        String email = usuarioHandle + "@empresa.com";
        String cedula = "9-" + String.format("%04d", idx) + "-" + String.format("%04d", 9999 - idx);

        return crearUsuarioSiNoExiste(nombreCompleto, email, usuarioHandle,
                cedula, "user1234", true, Set.of(rol), direcciones);
    }

    /**
     * Asegura que cada estudiante tenga 1 o 2 padres. Los que ya tienen al menos uno
     * (vínculos deliberados de la sección 4) se dejan intactos; al resto se le asignan
     * padres al azar (con semilla fija para que el resultado sea el mismo en cada
     * reinicio) tomados del mismo pool compartido entre direcciones.
     */
    private void asignarPadresFaltantes(List<Usuario> estudiantes, List<Usuario> padres) {
        Random random = new Random(42);
        for (Usuario estudiante : estudiantes) {
            if (!usuarioRepository.findPadresByEstudianteId(estudiante.getId()).isEmpty()) {
                continue;
            }
            int cantidadPadres = random.nextInt(2) + 1; // 1 o 2
            List<Usuario> disponibles = new ArrayList<>(padres);
            Collections.shuffle(disponibles, random);
            for (int i = 0; i < cantidadPadres; i++) {
                Usuario padre = disponibles.get(i);
                vincularPadreEstudiante(padre, estudiante);
                asegurarDireccionDelPadre(padre, estudiante);
            }
        }
    }

    private void asegurarDireccionDelPadre(Usuario padre, Usuario estudiante) {
        for (Direccion inst : estudiante.getDirecciones()) {
            if (padre.getDirecciones().stream().noneMatch(i -> i.getId().equals(inst.getId()))) {
                padre.getDirecciones().add(inst);
                usuarioRepository.save(padre);
            }
        }
    }

    private String normalizarParaHandle(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase();
    }

    /**
     * Cinco instituciones con datos al estilo de Costa Rica, un usuario de cada rol
     * y treinta estudiantes por institución. La sección del estudiante pertenece a
     * su misma dirección y el grado cabe en la oferta (primaria 1.º-6.º, secundaria 7.º-11.º o 7.º-12.º).
     * Idempotente: reconoce instituciones por cédula jurídica y personas por correo.
     * Contraseña de estas cuentas: {@code nexa2026}.
     */
    private void sembrarInstitucionesCostaRica(Rol rolAdmin, Rol rolSystemConfig, Rol rolDirector,
            Rol rolDocente, Rol rolPadre, Rol rolEstudiante) {
        List<ColegioCostaRica> colegios = List.of(
                new ColegioCostaRica("Escuela República de Costa Rica", "2511001001001", "3-101-700001",
                        "De la iglesia de González Lahmann, 200 m este, San José", "2221-4501",
                        "direccion.republica@mep.go.cr", OfertaEducativa.PRIMARIA, 1, 6, 5, 1),
                new ColegioCostaRica("Liceo de San Pedro", "2512001001002", "3-101-700002",
                        "Barrio Pinto, de la iglesia católica 100 m norte, Montes de Oca, San José", "2225-3102",
                        "direccion.sanpedro@mep.go.cr", OfertaEducativa.SECUNDARIA, 7, 5, 6, 1),
                new ColegioCostaRica("Colegio Técnico Profesional de Grecia", "2523001001003", "3-002-700003",
                        "Del parque central de Grecia, 300 m oeste, Alajuela", "2494-2203",
                        "direccion.ctpgrecia@mep.go.cr", OfertaEducativa.SECUNDARIA, 7, 6, 5, 2),
                new ColegioCostaRica("Escuela José Martí", "2534001001004", "3-003-700004",
                        "Paraíso centro, del parque 50 m sur, Cartago", "2574-1804",
                        "direccion.josemarti@mep.go.cr", OfertaEducativa.PRIMARIA, 1, 6, 5, 3),
                new ColegioCostaRica("Liceo de Liberia", "2555001001005", "3-005-700005",
                        "De la esquina noreste del parque de Liberia, 150 m este, Guanacaste", "2666-0905",
                        "direccion.liberia@mep.go.cr", OfertaEducativa.SECUNDARIA, 7, 5, 6, 5));

        List<Direccion> direcciones = new ArrayList<>();
        for (ColegioCostaRica colegio : colegios) {
            direcciones.add(asegurarColegio(colegio));
        }

        crearUsuarioSiNoExiste("Elena Vargas Solís", "elena.vargas@mep.go.cr", "admin.cr",
                "1-8100-0001", CLAVE_COSTA_RICA, true, Set.of(rolAdmin), new HashSet<>(direcciones));
        Usuario configCr = crearUsuarioSiNoExiste("Roberto Quesada Mora", "roberto.quesada@mep.go.cr", "config.cr",
                "1-8100-0002", CLAVE_COSTA_RICA, true, Set.of(rolAdmin, rolSystemConfig), new HashSet<>(direcciones));
        asegurarRol(configCr, rolAdmin);
        asegurarRol(configCr, rolSystemConfig);
        crearUsuarioSiNoExiste("María Campos Araya", "maria.campos@mep.go.cr", "director.cr",
                "1-8100-0003", CLAVE_COSTA_RICA, true, Set.of(rolDirector), new HashSet<>(direcciones));
        crearUsuarioSiNoExiste("Andrés Calderón Jiménez", "andres.calderon@mep.go.cr", "docente.cr",
                "1-8100-0004", CLAVE_COSTA_RICA, true, Set.of(rolDocente), Set.of(direcciones.get(1)));
        Usuario padre = crearUsuarioSiNoExiste("Laura Méndez Rojas", "laura.mendez@mep.go.cr", "padre.cr",
                "1-8100-0005", CLAVE_COSTA_RICA, true, Set.of(rolPadre), Set.of(direcciones.get(0)));

        int estudiantes = 0;
        List<Usuario> hijosDeLaura = new ArrayList<>();
        for (int s = 0; s < colegios.size(); s++) {
            ColegioCostaRica colegio = colegios.get(s);
            Direccion direccion = direcciones.get(s);
            for (int i = 0; i < ColegioCostaRica.ESTUDIANTES_POR_INSTITUCION; i++) {
                int grado = colegio.gradoDesde + (i / colegio.porGrado);
                NivelAcademico nivel = asegurarNivel(direccion, grado, "A");
                Usuario estudiante = crearEstudianteCostaRica(s, i, colegio, direccion, nivel, rolEstudiante);
                if (s == 0 && hijosDeLaura.size() < 2) {
                    hijosDeLaura.add(estudiante);
                }
                estudiantes++;
            }
        }
        for (Usuario hijo : hijosDeLaura) {
            vincularPadreEstudiante(padre, hijo);
        }
        if (estudiantes != colegios.size() * ColegioCostaRica.ESTUDIANTES_POR_INSTITUCION) {
            throw new IllegalStateException("Se esperaban "
                    + (colegios.size() * ColegioCostaRica.ESTUDIANTES_POR_INSTITUCION)
                    + " estudiantes y se generaron " + estudiantes);
        }
        log.info("  [COSTA RICA] {} instituciones, {} estudiantes, un usuario por rol",
                colegios.size(), estudiantes);
    }

    private Direccion asegurarColegio(ColegioCostaRica colegio) {
        colegio.validar();
        Institucion institucion = institucionRepository.findByCedula(colegio.cedula).orElseGet(Institucion::new);
        institucion.setCedula(colegio.cedula);
        institucion.setNombre(colegio.nombre);
        institucion.setDireccion(colegio.direccion);
        institucion.setTelefono(colegio.telefono);
        institucion.setEmail(colegio.email);
        institucion.setActiva(true);
        institucionRepository.save(institucion);

        Direccion registro = direccionRepository.findByCodigo(colegio.codigo)
                .or(() -> direccionRepository.findByCedula(colegio.cedula))
                .orElseGet(() -> new Direccion(colegio.nombre, colegio.codigo));
        registro.setInstitucion(institucion);
        registro.setNombre(colegio.nombre);
        registro.setCodigo(colegio.codigo);
        registro.setCedula(colegio.cedula);
        registro.setDireccion(colegio.direccion);
        registro.setTelefono(colegio.telefono);
        registro.setEmail(colegio.email);
        registro.setActiva(true);
        registro.setOferta(colegio.oferta);
        registro.setPrincipal(true);
        direccionRepository.save(registro);
        log.info("  [INSTITUCION] {} ({})", colegio.nombre, colegio.oferta.getEtiqueta());
        return registro;
    }

    private NivelAcademico asegurarNivel(Direccion direccion, int grado, String seccion) {
        return nivelAcademicoRepository.findByDireccionIdOrderByGradoAscSeccionAsc(direccion.getId()).stream()
                .filter(nivel -> grado == nivel.getGrado() && seccion.equals(nivel.getSeccion()))
                .findFirst()
                .orElseGet(() -> {
                    NivelAcademico nivel = new NivelAcademico();
                    nivel.setDireccion(direccion);
                    nivel.setGrado(grado);
                    nivel.setSeccion(seccion);
                    nivel.setCiclo(CicloEducativo.deGrado(grado));
                    nivel.setActivo(true);
                    return nivelAcademicoRepository.save(nivel);
                });
    }

    private Usuario crearEstudianteCostaRica(int colegioIdx, int indice, ColegioCostaRica colegio,
            Direccion direccion, NivelAcademico nivel, Rol rolEstudiante) {
        String nombre = NOMBRES_COSTA_RICA[indice % NOMBRES_COSTA_RICA.length];
        String apellido1 = APELLIDOS_COSTA_RICA[(indice + colegioIdx * 2) % APELLIDOS_COSTA_RICA.length];
        String apellido2 = APELLIDOS_COSTA_RICA[(indice + colegioIdx * 2 + 7) % APELLIDOS_COSTA_RICA.length];
        if (apellido1.equals(apellido2)) {
            apellido2 = APELLIDOS_COSTA_RICA[(indice + colegioIdx * 2 + 8) % APELLIDOS_COSTA_RICA.length];
        }
        String marca = String.format("%04d", 8300 + colegioIdx);
        String handle = normalizarParaHandle(nombre) + "." + normalizarParaHandle(apellido1) + "." + marca;
        String email = handle + "@mep.go.cr";
        String cedula = String.format("%d-%s-%04d", colegio.provinciaCedula, marca, indice + 1);
        String telefono = String.format("8%d%02d%04d", colegio.provinciaCedula, colegioIdx, indice + 1);

        Usuario estudiante = crearUsuarioSiNoExiste(nombre + " " + apellido1 + " " + apellido2, email, handle,
                cedula, CLAVE_COSTA_RICA, true, Set.of(rolEstudiante), Set.of(direccion));
        if (nivel.getDireccion() == null || nivel.getDireccion().getId() == null
                || !nivel.getDireccion().getId().equals(direccion.getId())) {
            throw new IllegalStateException("La sección no pertenece a la institución del estudiante " + email);
        }
        int grado = nivel.getGrado();
        int gradoHasta = colegio.gradoDesde + colegio.grados - 1;
        if (grado < colegio.gradoDesde || grado > gradoHasta) {
            throw new IllegalStateException("El grado " + grado + " no cabe en " + colegio.nombre);
        }
        if (estudiante.getNivelAcademico() == null) {
            estudiante.setNivelAcademico(nivel);
        }
        if (estudiante.getTelefono() == null) {
            estudiante.setTelefono(telefono);
        }
        return usuarioRepository.save(estudiante);
    }

    private static final String CLAVE_COSTA_RICA = "nexa2026";

    private static final String[] NOMBRES_COSTA_RICA = {
            "Sofía", "Mateo", "Valentina", "Santiago", "Isabella", "Diego", "Camila", "Sebastián",
            "Mariana", "Andrés", "Lucía", "Gabriel", "Daniela", "Tomás", "Valeria", "Nicolás",
            "Natalia", "Emilio", "Jimena", "Adrián", "Paula", "Fernando", "Victoria", "Ignacio",
            "Abigail", "Martín", "Alejandra", "Joaquín", "Fabiola", "Esteban"
    };

    private static final String[] APELLIDOS_COSTA_RICA = {
            "Rodríguez", "González", "Hernández", "Mora", "Jiménez", "Vargas", "Solís", "Araya",
            "Quesada", "Chaves", "Calderón", "Campos", "Méndez", "Rojas", "Alfaro", "Brenes"
    };

    /** Institución con una sola dirección (la oferta principal) y sus secciones. */
    private record ColegioCostaRica(String nombre, String codigo, String cedula, String direccion,
            String telefono, String email, OfertaEducativa oferta, int gradoDesde, int grados, int porGrado,
            int provinciaCedula) {

        static final int ESTUDIANTES_POR_INSTITUCION = 30;

        void validar() {
            if (porGrado * grados != ESTUDIANTES_POR_INSTITUCION) {
                throw new IllegalStateException(nombre + " no reparte 30 estudiantes en sus grados");
            }
            int gradoHasta = gradoDesde + grados - 1;
            if (gradoDesde < 1 || gradoHasta > 12) {
                throw new IllegalStateException(nombre + " tiene grados fuera de 1.º a 12.º");
            }
            if (oferta == OfertaEducativa.PRIMARIA && (gradoDesde != 1 || gradoHasta > 6)) {
                throw new IllegalStateException(nombre + " es primaria y debe cubrir solo de 1.º a 6.º");
            }
            if (oferta == OfertaEducativa.SECUNDARIA && gradoDesde < 7) {
                throw new IllegalStateException(nombre + " es secundaria y debe empezar en 7.º o después");
            }
            if (provinciaCedula < 1 || provinciaCedula > 7) {
                throw new IllegalStateException(nombre + " tiene una provincia de cédula inválida");
            }
        }
    }
}
