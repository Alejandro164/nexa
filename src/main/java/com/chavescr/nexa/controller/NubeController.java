package com.chavescr.nexa.controller;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.chavescr.nexa.entity.NubeNodo;
import com.chavescr.nexa.service.NubeNodoService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/nube-nexa")
public class NubeController {

    private final NubeNodoService nubeNodoService;

    public NubeController(NubeNodoService nubeNodoService) {
        this.nubeNodoService = nubeNodoService;
    }

    // Vista Principal (Raíz)
    @GetMapping
    public String nubeNexa(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest, Model model) {
        List<NubeNodo> nodos = nubeNodoService.obtenerNodosRaiz();

        List<NubeNodo> carpetas = nodos.stream().filter(n -> n.getTipo().name().equals("CARPETA"))
                .collect(Collectors.toList());
        List<NubeNodo> archivos = nodos.stream().filter(n -> n.getTipo().name().equals("ARCHIVO"))
                .collect(Collectors.toList());

        model.addAttribute("carpetas", carpetas);
        model.addAttribute("archivos", archivos);
        model.addAttribute("carpetaActual", null); // Indica que estamos en la raíz

        return htmxRequest ? "nube/index :: htmx-content" : "nube/index";
    }

    // Vista de una Carpeta Específica
    @GetMapping("/carpeta/{id}")
    public String verCarpeta(@PathVariable Long id, Model model) {
        List<NubeNodo> nodos = nubeNodoService.obtenerNodosPorPadre(id);

        List<NubeNodo> carpetas = nodos.stream().filter(n -> n.getTipo().name().equals("CARPETA"))
                .collect(Collectors.toList());
        List<NubeNodo> archivos = nodos.stream().filter(n -> n.getTipo().name().equals("ARCHIVO"))
                .collect(Collectors.toList());

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
            model.addAttribute("carpetas",
                    nodos.stream().filter(n -> n.getTipo().name().equals("CARPETA")).collect(Collectors.toList()));
            model.addAttribute("archivos",
                    nodos.stream().filter(n -> n.getTipo().name().equals("ARCHIVO")).collect(Collectors.toList()));
            model.addAttribute("carpetaActual", null);
            return "nube/index :: nube-content-area";
        }
    }

    // Subir Archivo
    @PostMapping("/subir-archivo")
    public String subirArchivo(@RequestParam("archivo") MultipartFile archivo,
            @RequestParam(required = false) Long padreId,
            Model model,
            HttpSession session) {
        Long institucionId = (Long) session.getAttribute("SESSION_INSTITUCION_ID");
        if (institucionId == null) {
            throw new IllegalStateException("Debe seleccionar una institución");
        }
        try {
            nubeNodoService.subirArchivo(archivo, padreId, institucionId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (padreId != null) {
            return verCarpeta(padreId, model);
        } else {
            List<NubeNodo> nodos = nubeNodoService.obtenerNodosRaiz();
            model.addAttribute("carpetas",
                    nodos.stream().filter(n -> n.getTipo().name().equals("CARPETA")).collect(Collectors.toList()));
            model.addAttribute("archivos",
                    nodos.stream().filter(n -> n.getTipo().name().equals("ARCHIVO")).collect(Collectors.toList()));
            model.addAttribute("carpetaActual", null);
            return "nube/index :: nube-content-area";
        }
    }

    // Descargar Archivo
    @GetMapping("/descargar/{id}")
    public ResponseEntity<Resource> descargarArchivo(@PathVariable Long id) {
        NubeNodo archivo = nubeNodoService.obtenerNodo(id)
                .orElseThrow(() -> new IllegalArgumentException("Archivo no encontrado"));

        if (!archivo.getTipo().name().equals("ARCHIVO")) {
            throw new IllegalArgumentException("El nodo no es un archivo");
        }

        try {
            Path filePath = Paths.get(nubeNodoService.getRutaRecursos()).resolve(archivo.getUrlArchivo());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo.getNombre() + "\"")
                        .body(resource);
            } else {
                throw new RuntimeException("No se puede leer el archivo");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al descargar el archivo: " + e.getMessage());
        }
    }

    // Descargar Carpeta como ZIP
    @GetMapping("/descargar-carpeta/{id}")
    public ResponseEntity<StreamingResponseBody> descargarCarpeta(@PathVariable Long id) {
        NubeNodo carpeta = nubeNodoService.obtenerNodo(id)
                .orElseThrow(() -> new IllegalArgumentException("Carpeta no encontrada"));

        if (!carpeta.getTipo().name().equals("CARPETA")) {
            throw new IllegalArgumentException("El nodo no es una carpeta");
        }

        StreamingResponseBody stream = out -> {
            try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(out)) {
                agregarNodoAZip(carpeta, carpeta.getNombre() + "/", zos);
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + carpeta.getNombre() + ".zip\"")
                .contentType(org.springframework.http.MediaType.parseMediaType("application/zip"))
                .body(stream);
    }

    private void agregarNodoAZip(NubeNodo nodo, String pathActual, java.util.zip.ZipOutputStream zos)
            throws java.io.IOException {
        if (nodo.getTipo().name().equals("ARCHIVO")) {
            Path filePath = Paths.get(nubeNodoService.getRutaRecursos()).resolve(nodo.getUrlArchivo());
            if (java.nio.file.Files.exists(filePath)) {
                zos.putNextEntry(new java.util.zip.ZipEntry(pathActual));
                java.nio.file.Files.copy(filePath, zos);
                zos.closeEntry();
            }
        } else if (nodo.getTipo().name().equals("CARPETA")) {
            List<NubeNodo> hijos = nubeNodoService.obtenerNodosPorPadre(nodo.getId());
            if (hijos == null || hijos.isEmpty()) {
                zos.putNextEntry(new java.util.zip.ZipEntry(pathActual));
                zos.closeEntry();
            } else {
                for (NubeNodo hijo : hijos) {
                    String hijoPath = pathActual + hijo.getNombre()
                            + (hijo.getTipo().name().equals("CARPETA") ? "/" : "");
                    agregarNodoAZip(hijo, hijoPath, zos);
                }
            }
        }
    }

    // Renombrar Archivo o Carpeta
    @PostMapping("/renombrar")
    public String renombrar(@RequestParam Long id,
            @RequestParam String nuevoNombre,
            @RequestParam(required = false) Long padreId,
            Model model) {
        nubeNodoService.renombrarNodo(id, nuevoNombre);

        if (padreId != null) {
            return verCarpeta(padreId, model);
        } else {
            return nubeNexa(false, model).replace("nube/index", "nube/index :: nube-content-area");
        }
    }

    // Eliminar Archivo o Carpeta
    @PostMapping("/eliminar")
    public String eliminar(@RequestParam Long id,
            @RequestParam(required = false) Long padreId,
            Model model) {
        nubeNodoService.eliminarNodo(id);

        if (padreId != null) {
            return verCarpeta(padreId, model);
        } else {
            return nubeNexa(false, model).replace("nube/index", "nube/index :: nube-content-area");
        }
    }
}
