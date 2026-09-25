package com.chavescr.nexa.exception;

/** Se lanza cuando un controlador requiere SESSION_DIRECCION_ID pero la sesión no la tiene. */
public class DireccionNoSeleccionadaException extends RuntimeException {

    public DireccionNoSeleccionadaException() {
        super("No hay dirección seleccionada");
    }
}
