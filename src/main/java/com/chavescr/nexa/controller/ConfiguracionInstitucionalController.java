package com.chavescr.nexa.controller;

import java.time.LocalTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.entity.ComponenteEvaluacion;
import com.chavescr.nexa.entity.DestinoRebajaAsistencia;
import com.chavescr.nexa.entity.DiaLaboral;
import com.chavescr.nexa.entity.QuienRegistraPuntosBoleta;
import com.chavescr.nexa.entity.TipoMateria;
import com.chavescr.nexa.exception.DireccionNoSeleccionadaException;
import com.chavescr.nexa.service.ComponenteEvaluacionService;
import com.chavescr.nexa.service.ConfiguracionDireccionService;
import com.chavescr.nexa.service.EscalaEvaluacionService;
import com.chavescr.nexa.service.EscalaNotasService;
import com.chavescr.nexa.service.RebajaConductaService;
import com.chavescr.nexa.service.TipoMateriaService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/configuracion-institucional")
public class ConfiguracionInstitucionalController {

    @Autowired
    private ConfiguracionDireccionService service;

    @Autowired
    private ComponenteEvaluacionService componenteEvaluacionService;

    @Autowired
    private TipoMateriaService tipoMateriaService;

    @Autowired
    private EscalaNotasService escalaNotasService;

    @Autowired
    private EscalaEvaluacionService escalaEvaluacionService;

    @Autowired
    private RebajaConductaService rebajaConductaService;

