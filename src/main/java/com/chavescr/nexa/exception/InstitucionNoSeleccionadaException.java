package com.chavescr.nexa.exception;

/** Se lanza cuando un controlador requiere SESSION_INSTITUCION_ID pero la sesión no la tiene. */
public class InstitucionNoSeleccionadaException extends RuntimeException {

    public InstitucionNoSeleccionadaException() {
        super("No hay institución seleccionada");
    }
}
