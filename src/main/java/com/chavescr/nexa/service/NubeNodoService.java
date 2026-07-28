package com.chavescr.nexa.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.entity.NubeNodo;
import com.chavescr.nexa.entity.TipoNodo;
import com.chavescr.nexa.repository.InstitucionRepository;
import com.chavescr.nexa.repository.NubeNodoRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
public class NubeNodoService {

    private static final Logger log = LoggerFactory.getLogger(NubeNodoService.class);
    private static final Set<String> FORMATOS_OFFICE = Set.of(
            "DOCX", "DOC", "XLSX", "XLS", "PPTX", "PPT", "ODT", "ODS", "ODP");

    @Value("${ruta.recursos}")
    private String rutaRecursos;

    private final NubeNodoRepository repository;
    private final InstitucionRepository institucionRepository;
    private final DocumentConversionService conversionService;
    private final UsuarioRepository usuarioRepository;

    public NubeNodoService(NubeNodoRepository repository,
            InstitucionRepository institucionRepository,
            DocumentConversionService conversionService,
            UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.institucionRepository = institucionRepository;
        this.conversionService = conversionService;
        this.usuarioRepository = usuarioRepository;
    }

    public String getRutaRecursos() {
        return rutaRecursos;
    }

    public List<NubeNodo> obtenerNodosRaiz() {
        return repository.findByPadreIsNullAndFechaEliminacionIsNullOrderByTipoAscNombreAsc();
    }

    public List<NubeNodo> obtenerNodosPorPadre(Long padreId) {
        return repository.findByPadreIdAndFechaEliminacionIsNullOrderByTipoAscNombreAsc(padreId);
    }

    public List<NubeNodo> obtenerPapelera() {
        return repository.findRaicesEnPapelera();
    }

    public List<NubeNodo> obtenerRecientes() {
        return repository.findTop50ByFechaEliminacionIsNullAndUltimoAccesoIsNotNullOrderByUltimoAccesoDesc();
    }

    @Transactional(rollbackFor = Exception.class)
    public void moverNodo(Long id, Long destinoId) {
        NubeNodo nodo = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nodo no encontrado"));

        if (destinoId != null) {
            if (destinoId.equals(id) || esDescendienteOMismo(destinoId, id)) {
                throw new IllegalArgumentException(
                        "No se puede mover una carpeta dentro de sí misma o de una subcarpeta suya");
            }
            NubeNodo destino = repository.findById(destinoId)
                    .orElseThrow(() -> new IllegalArgumentException("Carpeta de destino no encontrada"));
            nodo.setPadre(destino);
        } else {
            nodo.setPadre(null);
        }

        repository.save(nodo);
    }

    // ¿"candidatoId" es el mismo nodo que "origenId", o desciende de él? (camina hacia arriba por padre)
    private boolean esDescendienteOMismo(Long candidatoId, Long origenId) {
        NubeNodo actual = repository.findById(candidatoId).orElse(null);
        while (actual != null) {
            if (actual.getId().equals(origenId)) {
                return true;
            }
            actual = actual.getPadre();
        }
        return false;
    }

    // Carpetas navegables en el selector de "Mover a...": excluye la que se mueve, sus propias
    // subcarpetas (evita referencias circulares) y lo que esté en la papelera.
    public List<NubeNodo> obtenerCarpetasParaMover(Long padreId, Long idExcluir) {
        List<NubeNodo> nivel = padreId != null ? obtenerNodosPorPadre(padreId) : obtenerNodosRaiz();
        return nivel.stream()
                .filter(n -> n.getTipo() == TipoNodo.CARPETA)
                .filter(n -> !n.getId().equals(idExcluir) && !esDescendienteOMismo(n.getId(), idExcluir))
                .collect(Collectors.toList());
    }

