package com.empresa.proyecto.config;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.empresa.proyecto.entity.Empresa;
import com.empresa.proyecto.entity.Rol;
import com.empresa.proyecto.entity.Usuario;
import com.empresa.proyecto.repository.EmpresaRepository;
import com.empresa.proyecto.repository.RolRepository;
import com.empresa.proyecto.repository.UsuarioRepository;

/**
 * Inicializa datos de ejemplo al arrancar en perfil "dev".
 * Es idempotente: verifica la existencia antes de insertar,
 * por lo que es seguro reiniciar la aplicación sin duplicar datos.
 * 
 * 
 * 
 * docker exec -i nexa_db_dev psql -U postgres -d nexa -c "
 * TRUNCATE TABLE usuario_roles, usuario_empresas, usuarios RESTART IDENTITY
 * CASCADE;
 * TRUNCATE TABLE roles RESTART IDENTITY CASCADE;
 * TRUNCATE TABLE empresas RESTART IDENTITY CASCADE;
 * "
 */
@Component
@Profile("dev")
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RolRepository rolRepository;
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RolRepository rolRepository,
            EmpresaRepository empresaRepository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {
        this.rolRepository = rolRepository;
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("=== [DataInitializer] Verificando datos iniciales ===");

        // ── 1. Roles ──────────────────────────────────────────────────────────
        Rol rolAdmin = crearRolSiNoExiste("ROLE_ADMIN");
        Rol rolEditor = crearRolSiNoExiste("ROLE_EDITOR");
        Rol rolUser = crearRolSiNoExiste("ROLE_USER");

        // ── 2. Empresas ───────────────────────────────────────────────────────
        Empresa empresaAlpha = crearEmpresaSiNoExiste("Alpha Corp S.A.", "1790012301001", "Av. Principal 100");
        Empresa empresaBeta = crearEmpresaSiNoExiste("Beta Solutions Ltda.", "1790098765001", "Calle Secundaria 200");
        Empresa empresaGamma = crearEmpresaSiNoExiste("Gamma Tech S.A.S.", "9000123456-1", "Zona Industrial 300");

        // ── 3. Usuarios ───────────────────────────────────────────────────────
        crearUsuarioSiNoExiste(
                "Alejandro Chaves", "admin@empresa.com", "admin",
                "1-2345-6789", "admin", true,
                Set.of(rolAdmin), Set.of(empresaAlpha, empresaBeta, empresaGamma));

        crearUsuarioSiNoExiste(
                "María González", "maria@empresa.com", "maria.gonzalez",
                "2-3456-7890", "editor1", true,
                Set.of(rolEditor), Set.of(empresaAlpha));

        crearUsuarioSiNoExiste(
                "Carlos López", "carlos@empresa.com", "carlos.lopez",
                "3-4567-8901", "user1234", true,
                Set.of(rolUser), Set.of(empresaBeta));

        crearUsuarioSiNoExiste(
                "Ana Rodríguez", "ana@empresa.com", "ana.rodriguez",
                "4-5678-9012", "user1234", true,
                Set.of(rolEditor, rolUser), Set.of(empresaAlpha, empresaGamma));

        crearUsuarioSiNoExiste(
                "Luis Pérez", "luis@empresa.com", "luis.perez",
                "5-6789-0123", "user1234", false, // inactivo
                Set.of(rolUser), Set.of(empresaGamma));

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

    private Empresa crearEmpresaSiNoExiste(String nombre, String cedula, String direccion) {
        return empresaRepository.findByCedula(cedula).orElseGet(() -> {
            Empresa e = new Empresa(nombre, cedula);
            e.setDireccion(direccion);
            empresaRepository.save(e);
            log.info("  [EMPRESA creada] {}", nombre);
            return e;
        });
    }

    private void crearUsuarioSiNoExiste(String nombre, String email, String usuario,
            String cedula, String rawPassword, boolean activo,
            Set<Rol> roles, Set<Empresa> empresas) {
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
        u.setEmpresas(empresas);
        usuarioRepository.save(u);
        log.info("  [USUARIO creado] {} / {} ({})", email, usuario, activo ? "activo" : "inactivo");
    }
}
