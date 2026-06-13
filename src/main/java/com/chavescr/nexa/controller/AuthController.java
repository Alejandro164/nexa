package com.chavescr.nexa.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Authentication authentication,
            CsrfToken csrfToken,
            Model model) {

        // Materializar el token antes de renderizar evita crear la sesión
        // cuando Thymeleaf ya comenzó a enviar la respuesta.
        csrfToken.getToken();

        // Si ya está autenticado, no permitir volver al login
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/";
        }

        if (error != null) {
            model.addAttribute("errorMsg", "Credenciales incorrectas. Intente con email, usuario o cédula.");
        }
        if (logout != null) {
            model.addAttribute("logoutMsg", "Sesión cerrada correctamente.");
        }
        return "auth/login";
    }
}
