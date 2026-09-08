package com.chavescr.nexa.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.security.CustomUserDetails;
import com.chavescr.nexa.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/perfil")
public class PerfilController {

    private final UsuarioService usuarioService;

    public PerfilController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String perfil(Model model, HttpServletRequest request, HttpSession session) {
        cargarModelo(model, session);
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "perfil/index :: htmx-content";
        }
        return "perfil/index";
    }

    @GetMapping("/editar-modal")
    public String editarModal(Model model) {
        model.addAttribute("usuario", usuarioService.obtenerUsuarioActual());
        return "perfil/editar-modal :: modal-content";
    }

    @PostMapping
    public String guardarDatosPersonales(@AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam String nombre, @RequestParam(required = false) String telefono,
            Model model, HttpSession session, HttpServletResponse response) {
        try {
            usuarioService.actualizarDatosPersonales(principal.getId(), nombre, telefono);
        } catch (Exception e) {
            response.setHeader("HX-Retarget", "#modal-container");
            response.setHeader("HX-Reswap", "innerHTML");
            model.addAttribute("error", e.getMessage());
            model.addAttribute("usuario", usuarioService.obtenerUsuarioActual());
            return "perfil/editar-modal :: modal-content";
        }
        cargarModelo(model, session);
        response.setHeader("HX-Trigger", "{\"perfilGuardado\":{\"mensaje\":\"Perfil actualizado correctamente\"}}");
        return "perfil/index :: content";
    }

    @GetMapping("/cambiar-password-modal")
    public String cambiarPasswordModal() {
        return "perfil/cambiar-password-modal :: modal-content";
    }

    @PostMapping("/cambiar-password")
    public String cambiarPassword(@AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam String passwordActual, @RequestParam String passwordNueva,
            @RequestParam String passwordConfirmar, Model model, HttpServletResponse response) {
        try {
            if (!passwordNueva.equals(passwordConfirmar)) {
                throw new IllegalArgumentException("La confirmación no coincide con la nueva contraseña");
            }
            usuarioService.cambiarPassword(principal.getId(), passwordActual, passwordNueva);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "perfil/cambiar-password-modal :: modal-content";
        }
        response.setHeader("HX-Trigger", "{\"perfilGuardado\":{\"mensaje\":\"Contraseña actualizada correctamente\"}}");
        return "perfil/cambiar-password-modal :: modal-content";
    }

    private void cargarModelo(Model model, HttpSession session) {
        Usuario usuario = usuarioService.obtenerUsuarioActual();
        model.addAttribute("usuario", usuario);
        model.addAttribute("institucionActualId", session.getAttribute("SESSION_INSTITUCION_ID"));
        model.addAttribute("institucionActualNombre", session.getAttribute("SESSION_INSTITUCION_NOMBRE"));
    }
}
