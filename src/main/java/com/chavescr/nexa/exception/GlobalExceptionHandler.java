package com.chavescr.nexa.exception;

import java.io.IOException;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * La sesión perdió SESSION_INSTITUCION_ID (p. ej. expiró o el usuario nunca la eligió) a mitad
     * de navegación. En vez de dejar que la excepción llegue a la página de error genérica, se
     * redirige a "/", que ya sabe mostrar el selector de institución o auto-seleccionar la única
     * disponible.
     */
    @ExceptionHandler(InstitucionNoSeleccionadaException.class)
    public void manejarInstitucionNoSeleccionada(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if ("true".equalsIgnoreCase(request.getHeader("HX-Request"))) {
            response.setHeader("HX-Redirect", "/");
            response.setHeader("Cache-Control", "no-store");
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        response.sendRedirect("/");
    }
}