    public Optional<NubeNodo> obtenerNodo(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public NubeNodo crearCarpeta(String nombre, Long padreId, Long propietarioId) {
        NubeNodo carpeta = new NubeNodo();
        carpeta.setNombre(nombre);
        carpeta.setTipo(TipoNodo.CARPETA);

        if (padreId != null) {
            NubeNodo padre = repository.findById(padreId)
                    .orElseThrow(() -> new IllegalArgumentException("Carpeta padre no encontrada"));
            carpeta.setPadre(padre);
        }

        if (propietarioId != null) {
            usuarioRepository.findById(propietarioId).ifPresent(carpeta::setPropietario);
        }

        return repository.save(carpeta);
    }

    @Transactional
    public void registrarAcceso(Long id) {
        repository.findById(id).ifPresent(nodo -> {
            nodo.setUltimoAcceso(LocalDateTime.now());
            repository.save(nodo);
        });
    }

    public List<NubeNodo> obtenerRutaBreadcrumb(Long nodoId) {
        List<NubeNodo> ruta = new ArrayList<>();
        if (nodoId == null) {
            return ruta;
        }

        NubeNodo actual = repository.findById(nodoId).orElse(null);
        while (actual != null) {
            ruta.add(actual);
            actual = actual.getPadre();
        }

        Collections.reverse(ruta);
        return ruta;
    }

    @Transactional(rollbackFor = Exception.class)
    public NubeNodo subirArchivo(MultipartFile archivo, Long padreId, Long institucionId, Long propietarioId)
            throws IOException {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío o es nulo");
        }

        Institucion institucion = institucionRepository.findById(institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Institución no encontrada"));
        String codigoInstitucion = sanitizarNombreCarpeta(institucion.getCodigo());

        Path directorioDestino = Paths.get(rutaRecursos, codigoInstitucion, "nube-nexa");
        if (!Files.exists(directorioDestino)) {
            Files.createDirectories(directorioDestino);
        }

        String nombreOriginal = archivo.getOriginalFilename();
        String extension = "";
        if (nombreOriginal != null && nombreOriginal.contains(".")) {
            extension = nombreOriginal.substring(nombreOriginal.lastIndexOf(".") + 1).toUpperCase();
        }

        String nombreArchivoUnico = UUID.randomUUID().toString() + "_" + nombreOriginal;
        Path rutaDestino = directorioDestino.resolve(nombreArchivoUnico);

        Files.copy(archivo.getInputStream(), rutaDestino, StandardCopyOption.REPLACE_EXISTING);

        String rutaRelativa = codigoInstitucion + "/nube-nexa/" + nombreArchivoUnico;

        NubeNodo nodoArchivo = new NubeNodo();
        nodoArchivo.setNombre(nombreOriginal);
        nodoArchivo.setTipo(TipoNodo.ARCHIVO);
        nodoArchivo.setExtension(extension);
        nodoArchivo.setTamanoBytes(archivo.getSize());
        nodoArchivo.setUrlArchivo(rutaRelativa);

        if (padreId != null) {
            NubeNodo padre = repository.findById(padreId)
                    .orElseThrow(() -> new IllegalArgumentException("Carpeta padre no encontrada"));
            nodoArchivo.setPadre(padre);
        }

        if (propietarioId != null) {
            usuarioRepository.findById(propietarioId).ifPresent(nodoArchivo::setPropietario);
        }

        return repository.save(nodoArchivo);
    }

    private String sanitizarNombreCarpeta(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "institucion";
        }
        return nombre.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    public boolean generarPreview(Long nodoId) {
        NubeNodo nodo = repository.findById(nodoId).orElse(null);
        if (nodo == null || nodo.getTipo() != TipoNodo.ARCHIVO) {
            return false;
        }

        if (nodo.getUrlPrevisualizacion() != null) {
            Path previewPath = Paths.get(rutaRecursos).resolve(nodo.getUrlPrevisualizacion());
            if (Files.exists(previewPath)) {
                return true;
            }
        }

        String ext = nodo.getExtension();
        if (ext == null || !FORMATOS_OFFICE.contains(ext.toUpperCase())) {
            return false;
        }

        Path archivoOriginal = Paths.get(rutaRecursos).resolve(nodo.getUrlArchivo());
        if (!Files.exists(archivoOriginal)) {
            log.warn("Archivo original no encontrado para preview del nodo {}: {}", nodoId,
                    nodo.getUrlArchivo());
            return false;
        }

        String rutaRelativaPreview = generarRutaPreview(nodo.getUrlArchivo());
        Path rutaSalidaPreview = Paths.get(rutaRecursos).resolve(rutaRelativaPreview);

        Path resultado = conversionService.convertirAPdf(archivoOriginal, rutaSalidaPreview);
        if (resultado != null) {
            nodo.setUrlPrevisualizacion(rutaRelativaPreview);
            repository.save(nodo);
            log.info("Preview generado para nodo {}: {}", nodoId, rutaRelativaPreview);
            return true;
        }

        log.warn("No se pudo generar preview para nodo {} (ext: {}). El usuario podrá descargar el original.",
                nodoId, ext);
        return false;
    }

    private String generarRutaPreview(String urlArchivo) {
        int lastDot = urlArchivo.lastIndexOf('.');
        if (lastDot > 0) {
            return urlArchivo.substring(0, lastDot) + "_preview.pdf";
        }
        return urlArchivo + "_preview.pdf";
    }

    @Transactional(rollbackFor = Exception.class)
    public void renombrarNodo(Long id, String nuevoNombre) {
        NubeNodo nodo = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nodo no encontrado"));
        nodo.setNombre(nuevoNombre);
        repository.save(nodo);
    }

    @Transactional(rollbackFor = Exception.class)
    public void eliminarNodo(Long id) {
        NubeNodo nodo = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nodo no encontrado"));

        marcarEliminado(nodo, LocalDateTime.now());
    }

    @Transactional(rollbackFor = Exception.class)
    public void restaurarNodo(Long id) {
        NubeNodo nodo = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nodo no encontrado"));

        marcarEliminado(nodo, null);
    }

    private void marcarEliminado(NubeNodo nodo, LocalDateTime fecha) {
        nodo.setFechaEliminacion(fecha);
        repository.save(nodo);

        if (nodo.getTipo() == TipoNodo.CARPETA) {
            for (NubeNodo hijo : nodo.getHijos()) {
                marcarEliminado(hijo, fecha);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void eliminarNodoDefinitivamente(Long id) {
        NubeNodo nodo = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nodo no encontrado"));

        eliminarFisicamente(nodo);
        repository.delete(nodo);
    }

    private void eliminarFisicamente(NubeNodo nodo) {
        if (nodo.getTipo() == TipoNodo.ARCHIVO && nodo.getUrlArchivo() != null) {
            try {
                Path rutaFisica = Paths.get(rutaRecursos).resolve(nodo.getUrlArchivo());
                Files.deleteIfExists(rutaFisica);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (nodo.getUrlPrevisualizacion() != null) {
                try {
                    Path rutaPreview = Paths.get(rutaRecursos).resolve(nodo.getUrlPrevisualizacion());
                    Files.deleteIfExists(rutaPreview);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if (nodo.getTipo() == TipoNodo.CARPETA) {
            for (NubeNodo hijo : nodo.getHijos()) {
                eliminarFisicamente(hijo);
            }
        }
    }
}
