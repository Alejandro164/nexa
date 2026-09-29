package com.chavescr.nexa.service;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import com.chavescr.nexa.entity.CorreoConfiguracion.Seguridad;
import com.chavescr.nexa.exception.CorreoEnvioException;
import com.chavescr.nexa.service.CorreoConfiguracionService.CredencialesCorreo;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Envío de notificaciones por correo con el servidor SMTP de cada dirección (Configuración ›
 * Integraciones › Correo). Contraparte de {@link WhatsAppMensajeService} para correo electrónico.
 */
@Service
public class CorreoMensajeService {

    private static final Logger log = LoggerFactory.getLogger(CorreoMensajeService.class);
    private static final String TIMEOUT_MS = "15000";
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final CorreoConfiguracionService configuracionService;
    private final ITemplateEngine templateEngine;

    public CorreoMensajeService(CorreoConfiguracionService configuracionService, ITemplateEngine templateEngine) {
        this.configuracionService = configuracionService;
        this.templateEngine = templateEngine;
    }

    /** Abre y autentica la conexión con el servidor SMTP guardado, sin enviar ningún correo. */
    public void probarConexion(Long direccionId) {
        JavaMailSenderImpl sender = crearSender(configuracionService.obtenerCredenciales(direccionId));
        try {
            sender.testConnection();
        } catch (MessagingException e) {
            throw new CorreoEnvioException(mensajeDeError(e), e);
        }
        configuracionService.registrarPruebaExitosa(direccionId);
        log.info("Conexión SMTP verificada: direccionId={}, host={}", direccionId, sender.getHost());
    }

    /** Envía un correo de prueba para comprobar el envío de extremo a extremo. */
    public void enviarCorreoPrueba(Long direccionId, String destinatarioEmail) {
        CredencialesCorreo creds = configuracionService.obtenerCredenciales(direccionId);
        enviar(creds, destinatarioEmail, "Correo de prueba de Nexa", "¡La conexión funciona!",
                "Este es un correo de prueba enviado desde Nexa para verificar la configuración del servidor "
                        + "de correo de " + creds.remitenteNombre() + ".\n\nSi lo estás leyendo, las notificaciones "
                        + "por correo ya pueden enviarse.",
                null, null);
        configuracionService.registrarPruebaExitosa(direccionId);
        log.info("Correo de prueba enviado: direccionId={}, destino={}", direccionId, destinatarioEmail);
    }

    /**
     * Envía una notificación con el diseño de Nexa. Pensado para que otras funcionalidades (ausencias,
     * notas, tareas, etc.) lo reutilicen. Falla si la dirección no tiene el envío por correo activado.
     */
    public void enviarNotificacion(Long direccionId, String destinatarioEmail, String asunto, String mensaje) {
        enviarNotificacion(direccionId, destinatarioEmail, asunto, mensaje, null, null);
    }

    /** Igual que {@link #enviarNotificacion(Long, String, String, String)}, con un botón que lleva a {@code enlaceUrl}. */
    public void enviarNotificacion(Long direccionId, String destinatarioEmail, String asunto, String mensaje,
            String enlaceUrl, String enlaceTexto) {
        if (!configuracionService.estaActivo(direccionId)) {
            throw new IllegalStateException("El envío de notificaciones por correo está desactivado para esta dirección");
        }
        CredencialesCorreo creds = configuracionService.obtenerCredenciales(direccionId);
        enviar(creds, destinatarioEmail, asunto, asunto, mensaje, enlaceUrl, enlaceTexto);
    }

