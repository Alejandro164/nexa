package com.chavescr.nexa.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Separado de {@link SecurityConfig} para evitar un ciclo: servicios como UsuarioService
 * necesitan PasswordEncoder, y SecurityConfig depende (transitivamente, vía LoginSuccessHandler
 * y SesionInstitucionService) de esos mismos servicios.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
