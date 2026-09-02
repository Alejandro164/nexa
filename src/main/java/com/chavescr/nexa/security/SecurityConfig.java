package com.chavescr.nexa.security;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import com.chavescr.nexa.entity.AccionAcceso;
import com.chavescr.nexa.entity.ResultadoAcceso;
import com.chavescr.nexa.service.RegistroAccesoService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Los authorities ya incluyen el prefijo "ROLE_" (ver Usuario/CustomUserDetails), por eso se usa
    // hasAuthority/hasAnyAuthority en vez de hasRole (que agregaría el prefijo por segunda vez).
    private static final String ADMIN = "ROLE_ADMIN";
    private static final String DIRECTOR = "ROLE_DIRECTOR";
    private static final String DOCENTE = "ROLE_DOCENTE";
    private static final String PADRE = "ROLE_PADRE";
    private static final String ESTUDIANTE = "ROLE_ESTUDIANTE";

    private final UserDetailsServiceImpl userDetailsService;
    private final LoginSuccessHandler loginSuccessHandler;
    private final LoginFailureHandler loginFailureHandler;
    private final PasswordEncoder passwordEncoder;
    private final RegistroAccesoService registroAccesoService;

    public SecurityConfig(UserDetailsServiceImpl userDetailsService, LoginSuccessHandler loginSuccessHandler,
            LoginFailureHandler loginFailureHandler, PasswordEncoder passwordEncoder,
            RegistroAccesoService registroAccesoService) {
        this.userDetailsService = userDetailsService;
        this.loginSuccessHandler = loginSuccessHandler;
        this.loginFailureHandler = loginFailureHandler;
        this.passwordEncoder = passwordEncoder;
        this.registroAccesoService = registroAccesoService;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        // Spring Security 6: UserDetailsService va en el constructor
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Cache de solicitudes: ignorar /login para que no redirija al login
        // tras hacer logout y volver con el botón atrás
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        requestCache.setMatchingRequestParameterName(null);

        http
                .authenticationProvider(authenticationProvider())
                .requestCache(cache -> cache.requestCache(requestCache))
                // Headers: impedir que el browser cachee páginas protegidas
                .headers(headers -> headers
                        .cacheControl(cache -> {
                        }) // emite Cache-Control: no-cache, no-store, must-revalidate
                        .frameOptions(frame -> frame.sameOrigin()))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> redirectToLogin(request, response))
                        .accessDeniedHandler((request, response, exception) -> handleAccessDenied(request, response)))
                .sessionManagement(session -> session
                        .sessionFixation().migrateSession()
                        .maximumSessions(1))

                .authorizeHttpRequests(auth -> auth
                        // Recursos públicos
                        .requestMatchers("/login", "/css/**", "/js/**", "/images/**").permitAll()

                        // ── Administración: solo ADMIN, salvo Config. Académica (también DIRECTOR) ──
                        .requestMatchers("/configuracion-academica/**").hasAnyAuthority(ADMIN, DIRECTOR)
                        .requestMatchers("/configuracion/**", "/usuarios/**", "/seguridad", "/instituciones/**",
                                "/componentes").hasAuthority(ADMIN)

                        // ── Personal: Solicitudes de Padres también la ve DOCENTE; el resto no ──
                        .requestMatchers("/personal/solicitudes/**").hasAnyAuthority(ADMIN, DIRECTOR, DOCENTE)
                        .requestMatchers("/personal/**").hasAnyAuthority(ADMIN, DIRECTOR)

                        // ── Agenda: calendario abierto a todos; crear actividad ya reforzado a DIRECTOR/ADMIN;
                        //    el resto (tareas, recordatorios, proyectos, participación) es herramienta interna de staff ──
                        .requestMatchers(HttpMethod.GET, "/agenda", "/agenda/calendario/**")
                        .hasAnyAuthority(ADMIN, DIRECTOR, DOCENTE, PADRE, ESTUDIANTE)
                        .requestMatchers("/agenda/actividad/**").hasAnyAuthority(ADMIN, DIRECTOR)
                        .requestMatchers("/agenda/**").hasAnyAuthority(ADMIN, DIRECTOR, DOCENTE)

                        // ── Portal Padres: ya reforzado en el controller, se agrega también aquí ──
                        .requestMatchers("/portal-padres/**").hasAnyAuthority(ADMIN, PADRE)

                        // ── Estudiantes / Padres: lectura para staff, edición solo ADMIN/DIRECTOR ──
                        .requestMatchers(HttpMethod.GET, "/estudiantes/**", "/padres/**")
                        .hasAnyAuthority(ADMIN, DIRECTOR, DOCENTE)
                        .requestMatchers("/estudiantes/**", "/padres/**").hasAnyAuthority(ADMIN, DIRECTOR)

                        // ── Resto de módulos operativos: abiertos a todo el staff (ADMIN/DIRECTOR/DOCENTE) ──
                        .requestMatchers("/docentes/**", "/control-de-acceso/**", "/gestion-academica/**",
                                "/gestion-especial/**", "/evaluacion-academica/**", "/conducta/**", "/comedor/**",
                                "/reportes/**", "/contacto/**", "/mis-cursos/**", "/nube-nexa/**", "/archivos/**")
                        .hasAnyAuthority(ADMIN, DIRECTOR, DOCENTE)

                        // ── Solo gestión (ADMIN/DIRECTOR), no docente ──
                        .requestMatchers("/coordinacion-academica/**", "/archivo-graduados/**", "/oficios/**")
                        .hasAnyAuthority(ADMIN, DIRECTOR)

                        // ── Comunicación: visible para todos los roles (declarado explícito) ──
                        .requestMatchers("/comunicacion/**")
                        .hasAnyAuthority(ADMIN, DIRECTOR, DOCENTE, PADRE, ESTUDIANTE)

                        // Todo lo demás (inicio, perfil, configuración de cuenta, login, logout, etc.)
                        // requiere solo estar autenticado, sin distinción de rol
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login") // Nuestra página de login personalizada
                        .loginProcessingUrl("/login") // Spring Security procesa el POST aquí
                        .successHandler(loginSuccessHandler) // JSON si es AJAX, redirect si no
                        .failureHandler(loginFailureHandler)
                        .usernameParameter("email") // Campo del formulario (acepta email/usuario/cédula)
                        .passwordParameter("password")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        // LogoutHandler (no LogoutSuccessHandler): corre ANTES de invalidar la sesión,
                        // así todavía hay Authentication disponible para saber quién cerró sesión.
                        .addLogoutHandler((request, response, authentication) -> {
                            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails usuario) {
                                registroAccesoService.registrar(usuario.getEmail(),
                                        RegistroAccesoService.resolverIp(request), AccionAcceso.LOGOUT,
                                        ResultadoAcceso.EXITOSO);
                            }
                        })
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID", "SESSION", "XSRF-TOKEN")
                        .permitAll());

        return http.build();
    }

    private static void redirectToLogin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String loginUrl = request.getContextPath() + "/login?expired";

        if ("true".equalsIgnoreCase(request.getHeader("HX-Request"))) {
            response.setHeader("HX-Redirect", loginUrl);
            response.setHeader("Cache-Control", "no-store");
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        response.sendRedirect(loginUrl);
    }

    private static void handleAccessDenied(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // En navegación htmx un 403 normal no se puede "mostrar" dentro del fragmento que se esperaba
        // reemplazar, así que se fuerza una recarga completa a /inicio (igual que redirectToLogin).
        if ("true".equalsIgnoreCase(request.getHeader("HX-Request"))) {
            response.setHeader("HX-Redirect", request.getContextPath() + "/inicio");
            response.setHeader("Cache-Control", "no-store");
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
}