    private void enviar(CredencialesCorreo creds, String destinatarioEmail, String asunto, String titulo,
            String mensaje, String enlaceUrl, String enlaceTexto) {
        String destino = destinatarioEmail == null ? "" : destinatarioEmail.trim();
        if (!EMAIL.matcher(destino).matches()) {
            throw new IllegalArgumentException("Indica un correo de destino válido");
        }
        if (enlaceUrl != null && !enlaceUrl.matches("(?i)^https?://.*")) {
            throw new IllegalArgumentException("El enlace de la notificación debe empezar con http:// o https://");
        }

        Context contexto = new Context();
        contexto.setVariable("titulo", titulo);
        contexto.setVariable("mensaje", mensaje);
        contexto.setVariable("remitente", creds.remitenteNombre());
        contexto.setVariable("enlaceUrl", enlaceUrl);
        contexto.setVariable("enlaceTexto", enlaceTexto != null ? enlaceTexto : "Abrir en Nexa");
        String html = templateEngine.process("correo/notificacion", contexto);

        JavaMailSenderImpl sender = crearSender(creds);
        try {
            MimeMessage correo = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(correo, true, StandardCharsets.UTF_8.name());
            helper.setFrom(creds.remitenteEmail(), creds.remitenteNombre());
            helper.setTo(destino);
            helper.setSubject(asunto);
            // Texto plano de respaldo para clientes que no muestran HTML
            String texto = mensaje + (enlaceUrl != null ? "\n\n" + enlaceUrl : "");
            helper.setText(texto, html);
            sender.send(correo);
        } catch (MessagingException | MailException | java.io.UnsupportedEncodingException e) {
            throw new CorreoEnvioException(mensajeDeError(e), e);
        }
    }

    private JavaMailSenderImpl crearSender(CredencialesCorreo creds) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(creds.host());
        sender.setPort(creds.puerto());
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        boolean autenticar = creds.usuario() != null;
        if (autenticar) {
            sender.setUsername(creds.usuario());
            sender.setPassword(creds.password());
        }

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", String.valueOf(autenticar));
        props.put("mail.smtp.connectiontimeout", TIMEOUT_MS);
        props.put("mail.smtp.timeout", TIMEOUT_MS);
        props.put("mail.smtp.writetimeout", TIMEOUT_MS);
        if (creds.seguridad() == Seguridad.STARTTLS) {
            props.put("mail.smtp.starttls.enable", "true");
            // Si el servidor no ofrece STARTTLS se falla, en vez de mandar la contraseña en claro
            props.put("mail.smtp.starttls.required", "true");
        } else if (creds.seguridad() == Seguridad.SSL) {
            props.put("mail.smtp.ssl.enable", "true");
        }
        if (creds.seguridad() != Seguridad.NINGUNA) {
            props.put("mail.smtp.ssl.checkserveridentity", "true");
        }
        return sender;
    }

    // Traduce las excepciones de JavaMail (a veces anidadas varios niveles) a un mensaje entendible.
    private String mensajeDeError(Throwable error) {
        if (error instanceof MailAuthenticationException) {
            return "El servidor rechazó el usuario o la contraseña";
        }
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa instanceof AuthenticationFailedException) {
                return "El servidor rechazó el usuario o la contraseña";
            }
            if (causa instanceof UnknownHostException) {
                return "No se encontró el servidor SMTP \"" + causa.getMessage() + "\"";
            }
            if (causa instanceof ConnectException) {
                return "No se pudo conectar al servidor SMTP. Revisa el servidor y el puerto";
            }
            if (causa instanceof SocketTimeoutException) {
                return "El servidor SMTP no respondió a tiempo. Revisa el puerto y la seguridad";
            }
            if (causa.getMessage() != null && causa.getMessage().contains("does not support STARTTLS")) {
                return "El servidor no admite STARTTLS en ese puerto. Prueba con SSL/TLS (465) o revisa el puerto";
            }
            if (causa instanceof SSLException) {
                return "Falló la conexión segura. Revisa que la seguridad coincida con el puerto "
                        + "(STARTTLS suele ser 587 y SSL 465)";
            }
        }
        String detalle = error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
        log.warn("Error SMTP no clasificado: {}", detalle);
        return "El servidor de correo respondió con un error: " + detalle;
    }
}
