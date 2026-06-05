package com.empresa.proyecto.controller;

import com.empresa.proyecto.entity.NubeNodo;
import com.empresa.proyecto.service.NubeNodoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/nube-nexa")
public class NubeController {

    @Autowired
    private NubeNodoService nubeNodoService;

    // Vista Principal (Raíz)
    @GetMapping
    public String nubeNexa(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest, Model model) {
        List<NubeNodo> nodos = nubeNodoService.obtenerNodosRaiz();
        
        List<NubeNodo> carpetas = nodos.stream().filter(n -> n.getTipo().name().equals("CARPETA")).collect(Collectors.toList());
        List<NubeNodo> archivos = nodos.stream().filter(n -> n.getTipo().name().equals("ARCHIVO")).collect(Collectors.toList());
        
        model.addAttribute("carpetas", carpetas);
        model.addAttribute("archivos", archivos);
        model.addAttribute("carpetaActual", null); // Indica que estamos en la raíz
        
        return htmxRequest ? "nube/index :: htmx-content" : "nube/index";
    }

    // Vista de una Carpeta Específica
    @GetMapping("/carpeta/{id}")
    public String verCarpeta(@PathVariable Long id, Model model) {
        List<NubeNodo> nodos = nubeNodoService.obtenerNodosPorPadre(id);
        
        List<NubeNodo> carpetas = nodos.stream().filter(n -> n.getTipo().name().equals("CARPETA")).collect(Collectors.toList());
        List<NubeNodo> archivos = nodos.stream().filter(n -> n.getTipo().name().equals("ARCHIVO")).collect(Collectors.toList());
        
        NubeNodo carpetaActual = nubeNodoService.obtenerNodo(id).orElse(null);
        List<NubeNodo> breadcrumbs = nubeNodoService.obtenerRutaBreadcrumb(id);
        
        model.addAttribute("carpetas", carpetas);
        model.addAttribute("archivos", archivos);
        model.addAttribute("carpetaActual", carpetaActual);
        model.addAttribute("breadcrumbs", breadcrumbs);
        
        return "nube/index :: nube-content-area";
    }

    // Crear Nueva Carpeta
    @PostMapping("/crear-carpeta")
    public String crearCarpeta(@RequestParam String nombre, @RequestParam(required = false) Long padreId, Model model) {
        nubeNodoService.crearCarpeta(nombre, padreId);
        
        // Recargar la vista actual (padre o raíz)
        if (padreId != null) {
            return verCarpeta(padreId, model);
        } else {
            List<NubeNodo> nodos = nubeNodoService.obtenerNodosRaiz();
            model.addAttribute("carpetas", nodos.stream().filter(n -> n.getTipo().name().equals("CARPETA")).collect(Collectors.toList()));
            model.addAttribute("archivos", nodos.stream().filter(n -> n.getTipo().name().equals("ARCHIVO")).collect(Collectors.toList()));
            model.addAttribute("carpetaActual", null);
            return "nube/index :: nube-content-area";
        }
    }
}
