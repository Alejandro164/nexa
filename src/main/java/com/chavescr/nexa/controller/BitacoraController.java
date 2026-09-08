package com.chavescr.nexa.controller;

import com.chavescr.nexa.exception.InstitucionNoSeleccionadaException;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.dto.FilaBitacora;
import com.chavescr.nexa.entity.AccionHistorial;
import com.chavescr.nexa.entity.ModuloSistema;
import com.chavescr.nexa.service.BitacoraService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/bitacora")
public class BitacoraController {

    private final BitacoraService service;

    public BitacoraController(BitacoraService service) {
        this.service = service;
    }

    @GetMapping("/panel")
    public String panel(@RequestParam(required = false) String ruta,
            @RequestParam(required = false) String alcance,
            @RequestParam(required = false) AccionHistorial accion,
            Model model, HttpSession session, HttpServletRequest request) {
        Long institucionId = requerirInstitucion(session);
        boolean institucional = "institucion".equalsIgnoreCase(alcance) && puedeVerInstitucional(request);
        ModuloSistema modulo = institucional ? null : ModuloSistema.desdeRuta(ruta);

        List<FilaBitacora> eventos = service.listar(institucionId, modulo, accion);

        model.addAttribute("eventos", eventos);
        model.addAttribute("modulo", modulo);
        model.addAttribute("ruta", ruta);
        model.addAttribute("alcance", institucional ? "institucion" : "modulo");
        model.addAttribute("accionFiltro", accion);
        model.addAttribute("puedeVerInstitucional", puedeVerInstitucional(request));
        return "bitacora/panel :: content";
    }

    private boolean puedeVerInstitucional(HttpServletRequest request) {
        return request.isUserInRole("ROLE_ADMIN") || request.isUserInRole("ROLE_DIRECTOR");
    }

    private Long requerirInstitucion(HttpSession session) {
        Long id = (Long) session.getAttribute("SESSION_INSTITUCION_ID");
        if (id == null) {
            throw new InstitucionNoSeleccionadaException();
        }
        return id;
    }
}
