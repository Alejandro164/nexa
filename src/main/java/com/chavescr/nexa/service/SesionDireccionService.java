package com.chavescr.nexa.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.dto.DireccionDTO;
import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.OfertaEducativa;

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

    /** Una dirección que el usuario puede elegir desde el menú lateral. */
    public record OpcionDireccion(Long id, String texto, String titulo, boolean actual) {
    }

    /**
     * Direcciones de la institución activa. El menú lateral solo se muestra cuando hay más de una:
     * con una sola no hay nada que cambiar.
     */
    public record MenuCambioDireccion(List<OpcionDireccion> opciones) {
        public boolean isVisible() {
            return opciones.size() > 1;
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

    /**
     * Direcciones de la misma institución que la activa. No incluye otras instituciones: cambiar
     * de institución se hace desde "Cambiar cuenta". Con una sola dirección el menú no se muestra.
     */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public MenuCambioDireccion menu(boolean esAdmin, Long actualId) {
        if (actualId == null) {
            return oculto();
        }
        List<Direccion> propias = esAdmin
                ? List.of()
                : usuarioService.listarDireccionesActivasDelUsuarioActual();
        Direccion actual = esAdmin
                ? direccionService.findByIdConInstitucion(actualId).orElse(null)
                : propias.stream().filter(d -> actualId.equals(d.getId())).findFirst().orElse(null);
        if (actual == null || actual.getInstitucion() == null || actual.getInstitucion().getId() == null) {
            return oculto();
        }
        Long institucionId = actual.getInstitucion().getId();
        List<Direccion> hermanas = new ArrayList<>(esAdmin
                ? direccionService.listarActivasPorInstitucion(institucionId)
                : propias.stream()
                        .filter(d -> d.getInstitucion() != null
                                && institucionId.equals(d.getInstitucion().getId()))
                        .toList());
        if (hermanas.stream().noneMatch(d -> actualId.equals(d.getId()))) {
            hermanas.add(actual);
        }
        if (hermanas.size() < 2) {
            return oculto();
        }
        ordenar(hermanas);
        return new MenuCambioDireccion(hermanas.stream().map(d -> opcion(d, actualId, true)).toList());
    }

    private static MenuCambioDireccion oculto() {
        return new MenuCambioDireccion(List.of());
    }

    /**
     * Cambia la dirección de la sesión solo si el usuario puede usarla. Los dos atributos de
     * sesión se escriben juntos, después de validar, para no dejar un id sin nombre o al revés.
     * Devuelve false si la dirección no existe o no le pertenece: la sesión queda intacta.
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean cambiar(HttpSession session, Long usuarioId, Long direccionId, boolean esAdmin) {
        if (direccionId == null) {
            return false;
        }
        if (esAdmin) {
            return direccionService.findByIdConInstitucion(direccionId)
                    .map(inst -> aplicar(session, usuarioId, inst))
                    .orElse(false);
        }
        return usuarioService.obtenerDireccionesDelUsuarioActual().stream()
                .filter(inst -> direccionId.equals(inst.getId()))
                .findFirst()
                .map(inst -> {
                    if (usuarioId != null) {
                        direccionService.findByIdConInstitucion(inst.getId())
                                .ifPresent(entidad -> usuarioService.actualizarUltimaDireccion(usuarioId, entidad));
                    }
                    session.setAttribute("SESSION_DIRECCION_ID", inst.getId());
                    session.setAttribute("SESSION_DIRECCION_NOMBRE", inst.getNombre());
                    return true;
                })
                .orElse(false);
    }

    /** Quita la dirección activa. Solo debe llamarse tras comprobar ROLE_ADMIN. */
    public void salir(HttpSession session, Long usuarioId) {
        session.removeAttribute("SESSION_DIRECCION_ID");
        session.removeAttribute("SESSION_DIRECCION_NOMBRE");
        if (usuarioId != null) {
            usuarioService.actualizarUltimaDireccion(usuarioId, null);
        }
    }

    private static void ordenar(List<Direccion> direcciones) {
        direcciones.sort(Comparator
                .comparingInt((Direccion d) -> d.getOferta() == null ? Integer.MAX_VALUE : d.getOferta().ordinal())
                .thenComparing(d -> d.getPresentacion() == null ? "" : d.getPresentacion(),
                        String.CASE_INSENSITIVE_ORDER));
    }

    private static OpcionDireccion opcion(Direccion direccion, Long actualId, boolean mismaInstitucion) {
        String titulo = direccion.getPresentacion() == null ? "" : direccion.getPresentacion();
        OfertaEducativa oferta = direccion.getOferta();
        String texto = mismaInstitucion && oferta != null ? oferta.getEtiqueta() : titulo;
        if (texto == null || texto.isBlank()) {
            texto = titulo.isBlank() ? "Dirección" : titulo;
        }
        return new OpcionDireccion(direccion.getId(), texto, titulo, actualId != null && actualId.equals(direccion.getId()));
    }

    private boolean aplicar(HttpSession session, Long usuarioId, Direccion inst) {
        String nombre = inst.getPresentacion();
        if (usuarioId != null) {
            usuarioService.actualizarUltimaDireccion(usuarioId, inst);
        }
        session.setAttribute("SESSION_DIRECCION_ID", inst.getId());
        session.setAttribute("SESSION_DIRECCION_NOMBRE", nombre);
        return true;
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
