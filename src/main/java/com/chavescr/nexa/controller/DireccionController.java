package com.chavescr.nexa.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.entity.OfertaEducativa;
import com.chavescr.nexa.service.InstitucionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/direcciones")
public class DireccionController {

    @Autowired
    private InstitucionService institucionService;

    @GetMapping
    public String index(Model model, HttpServletRequest request) {
        model.addAttribute("instituciones", institucionService.listar());
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "direcciones/index :: htmx-content";
        }
        return "direcciones/index";
    }

    @GetMapping("/lista")
    public String lista(Model model) {
        model.addAttribute("instituciones", institucionService.listar());
        return "direcciones/lista :: tabla-direcciones";
    }

    @GetMapping("/form")
    public String showCreateForm(Model model) {
        prepararFormulario(model, new Institucion(), List.of());
        return "direcciones/form :: form-content";
    }

    @GetMapping("/form/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        prepararFormulario(model, institucionService.obtener(id), institucionService.ofertas(id));
        return "direcciones/form :: form-content";
    }

    @PostMapping("/save")
    public String save(@RequestParam(required = false) Long institucionId,
            @RequestParam(required = false) String cedula,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String direccion,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String activa,
            @RequestParam(required = false) List<String> ofertas,
            @RequestParam Map<String, String> codigos,
            Model model, HttpServletResponse response) {
        try {
            institucionService.guardar(institucionId, cedula, nombre, direccion, telefono, email, activa != null, ofertas, codigos);
            model.addAttribute("instituciones", institucionService.listar());
            return "direcciones/lista :: tabla-direcciones";
        } catch (IllegalArgumentException e) {
            response.setHeader("HX-Retarget", "#modal-container");
            response.setHeader("HX-Reswap", "innerHTML");
            Institucion institucion = institucionId == null ? new Institucion() : institucionService.obtener(institucionId);
            institucion.setCedula(cedula);
            institucion.setNombre(nombre);
            institucion.setDireccion(direccion);
            institucion.setTelefono(telefono);
            institucion.setEmail(email);
            institucion.setActiva(activa != null);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("institucion", institucion);
            model.addAttribute("ofertas", OfertaEducativa.values());
            model.addAttribute("codigos", codigos);
            model.addAttribute("elegidas", ofertas == null ? List.of() : ofertas);
            return "direcciones/form :: form-content";
        }
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable("id") Long id, Model model) {
        institucionService.eliminar(id);
        model.addAttribute("instituciones", institucionService.listar());
        return "direcciones/lista :: tabla-direcciones";
    }

    private void prepararFormulario(Model model, Institucion institucion, List<com.chavescr.nexa.entity.Direccion> direcciones) {
        model.addAttribute("institucion", institucion);
        model.addAttribute("ofertas", OfertaEducativa.values());
        model.addAttribute("elegidas", direcciones.stream()
                .filter(inst -> inst.getOferta() != null)
                .map(inst -> inst.getOferta().name())
                .toList());
        java.util.HashMap<String, String> codigos = new java.util.HashMap<>();
        for (var inst : direcciones) {
            if (inst.getOferta() != null) {
                codigos.put(inst.getOferta().name(), inst.getCodigo());
            }
        }
        model.addAttribute("codigos", codigos);
    }
}
