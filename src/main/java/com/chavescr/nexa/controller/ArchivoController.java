package com.chavescr.nexa.controller;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.chavescr.nexa.entity.NubeNodo;
import com.chavescr.nexa.service.NubeNodoService;

import org.springframework.beans.factory.annotation.Autowired;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/archivos")
public class ArchivoController {

    @Autowired
    private NubeNodoService nubeNodoService;

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

        nubeNodoService.registrarAcceso(id);

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
                    <a href="/archivos/descargar/%d">Descargar archivo</a>
                    </div></body></html>""",
                    archivo.getNombre(), id);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(new ByteArrayResource(html.getBytes(StandardCharsets.UTF_8)));
        }

        return verArchivo(id);
    }

    @GetMapping("/ver/{id}")
    public ResponseEntity<Resource> verArchivo(@PathVariable Long id) {
        NubeNodo archivo = nubeNodoService.obtenerNodo(id)
                .orElseThrow(() -> new IllegalArgumentException("Archivo no encontrado"));

        if (!archivo.getTipo().name().equals("ARCHIVO")) {
            throw new IllegalArgumentException("El nodo no es un archivo");
        }

        nubeNodoService.registrarAcceso(id);

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
}
