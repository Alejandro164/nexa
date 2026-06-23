package com.chavescr.nexa.config;

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
import com.chavescr.nexa.entity.Rol;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.InstitucionRepository;
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

    private final RolRepository rolRepository;
    private final InstitucionRepository institucionRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RolRepository rolRepository,
            InstitucionRepository institucionRepository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {
        this.rolRepository = rolRepository;
        this.institucionRepository = institucionRepository;
        this.usuarioRepository = usuarioRepository;
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

    private void crearUsuarioSiNoExiste(String nombre, String email, String usuario,
            String cedula, String rawPassword, boolean activo,
            Set<Rol> roles, Set<Institucion> instituciones) {
        if (usuarioRepository.existsByEmail(email)) {
            log.info("  [USUARIO ya existe] {}", email);
            return;
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
    }
}