    @GetMapping
    public String index(Model model, HttpServletRequest request, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        cargarJornada(model, direccionId);
        cargarComponentes(model, direccionId);
        cargarTiposMateria(model, direccionId);
        cargarEscala(model, direccionId);
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

    @GetMapping("/componentes/form")
    public String nuevoComponente(Model model, HttpSession session) {
        ComponenteEvaluacion componente = new ComponenteEvaluacion();
        componente.setCotidiano(ComponenteEvaluacion.PESOS_POR_DEFECTO[0]);
        componente.setTareas(ComponenteEvaluacion.PESOS_POR_DEFECTO[1]);
        componente.setProyecto(ComponenteEvaluacion.PESOS_POR_DEFECTO[2]);
        componente.setPruebas(ComponenteEvaluacion.PESOS_POR_DEFECTO[3]);
        componente.setAsistencia(ComponenteEvaluacion.PESOS_POR_DEFECTO[4]);
        cargarFormularioComponente(model, requerirDireccion(session), componente);
        return "configuracion-institucional/componentes/formulario :: form-content";
    }

    @GetMapping("/componentes/form/{id}")
    public String editarComponente(@PathVariable Long id, Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        cargarFormularioComponente(model, direccionId, componenteEvaluacionService.obtener(direccionId, id));
        return "configuracion-institucional/componentes/formulario :: form-content";
    }

    /** Solo se copian nombre, descripción, porcentajes y estado; dirección y orden los pone el servicio. */
    @PostMapping("/componentes")
    public String guardarComponente(@ModelAttribute ComponenteEvaluacion componente,
            Model model, HttpSession session, HttpServletResponse response) {
        Long direccionId = requerirDireccion(session);
        try {
            componenteEvaluacionService.guardar(direccionId, componente);
            response.setHeader("HX-Trigger",
                    "{\"institucionalGuardado\":{\"mensaje\":\"Componente guardado\",\"cerrarModal\":true}}");
        } catch (IllegalArgumentException e) {
            notificarError(response, e.getMessage());
        }
        cargarComponentes(model, direccionId);
        return "configuracion-institucional/componentes/componentes :: content";
    }

    @DeleteMapping("/componentes/{id}")
    public String eliminarComponente(@PathVariable Long id,
            Model model, HttpSession session, HttpServletResponse response) {
        Long direccionId = requerirDireccion(session);
        try {
            componenteEvaluacionService.eliminar(direccionId, id);
            response.setHeader("HX-Trigger",
                    "{\"institucionalGuardado\":{\"mensaje\":\"Componente eliminado\"}}");
        } catch (IllegalArgumentException e) {
            notificarError(response, e.getMessage());
        }
        cargarComponentes(model, direccionId);
        return "configuracion-institucional/componentes/componentes :: content";
    }

    @GetMapping("/tipos-materia")
    public String tiposMateria(Model model, HttpSession session) {
        cargarTiposMateria(model, requerirDireccion(session));
        return "configuracion-institucional/tipos-materia/tipos-materia :: content";
    }

    @GetMapping("/tipos-materia/form")
    public String nuevoTipoMateria(Model model) {
        model.addAttribute("tipoMateria", new TipoMateria());
        return "configuracion-institucional/tipos-materia/formulario :: form-content";
    }

    @GetMapping("/tipos-materia/form/{id}")
    public String editarTipoMateria(@PathVariable Long id, Model model, HttpSession session) {
        model.addAttribute("tipoMateria", tipoMateriaService.obtener(requerirDireccion(session), id));
        return "configuracion-institucional/tipos-materia/formulario :: form-content";
    }

    @PostMapping("/tipos-materia")
    public String guardarTipoMateria(@RequestParam(required = false) Long id, @RequestParam String nombre,
            @RequestParam(defaultValue = "false") boolean activo,
            Model model, HttpSession session, HttpServletResponse response) {
        Long direccionId = requerirDireccion(session);
        try {
            if (id == null) {
                tipoMateriaService.crear(direccionId, nombre, activo);
            } else {
                tipoMateriaService.actualizar(direccionId, id, nombre, activo);
            }
            response.setHeader("HX-Trigger",
                    "{\"institucionalGuardado\":{\"mensaje\":\"Tipo de materia guardado\",\"cerrarModal\":true}}");
        } catch (IllegalArgumentException e) {
            notificarError(response, e.getMessage());
        }
        cargarTiposMateria(model, direccionId);
        return "configuracion-institucional/tipos-materia/tipos-materia :: content";
    }

    @DeleteMapping("/tipos-materia/{id}")
    public String eliminarTipoMateria(@PathVariable Long id,
            Model model, HttpSession session, HttpServletResponse response) {
        Long direccionId = requerirDireccion(session);
        try {
            tipoMateriaService.eliminar(direccionId, id);
            response.setHeader("HX-Trigger",
                    "{\"institucionalGuardado\":{\"mensaje\":\"Tipo de materia eliminado\"}}");
        } catch (IllegalArgumentException e) {
            notificarError(response, e.getMessage());
        }
        cargarTiposMateria(model, direccionId);
        return "configuracion-institucional/tipos-materia/tipos-materia :: content";
    }

    @GetMapping("/escala-notas")
    public String escalaNotas(Model model, HttpSession session) {
        cargarEscala(model, requerirDireccion(session));
        return "configuracion-institucional/escala-notas/escala-notas :: content";
    }

    /** Guarda la escala de notas y las rebajas juntas; si una no es válida, no se guarda ninguna. */
    @PostMapping("/escala-notas")
    public String guardarEscala(
            @RequestParam Integer notaMinima,
            @RequestParam Integer notaMaxima,
            @RequestParam Integer decimales,
            @RequestParam Integer notaAprobacion,
            @RequestParam String tramos,
            @RequestParam String rebajas,
            @RequestParam QuienRegistraPuntosBoleta quienRegistraPuntosBoleta,
            @RequestParam DestinoRebajaAsistencia destinoRebajaAsistencia,
            Model model, HttpSession session, HttpServletResponse response) {
        Long direccionId = requerirDireccion(session);
        try {
            escalaEvaluacionService.guardar(direccionId, notaMinima, notaMaxima, decimales, notaAprobacion, tramos,
                    rebajas, quienRegistraPuntosBoleta, destinoRebajaAsistencia);
            response.setHeader("HX-Trigger",
                    "{\"institucionalGuardado\":{\"mensaje\":\"Escala de notas y rebajas actualizadas\"}}");
        } catch (IllegalArgumentException e) {
            notificarError(response, e.getMessage());
        }
        cargarEscala(model, direccionId);
        return "configuracion-institucional/escala-notas/escala-notas :: content";
    }

    private void cargarJornada(Model model, Long direccionId) {
        model.addAttribute("configJornada", service.obtener(direccionId));
        model.addAttribute("diasCatalogo", DiaLaboral.CATALOGO);
    }

    private void cargarComponentes(Model model, Long direccionId) {
        model.addAttribute("componentesEvaluacion", componenteEvaluacionService.listar(direccionId));
        model.addAttribute("materiasPorComponente", componenteEvaluacionService.contarMaterias(direccionId));
        model.addAttribute("asistenciaCuenta", rebajaConductaService.asistenciaRebajaComponente(direccionId));
    }

    private void cargarFormularioComponente(Model model, Long direccionId, ComponenteEvaluacion componente) {
        model.addAttribute("componente", componente);
        model.addAttribute("materiasDelComponente", componente.getId() == null ? 0L
                : componenteEvaluacionService.contarMaterias(direccionId).getOrDefault(componente.getId(), 0L));
        model.addAttribute("asistenciaCuenta", rebajaConductaService.asistenciaRebajaComponente(direccionId));
    }

    private void cargarTiposMateria(Model model, Long direccionId) {
        model.addAttribute("tiposMateria", tipoMateriaService.listar(direccionId));
    }

    private void cargarEscala(Model model, Long direccionId) {
        model.addAttribute("escalaNotas", escalaNotasService.obtener(direccionId));
        model.addAttribute("rebajaConducta", rebajaConductaService.obtener(direccionId));
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
