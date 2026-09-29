package com.chavescr.nexa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.entity.CorreoConfiguracion;
import com.chavescr.nexa.entity.CorreoConfiguracion.Seguridad;
import com.chavescr.nexa.exception.DireccionNoSeleccionadaException;
import com.chavescr.nexa.service.CorreoConfiguracionService;
import com.chavescr.nexa.service.CorreoMensajeService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/** Servidor de correo para notificaciones (pestaña "Integraciones" de Configuración). */
@Controller
@RequestMapping("/configuracion/correo")
public class CorreoConfiguracionController {

    private static final String FRAGMENTO = "configuracion/correo/correo :: content";

    private final CorreoConfiguracionService configuracionService;
    private final CorreoMensajeService mensajeService;

    public CorreoConfiguracionController(CorreoConfiguracionService configuracionService,
            CorreoMensajeService mensajeService) {
        this.configuracionService = configuracionService;
        this.mensajeService = mensajeService;
    }

    @PostMapping
    public String guardar(@RequestParam(required = false) String host,
            @RequestParam(required = false) Integer puerto,
            @RequestParam(required = false) Seguridad seguridad,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) String password,
            @RequestParam(defaultValue = "false") boolean borrarPassword,
            @RequestParam(required = false) String remitenteEmail,
            @RequestParam(required = false) String remitenteNombre,
            @RequestParam(defaultValue = "false") boolean activo,
            Model model, HttpSession session, HttpServletResponse response) {
        Long direccionId = requerirDireccion(session);
        try {
            CorreoConfiguracion config = configuracionService.guardar(direccionId, host, puerto, seguridad, usuario,
                    password, borrarPassword, remitenteEmail, remitenteNombre, activo);
            model.addAttribute("correoConfig", config);
            notificarGuardado(response, "Configuración de correo guardada correctamente");
        } catch (IllegalArgumentException e) {
            model.addAttribute("correoConfig", configuracionService.obtener(direccionId));
            notificarError(response, e.getMessage());
        }
        return FRAGMENTO;
    }

    @PostMapping("/probar")
    public String probarConexion(Model model, HttpSession session, HttpServletResponse response) {
        Long direccionId = requerirDireccion(session);
        try {
            mensajeService.probarConexion(direccionId);
            notificarGuardado(response, "Conexión verificada correctamente con el servidor de correo");
        } catch (RuntimeException e) {
            notificarError(response, e.getMessage());
        }
        model.addAttribute("correoConfig", configuracionService.obtener(direccionId));
        return FRAGMENTO;
    }

    @PostMapping("/prueba-mensaje")
    public String enviarPrueba(@RequestParam String emailPrueba, Model model, HttpSession session,
            HttpServletResponse response) {
        Long direccionId = requerirDireccion(session);
        try {
            mensajeService.enviarCorreoPrueba(direccionId, emailPrueba);
            notificarGuardado(response, "Correo de prueba enviado a " + emailPrueba.trim());
        } catch (RuntimeException e) {
            notificarError(response, e.getMessage());
        }
        model.addAttribute("correoConfig", configuracionService.obtener(direccionId));
        return FRAGMENTO;
    }

    private Long requerirDireccion(HttpSession session) {
        Long direccionId = (Long) session.getAttribute("SESSION_DIRECCION_ID");
        if (direccionId == null) {
            throw new DireccionNoSeleccionadaException();
        }
        return direccionId;
    }

    private void notificarGuardado(HttpServletResponse response, String mensaje) {
        response.setHeader("HX-Trigger", "{\"correoGuardado\":{\"mensaje\":\"" + escaparJson(mensaje) + "\"}}");
    }

    private void notificarError(HttpServletResponse response, String mensaje) {
        response.setHeader("HX-Trigger", "{\"correoError\":{\"mensaje\":\"" + escaparJson(mensaje) + "\"}}");
    }

    private String escaparJson(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("\\", "\\\\").replace("\"", "\\\"").replaceAll("[\\r\\n]+", " ");
    }
}
