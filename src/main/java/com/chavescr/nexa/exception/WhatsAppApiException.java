package com.chavescr.nexa.exception;

/** Se lanza cuando la API de WhatsApp Business (Meta) rechaza una solicitud o falla la comunicación. */
public class WhatsAppApiException extends RuntimeException {

    public WhatsAppApiException(String message) {
        super(message);
    }

    public WhatsAppApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
