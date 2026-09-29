package com.chavescr.nexa.exception;

/** Error al conectar con el servidor SMTP de una dirección o al enviarle un correo. */
public class CorreoEnvioException extends RuntimeException {

    public CorreoEnvioException(String message) {
        super(message);
    }

    public CorreoEnvioException(String message, Throwable cause) {
        super(message, cause);
    }
}
