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

import com.chavescr.nexa.entity.Centro;
import com.chavescr.nexa.entity.OfertaEducativa;
import com.chavescr.nexa.service.CentroService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/instituciones")
public class InstitucionController {

    @Autowired
    private CentroService centroService;

    @GetMapping
    public String index(Model model, HttpServletRequest request) {
        model.addAttribute("centros", centroService.listar());
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "instituciones/index :: htmx-content";
        }
        return "instituciones/index";
    }

    @GetMapping("/lista")
    public String lista(Model model) {
        model.addAttribute("centros", centroService.listar());
        return "instituciones/lista :: tabla-instituciones";
    }

    @GetMapping("/form")
    public String showCreateForm(Model model) {
        prepararFormulario(model, new Centro(), List.of());
        return "instituciones/form :: form-content";
    }

    @GetMapping("/form/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        prepararFormulario(model, centroService.obtener(id), centroService.ofertas(id));
        return "instituciones/form :: form-content";
    }

    @PostMapping("/save")
    public String save(@RequestParam(required = false) Long centroId,
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
            centroService.guardar(centroId, cedula, nombre, direccion, telefono, email, activa != null, ofertas, codigos);
            model.addAttribute("centros", centroService.listar());
            return "instituciones/lista :: tabla-instituciones";
        } catch (IllegalArgumentException e) {
            response.setHeader("HX-Retarget", "#modal-container");
            response.setHeader("HX-Reswap", "innerHTML");
            Centro centro = centroId == null ? new Centro() : centroService.obtener(centroId);
            centro.setCedula(cedula);
            centro.setNombre(nombre);
            centro.setDireccion(direccion);
            centro.setTelefono(telefono);
            centro.setEmail(email);
            centro.setActiva(activa != null);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("centro", centro);
            model.addAttribute("ofertas", OfertaEducativa.values());
            model.addAttribute("codigos", codigos);
            model.addAttribute("elegidas", ofertas == null ? List.of() : ofertas);
            return "instituciones/form :: form-content";
        }
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable("id") Long id, Model model) {
        centroService.eliminar(id);
        model.addAttribute("centros", centroService.listar());
        return "instituciones/lista :: tabla-instituciones";
    }

    private void prepararFormulario(Model model, Centro centro, List<com.chavescr.nexa.entity.Institucion> instituciones) {
        model.addAttribute("centro", centro);
        model.addAttribute("ofertas", OfertaEducativa.values());
        model.addAttribute("elegidas", instituciones.stream()
                .filter(inst -> inst.getOferta() != null)
                .map(inst -> inst.getOferta().name())
                .toList());
        java.util.HashMap<String, String> codigos = new java.util.HashMap<>();
        for (var inst : instituciones) {
            if (inst.getOferta() != null) {
                codigos.put(inst.getOferta().name(), inst.getCodigo());
            }
        }
        model.addAttribute("codigos", codigos);
    }
}
