package com.chavescr.nexa.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.chavescr.nexa.dto.InstitucionDTO;

import jakarta.servlet.http.HttpSession;

/**
 * Resuelve SESSION_INSTITUCION_ID para una sesión: si el usuario solo tiene una institución la
 * auto-selecciona, si tiene varias intenta recordar la última que eligió, y solo si nada de eso
 * aplica pide una selección explícita. La usan tanto el login (para decidir si hay que mostrar el
 * selector) como MainController (para la carga normal de "/").
 */
@Service
public class SesionInstitucionService {

    public enum Estado { RESUELTA, SIN_INSTITUCIONES, REQUIERE_SELECCION }

    public record Resultado(Estado estado, List<InstitucionDTO> disponibles) {
        public static Resultado resuelta() {
            return new Resultado(Estado.RESUELTA, List.of());
        }
    }

    private final UsuarioService usuarioService;
    private final InstitucionService institucionService;

    public SesionInstitucionService(UsuarioService usuarioService, InstitucionService institucionService) {
        this.usuarioService = usuarioService;
        this.institucionService = institucionService;
    }

    public Resultado resolver(HttpSession session, boolean esAdmin) {
        if (session.getAttribute("SESSION_INSTITUCION_ID") != null) {
            return Resultado.resuelta();
        }

        List<InstitucionDTO> disponibles = esAdmin
                ? institucionService.obtenerTodasDTO()
                : usuarioService.obtenerInstitucionesDelUsuarioActual();

        if (disponibles.isEmpty()) {
            return new Resultado(Estado.SIN_INSTITUCIONES, disponibles);
        }
        if (disponibles.size() == 1) {
            seleccionar(disponibles.get(0), session);
            return Resultado.resuelta();
        }
        if (seleccionarRecordada(disponibles, session)) {
            return Resultado.resuelta();
        }
        return new Resultado(Estado.REQUIERE_SELECCION, disponibles);
    }

    private boolean seleccionarRecordada(List<InstitucionDTO> disponibles, HttpSession session) {
        Long ultimaId = usuarioService.obtenerUltimaInstitucionIdDelUsuarioActual();
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

    private void seleccionar(InstitucionDTO inst, HttpSession session) {
        session.setAttribute("SESSION_INSTITUCION_ID", inst.getId());
        session.setAttribute("SESSION_INSTITUCION_NOMBRE", inst.getNombre());
    }
}
