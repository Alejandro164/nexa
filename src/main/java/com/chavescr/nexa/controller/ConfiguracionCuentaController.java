package com.chavescr.nexa.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.entity.PreferenciaUsuario;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.security.CustomUserDetails;
import com.chavescr.nexa.service.PreferenciaUsuarioService;
import com.chavescr.nexa.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Preferencias personales de la cuenta (no confundir con /configuracion, la Configuración Institucional). */
@Controller
@RequestMapping("/configuracion-cuenta")
public class ConfiguracionCuentaController {

    private final UsuarioService usuarioService;
    private final PreferenciaUsuarioService preferenciaUsuarioService;

    public ConfiguracionCuentaController(UsuarioService usuarioService,
            PreferenciaUsuarioService preferenciaUsuarioService) {
        this.usuarioService = usuarioService;
        this.preferenciaUsuarioService = preferenciaUsuarioService;
    }

    @GetMapping
    public String configuracionCuenta(@AuthenticationPrincipal CustomUserDetails principal, Model model,
            HttpServletRequest request) {
        model.addAttribute("preferencia", preferenciaUsuarioService.obtenerPorUsuario(principal.getId()));
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "configuracion-cuenta/index :: htmx-content";
        }
        return "configuracion-cuenta/index";
    }

    @PostMapping
    public String guardar(@AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(defaultValue = "false") boolean notifEmail,
            @RequestParam(defaultValue = "false") boolean notifPush,
            @RequestParam(defaultValue = "false") boolean notifRecordatorios,
            @RequestParam(defaultValue = "false") boolean notifResumenSemanal,
            @RequestParam String idioma, @RequestParam String zonaHoraria, @RequestParam String tema,
            Model model, HttpServletResponse response) {
        PreferenciaUsuario preferencia = preferenciaUsuarioService.guardar(principal.getId(), notifEmail, notifPush,
                notifRecordatorios, notifResumenSemanal, idioma, zonaHoraria, tema);
        model.addAttribute("preferencia", preferencia);
        response.setHeader("HX-Trigger", "{\"perfilGuardado\":{\"mensaje\":\"Preferencias guardadas correctamente\"}}");
        return "configuracion-cuenta/index :: content";
    }

    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(@AuthenticationPrincipal CustomUserDetails principal) {
        Usuario usuario = usuarioService.obtenerUsuarioActual();
        PreferenciaUsuario preferencia = preferenciaUsuarioService.obtenerPorUsuario(principal.getId());

        StringBuilder texto = new StringBuilder();
        texto.append("Exportación de datos personales — Nexa\n");
        texto.append("Generado: ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .append("\n\n");
        texto.append("Nombre completo: ").append(usuario.getNombre()).append("\n");
        texto.append("Correo electrónico: ").append(usuario.getEmail()).append("\n");
        texto.append("Nombre de usuario: ").append(usuario.getUsuario()).append("\n");
        texto.append("Cédula: ").append(usuario.getCedula() != null ? usuario.getCedula() : "No especificada")
                .append("\n");
        texto.append("Teléfono: ").append(usuario.getTelefono() != null ? usuario.getTelefono() : "No especificado")
                .append("\n\n");

        texto.append("Instituciones asociadas:\n");
        for (Institucion inst : usuario.getInstituciones()) {
            texto.append(" - ").append(inst.getNombre()).append("\n");
        }

        texto.append("\nPreferencias de cuenta:\n");
        texto.append(" - Notificaciones por correo: ").append(preferencia.getNotifEmail() ? "Sí" : "No").append("\n");
        texto.append(" - Notificaciones push: ").append(preferencia.getNotifPush() ? "Sí" : "No").append("\n");
        texto.append(" - Recordatorios de tareas: ").append(preferencia.getNotifRecordatorios() ? "Sí" : "No")
                .append("\n");
        texto.append(" - Resumen semanal: ").append(preferencia.getNotifResumenSemanal() ? "Sí" : "No").append("\n");
        texto.append(" - Idioma: ").append(preferencia.getIdioma()).append("\n");
        texto.append(" - Zona horaria: ").append(preferencia.getZonaHoraria()).append("\n");
        texto.append(" - Apariencia: ").append(preferencia.getTema()).append("\n");

        byte[] contenido = texto.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mis-datos-nexa.txt\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(contenido);
    }
}
