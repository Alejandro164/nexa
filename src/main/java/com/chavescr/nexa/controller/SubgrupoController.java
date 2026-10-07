package com.chavescr.nexa.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.exception.DireccionNoSeleccionadaException;
import com.chavescr.nexa.service.SubgrupoService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/configuracion-academica/subgrupos")
public class SubgrupoController {

    @Autowired
    private SubgrupoService service;

    @GetMapping
    public String ver(@RequestParam(required = false) Long periodoId,
            @RequestParam(required = false) Long nivelId,
            @RequestParam(required = false) String bloque,
            Model model, HttpSession session) {
        model.addAttribute("vista", service.consultar(direccion(session), periodoId, nivelId, bloque));
        model.addAttribute("guardado", false);
        return "configuracion-academica/subgrupos/subgrupos :: content";
    }

    @PostMapping("/asignar")
    public String asignar(@RequestParam Long periodoId,
            @RequestParam Long nivelId,
            @RequestParam Long estudianteId,
            @RequestParam(required = false) String leccionId,
            @RequestParam(required = false) String bloque,
            Model model, HttpSession session, HttpServletResponse response) {
        Long direccionId = direccion(session);
        try {
            model.addAttribute("vista", service.asignar(
                    direccionId, periodoId, nivelId, estudianteId, id(leccionId), bloque));
            model.addAttribute("guardado", true);
        } catch (IllegalArgumentException e) {
            model.addAttribute("vista", service.consultar(direccionId, periodoId, nivelId, bloque));
            model.addAttribute("guardado", false);
            response.setHeader("HX-Trigger",
                    "{\"academicoError\":{\"mensaje\":\"" + e.getMessage().replace("\"", "") + "\"}}");
        }
        return "configuracion-academica/subgrupos/subgrupos :: content";
    }

    private Long id(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(valor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("La materia seleccionada no es válida");
        }
    }

    private Long direccion(HttpSession session) {
        Long direccionId = (Long) session.getAttribute("SESSION_DIRECCION_ID");
        if (direccionId == null) {
            throw new DireccionNoSeleccionadaException();
        }
        return direccionId;
    }
}
