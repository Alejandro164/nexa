package com.chavescr.nexa.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.entity.Oficio;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.InstitucionRepository;
import com.chavescr.nexa.repository.OficioRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class OficioService {

    private static final Logger log = LoggerFactory.getLogger(OficioService.class);

    @Value("${ruta.recursos}")
    private String rutaRecursos;

    private final OficioRepository oficioRepository;
    private final InstitucionRepository institucionRepository;
    private final UsuarioRepository usuarioRepository;

    public OficioService(OficioRepository oficioRepository, InstitucionRepository institucionRepository,
            UsuarioRepository usuarioRepository) {
        this.oficioRepository = oficioRepository;
        this.institucionRepository = institucionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<Oficio> listar(Long institucionId, String filtro) {
        List<Oficio> todos = oficioRepository.findByInstitucionIdOrderByFechaDesc(institucionId);
        if (filtro == null || filtro.isBlank()) {
            return todos;
        }
        String f = normalizar(filtro.trim());
        return todos.stream()
                .filter(o -> normalizar(o.getNumero()).contains(f)
                        || normalizar(o.getAsunto()).contains(f)
                        || normalizar(o.getDestinatario()).contains(f))
                .toList();
    }

    private String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase();
    }

    @Transactional(readOnly = true)
    public Oficio obtenerPorId(Long institucionId, Long id) {
        return oficioRepository.findByIdAndInstitucionId(id, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Oficio no encontrado"));
    }

    public Oficio crear(Long institucionId, Long usuarioId, String asunto, String destinatario, String numeroCircular) {
        if (asunto == null || asunto.isBlank()) {
            throw new IllegalArgumentException("El asunto es obligatorio");
        }
        if (destinatario == null || destinatario.isBlank()) {
            throw new IllegalArgumentException("El destinatario es obligatorio");
        }

        Institucion institucion = institucionRepository.findById(institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Institución no encontrada"));

        Oficio oficio = new Oficio();
        oficio.setInstitucion(institucion);
        oficio.setNumero(generarNumero(institucionId));
        oficio.setAsunto(asunto.trim());
        oficio.setDestinatario(destinatario.trim());
        oficio.setNumeroCircular(numeroCircular != null && !numeroCircular.isBlank() ? numeroCircular.trim() : null);
        oficio.setEstado("PENDIENTE");
        oficio.setFecha(LocalDate.now());
        if (usuarioId != null) {
            usuarioRepository.findById(usuarioId).ifPresent(oficio::setRedactadoPor);
        }

        Oficio guardado = oficioRepository.save(oficio);
        log.info("Oficio creado: id={}, numero={}", guardado.getId(), guardado.getNumero());
        return guardado;
    }

    private String generarNumero(Long institucionId) {
        String prefijo = "OF-" + Year.now().getValue() + "-";
        long consecutivo = oficioRepository.countByInstitucionIdAndNumeroStartingWith(institucionId, prefijo) + 1;
        return prefijo + "%03d".formatted(consecutivo);
    }

    public Oficio subirDocumento(Long institucionId, Long oficioId, MultipartFile archivo) throws IOException {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío o es nulo");
        }

        Oficio oficio = obtenerPorId(institucionId, oficioId);
        String codigoInstitucion = sanitizarNombreCarpeta(oficio.getInstitucion().getCodigo());

        Path directorioDestino = Paths.get(rutaRecursos, codigoInstitucion, "oficios");
        if (!Files.exists(directorioDestino)) {
            Files.createDirectories(directorioDestino);
        }

        // Si ya había un documento adjunto, se reemplaza: se borra el archivo viejo del volumen.
        if (oficio.getRutaArchivo() != null) {
            Files.deleteIfExists(Paths.get(rutaRecursos).resolve(oficio.getRutaArchivo()));
        }

        String nombreOriginal = archivo.getOriginalFilename();
        String nombreArchivoUnico = UUID.randomUUID() + "_" + nombreOriginal;
        Path rutaDestino = directorioDestino.resolve(nombreArchivoUnico);
        Files.copy(archivo.getInputStream(), rutaDestino, StandardCopyOption.REPLACE_EXISTING);

        oficio.setRutaArchivo(codigoInstitucion + "/oficios/" + nombreArchivoUnico);
        oficio.setNombreArchivoOriginal(nombreOriginal);
        oficio.setEstado("EMITIDO");

        Oficio guardado = oficioRepository.save(oficio);
        log.info("Documento subido para oficio: id={}, numero={}", guardado.getId(), guardado.getNumero());
        return guardado;
    }

    private String sanitizarNombreCarpeta(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "institucion";
        }
        return nombre.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    public void eliminar(Long institucionId, Long id) throws IOException {
        Oficio oficio = obtenerPorId(institucionId, id);
        if (oficio.getRutaArchivo() != null) {
            Files.deleteIfExists(Paths.get(rutaRecursos).resolve(oficio.getRutaArchivo()));
        }
        oficioRepository.delete(oficio);
        log.info("Oficio eliminado: id={}", id);
    }

    public String getRutaRecursos() {
        return rutaRecursos;
    }
}
