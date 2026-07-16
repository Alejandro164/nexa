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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.Rol;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.InstitucionRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.RolRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

/**
 * Inicializa datos de ejemplo al arrancar en perfil "dev".
 * Es idempotente: verifica la existencia antes de insertar,
 * por lo que es seguro reiniciar la aplicación sin duplicar datos.
 * 
 * docker exec -i nexa_db_dev psql -U postgres -d nexa -c "
 * TRUNCATE TABLE usuario_roles, usuario_instituciones, usuarios RESTART
 * IDENTITY CASCADE;
 * TRUNCATE TABLE roles RESTART IDENTITY CASCADE;
 * TRUNCATE TABLE instituciones RESTART IDENTITY CASCADE;
 * "
 */
@Component
@Profile("dev")
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
    private final InstitucionRepository institucionRepository;
    private final UsuarioRepository usuarioRepository;
    private final NivelAcademicoRepository nivelAcademicoRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RolRepository rolRepository,
            InstitucionRepository institucionRepository,
            UsuarioRepository usuarioRepository,
            NivelAcademicoRepository nivelAcademicoRepository,
            PasswordEncoder passwordEncoder) {
        this.rolRepository = rolRepository;
        this.institucionRepository = institucionRepository;
        this.usuarioRepository = usuarioRepository;
        this.nivelAcademicoRepository = nivelAcademicoRepository;
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

        // ── 2. Instituciones ──────────────────────────────────────────────────
        Institucion instAlpha = crearInstitucionSiNoExiste("Liceo Alpha", "1790012301001", "Av. Principal 100");
        Institucion instBeta = crearInstitucionSiNoExiste("Colegio Beta", "1790098765001", "Calle Secundaria 200");
        Institucion instGamma = crearInstitucionSiNoExiste("Escuela Gamma", "9000123456-1", "Zona Industrial 300");

        // ── 3. Usuarios ───────────────────────────────────────────────────────
        crearUsuarioSiNoExiste(
                "Alejandro Chaves", "admin@empresa.com", "admin",
                "1-2345-6789", "admin", true,
                Set.of(rolAdmin), Set.of(instAlpha, instBeta, instGamma));

        crearUsuarioSiNoExiste(
                "María González", "maria@empresa.com", "maria.gonzalez",
                "2-3456-7890", "editor1", true,
                Set.of(rolDirector), Set.of(instAlpha));

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
                .findByInstitucionIdAndActivoTrueOrderByGradoAscSeccionAsc(instAlpha.getId());
        asignarSeccionFaltante(sofia, nivelesAlpha, 0);
        asignarSeccionFaltante(diego, nivelesAlpha, 1);
        List<NivelAcademico> nivelesBeta = nivelAcademicoRepository
                .findByInstitucionIdAndActivoTrueOrderByGradoAscSeccionAsc(instBeta.getId());
        asignarSeccionFaltante(valeria, nivelesBeta, 0);
        asignarSeccionFaltante(kevin, nivelesBeta, 1);
        List<NivelAcademico> nivelesGamma = nivelAcademicoRepository
                .findByInstitucionIdAndActivoTrueOrderByGradoAscSeccionAsc(instGamma.getId());
        asignarSeccionFaltante(fernanda, nivelesGamma, 0);
        asignarSeccionFaltante(andres, nivelesGamma, 1);

        // ── 4. Relación Padres-Estudiantes ───────────────────────────────────
        // Rosa tiene dos estudiantes; Sofía tiene dos padres (Rosa y Manuel).
        vincularPadreEstudiante(rosa, sofia);
        vincularPadreEstudiante(rosa, diego);
        vincularPadreEstudiante(manuel, sofia);
        vincularPadreEstudiante(jorge, valeria);
        vincularPadreEstudiante(jorge, kevin);
        vincularPadreEstudiante(marcela, fernanda);
        vincularPadreEstudiante(marcela, andres);

        // ── 5. Estudiantes adicionales (mínimo 30 por institución) ───────────
        List<Usuario> estudiantesGenerados = new ArrayList<>();
        estudiantesGenerados.addAll(generarEstudiantes(instAlpha, rolEstudiante, 28, 0));
        estudiantesGenerados.addAll(generarEstudiantes(instBeta, rolEstudiante, 28, 28));
        estudiantesGenerados.addAll(generarEstudiantes(instGamma, rolEstudiante, 28, 56));

        // ── 6. Padres adicionales (mínimo 50 en total) ────────────────────────
        List<Usuario> padresGenerados = generarPadres(rolPadre, 56, 500);

        // ── 7. Asignar 1 o 2 padres a cada estudiante que aún no tenga ────────
        // Los padres se comparten en un mismo pool entre instituciones, así que un
        // padre puede terminar con hijos en más de una institución.
        List<Usuario> todosLosPadres = new ArrayList<>(List.of(rosa, manuel, jorge, marcela));
        todosLosPadres.addAll(padresGenerados);
        List<Usuario> todosLosEstudiantes = new ArrayList<>(List.of(sofia, diego, valeria, kevin, fernanda, andres));
        todosLosEstudiantes.addAll(estudiantesGenerados);
        asignarPadresFaltantes(todosLosEstudiantes, todosLosPadres);

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

    private Institucion crearInstitucionSiNoExiste(String nombre, String codigo, String direccion) {
        return institucionRepository.findByCodigo(codigo).orElseGet(() -> {
            Institucion inst = new Institucion(nombre, codigo);
            inst.setDireccion(direccion);
            institucionRepository.save(inst);
            log.info("  [INSTITUCION creada] {}", nombre);
            return inst;
        });
    }

    private Usuario crearUsuarioSiNoExiste(String nombre, String email, String usuario,
            String cedula, String rawPassword, boolean activo,
            Set<Rol> roles, Set<Institucion> instituciones) {
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
        u.setRoles(roles);
        u.setInstituciones(instituciones);
        usuarioRepository.save(u);
        log.info("  [USUARIO creado] {} / {} ({})", email, usuario, activo ? "activo" : "inactivo");
        return u;
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
     * Genera {@code cantidad} estudiantes de relleno para una institución, combinando
     * nombres/apellidos de ejemplo. {@code offset} debe ser distinto por institución
     * (y del rango usado por {@link #generarPadres}) para que los correos/cédulas
     * generados no colisionen entre sí.
     */
    private List<Usuario> generarEstudiantes(Institucion institucion, Rol rolEstudiante, int cantidad, int offset) {
        List<NivelAcademico> niveles = nivelAcademicoRepository
                .findByInstitucionIdAndActivoTrueOrderByGradoAscSeccionAsc(institucion.getId());
        List<Usuario> generados = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            Usuario estudiante = generarPersona(offset + i, rolEstudiante, Set.of(institucion));
            asignarSeccionFaltante(estudiante, niveles, i);
            generados.add(estudiante);
        }
        return generados;
    }

    /**
     * Asigna una sección (grado + sección) activa de la institución a un estudiante
     * que aún no tenga una, repartiéndolos de forma rotativa entre las secciones
     * disponibles. Si la institución no tiene secciones configuradas, no hace nada.
     */
    private void asignarSeccionFaltante(Usuario estudiante, List<NivelAcademico> niveles, int indice) {
        if (estudiante.getNivelAcademico() != null || niveles.isEmpty()) {
            return;
        }
        estudiante.setNivelAcademico(niveles.get(indice % niveles.size()));
        usuarioRepository.save(estudiante);
    }

    /**
     * Genera {@code cantidad} padres de relleno, sin institución asignada de entrada.
     * Su(s) institución(es) se completan luego en {@link #asignarPadresFaltantes},
     * a medida que se les vincula con estudiantes de una u otra institución.
     */
    private List<Usuario> generarPadres(Rol rolPadre, int cantidad, int offset) {
        List<Usuario> generados = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            generados.add(generarPersona(offset + i, rolPadre, new HashSet<>()));
        }
        return generados;
    }

    private Usuario generarPersona(int idx, Rol rol, Set<Institucion> instituciones) {
        String nombre = NOMBRES_PERSONAS[idx % NOMBRES_PERSONAS.length];
        String apellido1 = APELLIDOS_PERSONAS[idx % APELLIDOS_PERSONAS.length];
        String apellido2 = APELLIDOS_PERSONAS[(idx + 11) % APELLIDOS_PERSONAS.length];
        String nombreCompleto = nombre + " " + apellido1 + " " + apellido2;
        String usuarioHandle = normalizarParaHandle(nombre) + "." + normalizarParaHandle(apellido1) + idx;
        String email = usuarioHandle + "@empresa.com";
        String cedula = "9-" + String.format("%04d", idx) + "-" + String.format("%04d", 9999 - idx);

        return crearUsuarioSiNoExiste(nombreCompleto, email, usuarioHandle,
                cedula, "user1234", true, Set.of(rol), instituciones);
    }

    /**
     * Asegura que cada estudiante tenga 1 o 2 padres. Los que ya tienen al menos uno
     * (vínculos deliberados de la sección 4) se dejan intactos; al resto se le asignan
     * padres al azar (con semilla fija para que el resultado sea el mismo en cada
     * reinicio) tomados del mismo pool compartido entre instituciones.
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
                asegurarInstitucionDelPadre(padre, estudiante);
            }
        }
    }

    private void asegurarInstitucionDelPadre(Usuario padre, Usuario estudiante) {
        for (Institucion inst : estudiante.getInstituciones()) {
            if (padre.getInstituciones().stream().noneMatch(i -> i.getId().equals(inst.getId()))) {
                padre.getInstituciones().add(inst);
                usuarioRepository.save(padre);
            }
        }
    }

    private String normalizarParaHandle(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase();
    }
}
