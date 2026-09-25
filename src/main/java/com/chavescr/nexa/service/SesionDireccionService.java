package com.chavescr.nexa.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.chavescr.nexa.dto.DireccionDTO;

import jakarta.servlet.http.HttpSession;

/**
 * Resuelve SESSION_DIRECCION_ID para una sesión: si el usuario solo tiene una dirección la
 * auto-selecciona, si tiene varias intenta recordar la última que eligió, y solo si nada de eso
 * aplica pide una selección explícita. La usan tanto el login (para decidir si hay que mostrar el
 * selector) como MainController (para la carga normal de "/").
 */
@Service
public class SesionDireccionService {

    public enum Estado { RESUELTA, SIN_DIRECCIONES, REQUIERE_SELECCION }

    public record Resultado(Estado estado, List<DireccionDTO> disponibles) {
        public static Resultado resuelta() {
            return new Resultado(Estado.RESUELTA, List.of());
        }
    }

    private final UsuarioService usuarioService;
    private final DireccionService direccionService;

    public SesionDireccionService(UsuarioService usuarioService, DireccionService direccionService) {
        this.usuarioService = usuarioService;
        this.direccionService = direccionService;
    }

    public Resultado resolver(HttpSession session, boolean esAdmin) {
        if (session.getAttribute("SESSION_DIRECCION_ID") != null) {
            return Resultado.resuelta();
        }

        List<DireccionDTO> disponibles = esAdmin
                ? direccionService.obtenerTodasDTO()
                : usuarioService.obtenerDireccionesDelUsuarioActual();

        if (disponibles.size() == 1) {
            seleccionar(disponibles.get(0), session);
            return Resultado.resuelta();
        }
        if (seleccionarRecordada(disponibles, session)) {
            return Resultado.resuelta();
        }
        if (esAdmin) {
            // ROLE_ADMIN puede operar sin dirección seleccionada (ve solo Inicio + Administración).
            return Resultado.resuelta();
        }
        if (disponibles.isEmpty()) {
            return new Resultado(Estado.SIN_DIRECCIONES, disponibles);
        }
        return new Resultado(Estado.REQUIERE_SELECCION, disponibles);
    }

    private boolean seleccionarRecordada(List<DireccionDTO> disponibles, HttpSession session) {
        Long ultimaId = usuarioService.obtenerUltimaDireccionIdDelUsuarioActual();
        if (ultimaId == null) {
            return false;
        }
        return disponibles.stream()
                .filter(inst -> inst.getId().equals(ultimaId))
                .findFirst()
                .map(inst -> {
                    seleccionar(inst, session);
                    return true;
                })
                .orElse(false);
    }

    private void seleccionar(DireccionDTO inst, HttpSession session) {
        session.setAttribute("SESSION_DIRECCION_ID", inst.getId());
        session.setAttribute("SESSION_DIRECCION_NOMBRE", inst.getNombre());
    }
}
