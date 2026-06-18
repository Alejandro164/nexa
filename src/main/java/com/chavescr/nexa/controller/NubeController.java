package com.chavescr.nexa.controller;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    // Previsualizar Archivo en el Navegador
    @GetMapping("/ver/{id}")
    public ResponseEntity<Resource> verArchivo(@PathVariable Long id) {
        NubeNodo archivo = nubeNodoService.obtenerNodo(id)
                .orElseThrow(() -> new IllegalArgumentException("Archivo no encontrado"));

        if (!archivo.getTipo().name().equals("ARCHIVO")) {
            throw new IllegalArgumentException("El nodo no es un archivo");
        }

        try {
            Path filePath = Paths.get(nubeNodoService.getRutaRecursos()).resolve(archivo.getUrlArchivo());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                String contentType = java.nio.file.Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                return ResponseEntity.ok()
                        .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + archivo.getNombre() + "\"")
                        .body(resource);
            } else {
                throw new RuntimeException("No se puede leer el archivo");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al ver el archivo: " + e.getMessage());
        }
    }

    private static final Set<String> FORMATOS_OFFICE = Set.of(
            "DOCX", "DOC", "XLSX", "XLS", "PPTX", "PPT", "ODT", "ODS", "ODP");

    @GetMapping("/preview/{id}")
    public ResponseEntity<Resource> previewArchivo(@PathVariable Long id, HttpSession session) {
        Long institucionId = (Long) session.getAttribute("SESSION_INSTITUCION_ID");
        if (institucionId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        NubeNodo archivo = nubeNodoService.obtenerNodo(id)
                .orElseThrow(() -> new IllegalArgumentException("Archivo no encontrado"));

        if (!archivo.getTipo().name().equals("ARCHIVO")) {
            throw new IllegalArgumentException("El nodo no es un archivo");
        }

        boolean previewDisponible = nubeNodoService.generarPreview(id);

        if (previewDisponible) {
            NubeNodo actualizado = nubeNodoService.obtenerNodo(id).orElseThrow();
            try {
                Path previewPath = Paths.get(nubeNodoService.getRutaRecursos())
                        .resolve(actualizado.getUrlPrevisualizacion());
                Resource resource = new UrlResource(previewPath.toUri());
                if (resource.exists()) {
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_PDF)
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    "inline; filename=\"preview.pdf\"")
                            .body(resource);
                }
            } catch (Exception e) {
                // fallback abajo
            }
        }

        String ext = archivo.getExtension();
        if (ext != null && FORMATOS_OFFICE.contains(ext.toUpperCase())) {
            String html = String.format("""
                    <!DOCTYPE html>
                    <html><head><meta charset="UTF-8"><style>
                    body{font-family:-apple-system,sans-serif;display:flex;align-items:center;justify-content:center;
                    height:100vh;margin:0;background:#f8fafc;color:#334155}
                    .card{text-align:center;padding:2.5rem;background:white;border-radius:12px;
                    box-shadow:0 4px 12px rgba(0,0,0,.08);max-width:420px}
                    h3{margin:0 0 .5rem;font-size:1.25rem;color:#0f172a}
                    p{margin:0 0 1.5rem;font-size:.9rem;color:#64748b}
                    a{display:inline-block;padding:.65rem 1.5rem;background:#2563eb;color:white;
                    text-decoration:none;border-radius:6px;font-weight:500;font-size:.9rem}
                    </style></head><body><div class="card">
                    <h3>Vista previa no disponible</h3>
                    <p>No se pudo generar la previsualizaci\u00f3n para <strong>%s</strong>.
                    Pod\u00e9s descargar el archivo original.</p>
                    <a href="/nube-nexa/descargar/%d">Descargar archivo</a>
                    </div></body></html>""",
                    archivo.getNombre(), id);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(new ByteArrayResource(html.getBytes(StandardCharsets.UTF_8)));
        }

        return verArchivo(id);
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
