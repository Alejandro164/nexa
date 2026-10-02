package com.chavescr.nexa.controller;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.exception.DireccionNoSeleccionadaException;
import com.chavescr.nexa.security.CustomUserDetails;
import com.chavescr.nexa.service.HorarioConsultaService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/horario")
public class HorarioController {

    @Autowired
    private HorarioConsultaService horarioConsultaService;

    @GetMapping
    public String index(@RequestParam(required = false) String vista,
            @RequestParam(required = false) Long periodoId,
            @RequestParam(required = false) Long usuarioId,
            @RequestHeader(value = "HX-Request", required = false) boolean htmxRequest,
            @AuthenticationPrincipal CustomUserDetails usuario, HttpSession session, Model model) {
        Long direccionId = requerirDireccion(session);
        Set<String> roles = usuario.getRoles();
        model.addAttribute("consulta", horarioConsultaService.consultar(
                direccionId, usuario.getId(), roles, vista, periodoId, usuarioId));
        return htmxRequest ? "horario/index :: htmx-content" : "horario/index";
    }

    private Long requerirDireccion(HttpSession session) {
        Long direccionId = (Long) session.getAttribute("SESSION_DIRECCION_ID");
        if (direccionId == null) {
            throw new DireccionNoSeleccionadaException();
        }
        return direccionId;
    }
}
