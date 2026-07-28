package com.chavescr.nexa.controller;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.chavescr.nexa.entity.NubeNodo;
import com.chavescr.nexa.service.NubeNodoService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/nube-nexa")
public class NubeController {

    @Autowired
    private NubeNodoService nubeNodoService;

    // Vista Principal (Raíz)
    @GetMapping
    public String nubeNexa(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest,
            @RequestHeader(value = "HX-Target", required = false) String hxTarget, Model model) {
        List<NubeNodo> nodos = nubeNodoService.obtenerNodosRaiz();

        List<NubeNodo> carpetas = nodos.stream().filter(n -> n.getTipo().name().equals("CARPETA"))
                .collect(Collectors.toList());
        List<NubeNodo> archivos = nodos.stream().filter(n -> n.getTipo().name().equals("ARCHIVO"))
                .collect(Collectors.toList());

        model.addAttribute("carpetas", carpetas);
        model.addAttribute("archivos", archivos);
        model.addAttribute("carpetaActual", null); // Indica que estamos en la raíz

        // Navegación interna del módulo (breadcrumb "Mi Unidad") apunta a #nube-content-area:
        // debe recibir solo ese fragmento, no la página completa del módulo (que duplicaría
        // #nube-container al hacer hx-swap="outerHTML" sobre un elemento interno).
        if ("nube-content-area".equals(hxTarget)) {
            return "nube/index :: nube-content-area";
        }

        return htmxRequest ? "nube/index :: htmx-content" : "nube/index";
    }

    // Vista de una Carpeta Específica
    @GetMapping("/carpeta/{id}")
    public String verCarpeta(@PathVariable Long id, Model model) {
        nubeNodoService.registrarAcceso(id);

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
    public String crearCarpeta(@RequestParam String nombre, @RequestParam(required = false) Long padreId, Model model,
            HttpSession session) {
        Long propietarioId = (Long) session.getAttribute("SESSION_USUARIO_ID");
        nubeNodoService.crearCarpeta(nombre, padreId, propietarioId);

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
        Long propietarioId = (Long) session.getAttribute("SESSION_USUARIO_ID");
        try {
            nubeNodoService.subirArchivo(archivo, padreId, institucionId, propietarioId);
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
            return nubeNexa(false, null, model).replace("nube/index", "nube/index :: nube-content-area");
        }
    }

    // Eliminar Archivo o Carpeta (borrado lógico: va a la papelera)
    @DeleteMapping("/{id}")
    public String eliminar(@PathVariable Long id,
            @RequestParam(required = false) Long padreId,
            Model model) {
        nubeNodoService.eliminarNodo(id);

        if (padreId != null) {
            return verCarpeta(padreId, model);
        } else {
            return nubeNexa(false, null, model).replace("nube/index", "nube/index :: nube-content-area");
        }
    }

    // Vista de la Papelera
    @GetMapping("/papelera")
    public String papelera(Model model) {
        model.addAttribute("papeleraItems", nubeNodoService.obtenerPapelera());
        model.addAttribute("vistaPapelera", true);
        model.addAttribute("carpetas", List.of());
        model.addAttribute("archivos", List.of());
        model.addAttribute("carpetaActual", null);

        return "nube/index :: nube-content-area";
    }

    // Restaurar Archivo o Carpeta desde la Papelera
    @PostMapping("/restaurar/{id}")
    public String restaurar(@PathVariable Long id, Model model) {
        nubeNodoService.restaurarNodo(id);
        return papelera(model);
    }

    // Eliminar Archivo o Carpeta definitivamente desde la Papelera
    @DeleteMapping("/papelera/{id}")
    public String eliminarDefinitivamente(@PathVariable Long id, Model model) {
        nubeNodoService.eliminarNodoDefinitivamente(id);
        return papelera(model);
    }

    // Vista de Recientes
    @GetMapping("/recientes")
    public String recientes(Model model) {
        model.addAttribute("recientesItems", nubeNodoService.obtenerRecientes());
        model.addAttribute("vistaRecientes", true);
        model.addAttribute("carpetas", List.of());
        model.addAttribute("archivos", List.of());
        model.addAttribute("carpetaActual", null);

        return "nube/index :: nube-content-area";
    }

    // Registrar que un nodo fue abierto (usado por el modal de vista previa, incluso cuando
    // el archivo no tiene previsualización soportada y por eso nunca llega a golpear
    // ArchivoController.verArchivo/previewArchivo)
    @PostMapping("/registrar-acceso/{id}")
    @ResponseBody
    public void registrarAcceso(@PathVariable Long id) {
        nubeNodoService.registrarAcceso(id);
    }

    // Abre/navega el selector de destino de "Mover a..."
    @GetMapping("/mover/{id}")
    public String moverSelector(@PathVariable Long id,
            @RequestParam(required = false) Long destinoId,
            @RequestParam(required = false) Long padreId,
            Model model) {
        NubeNodo nodo = nubeNodoService.obtenerNodo(id)
                .orElseThrow(() -> new IllegalArgumentException("Nodo no encontrado"));

        model.addAttribute("nodo", nodo);
        model.addAttribute("destinoId", destinoId);
        model.addAttribute("padreId", padreId);
        model.addAttribute("breadcrumbsDestino",
                destinoId != null ? nubeNodoService.obtenerRutaBreadcrumb(destinoId) : List.of());
        model.addAttribute("carpetasDestino", nubeNodoService.obtenerCarpetasParaMover(destinoId, id));

        return "nube/mover-modal :: modal-content";
    }

    // Ejecuta el movimiento
    @PostMapping("/mover")
    public String mover(@RequestParam Long id,
            @RequestParam(required = false) Long destinoId,
            @RequestParam(required = false) Long padreId,
            Model model) {
        nubeNodoService.moverNodo(id, destinoId);

        if (padreId != null) {
            return verCarpeta(padreId, model);
        } else {
            return nubeNexa(false, null, model).replace("nube/index", "nube/index :: nube-content-area");
        }
    }
}
