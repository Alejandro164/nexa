package com.chavescr.nexa.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;

/** Envío de correo saliente de la aplicación (ej. notificar la emisión de un Oficio con su PDF adjunto). */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${mail.from.address:noreply@nexa.local}")
    private String remitenteEmail;

    @Value("${mail.from.name:Nexa}")
    private String remitenteNombre;

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarOficioEmitido(String destinatarioEmail, String destinatarioNombre, String institucionNombre,
            String numeroOficio, String asuntoOficio, byte[] pdf, String nombreArchivoPdf)
            throws MessagingException, java.io.UnsupportedEncodingException {
        MimeMessage mensaje = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

        helper.setFrom(remitenteEmail, remitenteNombre + " (" + institucionNombre + ")");
        helper.setTo(destinatarioEmail);
        helper.setSubject("Oficio " + numeroOficio + " — " + asuntoOficio);
        helper.setText(
                "Estimado(a) " + destinatarioNombre + ",\n\n" +
                "Se le remite el oficio " + numeroOficio + " emitido por " + institucionNombre + ".\n\n" +
                "Asunto: " + asuntoOficio + "\n\n" +
                "Encontrará el documento adjunto a este correo.\n\n" +
                "Este es un mensaje automático, por favor no responda a esta dirección.",
                false);
        helper.addAttachment(nombreArchivoPdf, new ByteArrayResource(pdf));

        // El PDF es contenido binario: se fuerza codificación base64 en vez de dejar que JavaMail
        // la infiera del contenido (un PDF minúsculo/de prueba, casi todo texto ASCII, puede terminar
        // codificado como 7bit/quoted-printable, y el transporte SMTP puede alterar saltos de línea
        // en esos casos — corrompiendo el archivo). Real o de prueba, siempre se manda como binario.
        forzarCodificacionBase64(mensaje, nombreArchivoPdf);

        mailSender.send(mensaje);
        log.info("Correo de oficio emitido enviado: numero={}, destinatario={}", numeroOficio, destinatarioEmail);
    }

    private void forzarCodificacionBase64(MimeMessage mensaje, String nombreArchivoAdjunto) throws MessagingException {
        Object contenido;
        try {
            contenido = mensaje.getContent();
        } catch (Exception e) {
            throw new MessagingException("No se pudo preparar el correo: " + e.getMessage(), e);
        }
        if (!(contenido instanceof Multipart multipart)) {
            return;
        }
        for (int i = 0; i < multipart.getCount(); i++) {
            jakarta.mail.BodyPart parte = multipart.getBodyPart(i);
            if (parte instanceof MimeBodyPart mimeParte && nombreArchivoAdjunto.equals(parte.getFileName())) {
                mimeParte.setHeader("Content-Transfer-Encoding", "base64");
            }
        }
    }
}
