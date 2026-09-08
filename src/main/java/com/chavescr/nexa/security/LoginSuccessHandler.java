package com.chavescr.nexa.security;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.chavescr.nexa.entity.AccionAcceso;
import com.chavescr.nexa.entity.ResultadoAcceso;
import com.chavescr.nexa.service.RegistroAccesoService;
import com.chavescr.nexa.service.SesionInstitucionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Si el login llega por AJAX (login.html), responde JSON en vez de redirigir, para que el
 * selector de institución (cuando aplica) se muestre como modal sobre la misma página de login
 * en lugar de navegar a otra página. Si no es AJAX (JS deshabilitado), cae al redirect normal.
 */
@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final SesionInstitucionService sesionInstitucionService;
    private final RegistroAccesoService registroAccesoService;

    public LoginSuccessHandler(SesionInstitucionService sesionInstitucionService,
            RegistroAccesoService registroAccesoService) {
        this.sesionInstitucionService = sesionInstitucionService;
        this.registroAccesoService = registroAccesoService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        HttpSession session = request.getSession();
        CustomUserDetails usuario = (CustomUserDetails) authentication.getPrincipal();
        session.setAttribute("SESSION_USUARIO_ID", usuario.getId());

        String ip = RegistroAccesoService.resolverIp(request);
        registroAccesoService.registrar(usuario.getEmail(), ip, AccionAcceso.LOGIN, ResultadoAcceso.EXITOSO);
        session.setAttribute("SESSION_LOGIN_IP", ip);
        session.setAttribute("SESSION_LOGIN_USER_AGENT", request.getHeader("User-Agent"));
        session.setAttribute("SESSION_LOGIN_FECHA", LocalDateTime.now());

        boolean esAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        var resultado = sesionInstitucionService.resolver(session, esAdmin);

        if (!esAjax(request)) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter writer = response.getWriter();
        switch (resultado.estado()) {
            case RESUELTA -> writer.write("{\"redirect\":\"/inicio\"}");
            case SIN_INSTITUCIONES -> writer.write(
                    "{\"error\":\"No tienes instituciones asociadas. Contacta a soporte.\"}");
            case REQUIERE_SELECCION -> writer.write("{\"seleccionarInstitucion\":true}");
        }
        writer.flush();
    }

    private boolean esAjax(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
    }
}
