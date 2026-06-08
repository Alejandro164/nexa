package com.chavescr.nexa.controller;

import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.service.InstitucionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/instituciones")
public class InstitucionController {

    @Autowired
    private InstitucionService institucionService;

    @GetMapping
    public String index(Model model, HttpServletRequest request) {
        model.addAttribute("instituciones", institucionService.findAll());
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "instituciones/index :: htmx-content";
        }
        return "instituciones/index";
    }

    @GetMapping("/lista")
    public String lista(Model model) {
        model.addAttribute("instituciones", institucionService.findAll());
        return "instituciones/lista :: tabla-instituciones";
    }

    @GetMapping("/form")
    public String showCreateForm(Model model) {
        model.addAttribute("institucion", new Institucion());
        return "instituciones/form :: form-content";
    }

    @GetMapping("/form/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Institucion institucion = institucionService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Institución no encontrada: " + id));
        model.addAttribute("institucion", institucion);
        return "instituciones/form :: form-content";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Institucion institucion, Model model) {
        institucionService.save(institucion);
        model.addAttribute("instituciones", institucionService.findAll());
        return "instituciones/lista :: tabla-instituciones";
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable("id") Long id, Model model) {
        institucionService.deleteById(id);
        model.addAttribute("instituciones", institucionService.findAll());
        return "instituciones/lista :: tabla-instituciones";
    }
}
