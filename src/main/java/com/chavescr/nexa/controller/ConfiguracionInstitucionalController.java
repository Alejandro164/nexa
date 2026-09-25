package com.chavescr.nexa.controller;

import java.time.LocalTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.entity.DiaLaboral;
import com.chavescr.nexa.exception.DireccionNoSeleccionadaException;
import com.chavescr.nexa.service.BloqueoLeccionService;
import com.chavescr.nexa.service.ConfiguracionDireccionService;
import com.chavescr.nexa.service.TipoComponenteService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/configuracion-institucional")
public class ConfiguracionInstitucionalController {

    @Autowired
    private ConfiguracionDireccionService service;

    @Autowired
    private BloqueoLeccionService bloqueoLeccionService;

    @Autowired
    private TipoComponenteService tipoComponenteService;

    @GetMapping
    public String index(Model model, HttpServletRequest request, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        cargarJornada(model, direccionId);
        cargarComponentes(model, direccionId);
        cargarBloqueo(model, direccionId);
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "configuracion-institucional/index :: htmx-content";
        }
        return "configuracion-institucional/index";
    }

    @GetMapping("/jornada")
    public String jornada(Model model, HttpSession session) {
        cargarJornada(model, requerirDireccion(session));
        return "configuracion-institucional/jornada/jornada :: content";
    }

    @PostMapping("/jornada")
    public String guardarJornada(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime inicioJornada,
            @RequestParam Integer minutosLeccion,
            @RequestParam(required = false) String bloquesJornada,
            @RequestParam(name = "dias", required = false) List<String> dias,
            Model model, HttpSession session, HttpServletResponse response) {
        Long direccionId = requerirDireccion(session);
        try {
            service.guardar(direccionId, inicioJornada, minutosLeccion, bloquesJornada, dias);
            cargarJornada(model, direccionId);
            response.setHeader("HX-Trigger",
                    "{\"institucionalGuardado\":{\"mensaje\":\"Jornada lectiva actualizada\"}}");
        } catch (IllegalArgumentException e) {
            cargarJornada(model, direccionId);
            notificarError(response, e.getMessage());
        }
        return "configuracion-institucional/jornada/jornada :: content";
    }

    @GetMapping("/componentes")
    public String componentes(Model model, HttpSession session) {
        cargarComponentes(model, requerirDireccion(session));
        return "configuracion-institucional/componentes/componentes :: content";
    }

    @PostMapping("/componentes")
    public String crearComponente(@RequestParam String nombre,
            @RequestParam(required = false) String emoji,
            @RequestParam(required = false) Boolean activo,
            Model model, HttpSession session, HttpServletResponse response) {
        Long direccionId = requerirDireccion(session);
        try {
            tipoComponenteService.crear(direccionId, nombre, emoji, activo);
            response.setHeader("HX-Trigger",
                    "{\"institucionalGuardado\":{\"mensaje\":\"Tipo de componente agregado\"}}");
        } catch (IllegalArgumentException e) {
            notificarError(response, e.getMessage());
        }
        cargarComponentes(model, direccionId);
        return "configuracion-institucional/componentes/componentes :: content";
    }

    @PostMapping("/componentes/{id}")
    public String actualizarComponente(@PathVariable Long id, @RequestParam String nombre,
            @RequestParam(required = false) String emoji,
            @RequestParam(required = false) Boolean activo,
            Model model, HttpSession session, HttpServletResponse response) {
        Long direccionId = requerirDireccion(session);
        try {
            tipoComponenteService.actualizar(direccionId, id, nombre, emoji, activo);
            response.setHeader("HX-Trigger",
                    "{\"institucionalGuardado\":{\"mensaje\":\"Tipo de componente actualizado\"}}");
        } catch (IllegalArgumentException e) {
            notificarError(response, e.getMessage());
        }
        cargarComponentes(model, direccionId);
        return "configuracion-institucional/componentes/componentes :: content";
    }

    @PostMapping("/componentes/{id}/eliminar")
    public String eliminarComponente(@PathVariable Long id,
            Model model, HttpSession session, HttpServletResponse response) {
        Long direccionId = requerirDireccion(session);
        try {
            tipoComponenteService.eliminar(direccionId, id);
            response.setHeader("HX-Trigger",
                    "{\"institucionalGuardado\":{\"mensaje\":\"Tipo de componente eliminado\"}}");
        } catch (IllegalArgumentException e) {
            notificarError(response, e.getMessage());
        }
        cargarComponentes(model, direccionId);
        return "configuracion-institucional/componentes/componentes :: content";
    }

    @GetMapping("/bloqueo-leccion")
    public String bloqueoLeccion(Model model, HttpSession session) {
        cargarBloqueo(model, requerirDireccion(session));
        return "configuracion-institucional/bloqueo-leccion/bloqueo-leccion :: content";
    }

    @PostMapping("/bloqueo-leccion")
    public String guardarBloqueo(@RequestParam String reglas,
            Model model, HttpSession session, HttpServletResponse response) {
        Long direccionId = requerirDireccion(session);
        try {
            bloqueoLeccionService.guardar(direccionId, reglas);
            cargarBloqueo(model, direccionId);
            response.setHeader("HX-Trigger",
                    "{\"institucionalGuardado\":{\"mensaje\":\"Bloqueos de lección actualizados\"}}");
        } catch (IllegalArgumentException e) {
            cargarBloqueo(model, direccionId);
            notificarError(response, e.getMessage());
        }
        return "configuracion-institucional/bloqueo-leccion/bloqueo-leccion :: content";
    }

    private void cargarJornada(Model model, Long direccionId) {
        model.addAttribute("configJornada", service.obtener(direccionId));
        model.addAttribute("diasCatalogo", DiaLaboral.CATALOGO);
    }

    private void cargarComponentes(Model model, Long direccionId) {
        model.addAttribute("tiposComponente", tipoComponenteService.listar(direccionId));
    }

    private void cargarBloqueo(Model model, Long direccionId) {
        model.addAttribute("bloqueoEstadoJson", bloqueoLeccionService.estadoJson(direccionId));
    }

    private Long requerirDireccion(HttpSession session) {
        Long direccionId = (Long) session.getAttribute("SESSION_DIRECCION_ID");
        if (direccionId == null) {
            throw new DireccionNoSeleccionadaException();
        }
        return direccionId;
    }

    private void notificarError(HttpServletResponse response, String mensaje) {
        response.setHeader("HX-Trigger", "{\"institucionalError\":{\"mensaje\":\"" + escaparJson(mensaje) + "\"}}");
    }

    private String escaparJson(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
