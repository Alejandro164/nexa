package com.chavescr.nexa.security;

import java.io.IOException;
import java.io.PrintWriter;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.chavescr.nexa.entity.AccionAcceso;
import com.chavescr.nexa.entity.ResultadoAcceso;
import com.chavescr.nexa.service.RegistroAccesoService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Contraparte de {@link LoginSuccessHandler}: JSON para AJAX, redirect normal si no. */
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final RegistroAccesoService registroAccesoService;

    public LoginFailureHandler(RegistroAccesoService registroAccesoService) {
        this.registroAccesoService = registroAccesoService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        registroAccesoService.registrar(request.getParameter("email"), RegistroAccesoService.resolverIp(request),
                AccionAcceso.LOGIN, ResultadoAcceso.FALLIDO);

        if (!"XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            response.sendRedirect(request.getContextPath() + "/login?error");
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter writer = response.getWriter();
        writer.write("{\"error\":\"Credenciales incorrectas. Intente con email, usuario o c\\u00e9dula.\"}");
        writer.flush();
    }
}
