package com.chavescr.nexa.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.NubeNodo;
import com.chavescr.nexa.entity.NubeNodoAcceso;
import com.chavescr.nexa.entity.Oficio;
import com.chavescr.nexa.entity.TipoNodo;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.DireccionRepository;
import com.chavescr.nexa.repository.NubeNodoAccesoRepository;
import com.chavescr.nexa.repository.NubeNodoRepository;
import com.chavescr.nexa.repository.OficioRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

import jakarta.mail.MessagingException;

@Service
@Transactional
public class OficioService {

    private static final Logger log = LoggerFactory.getLogger(OficioService.class);
    private static final String CARPETA_OFICIOS = "Oficios";
    private static final List<String> ROLES_CON_ACCESO_CARPETA = List.of("ROLE_ADMIN", "ROLE_DIRECTOR");

    private final OficioRepository oficioRepository;
    private final DireccionRepository direccionRepository;
    private final DireccionService direccionService;
    private final UsuarioRepository usuarioRepository;
    private final NubeNodoService nubeNodoService;
    private final NubeNodoRepository nubeNodoRepository;
    private final NubeNodoAccesoRepository nubeNodoAccesoRepository;
    private final EmailService emailService;

    public OficioService(OficioRepository oficioRepository, DireccionRepository direccionRepository,
            DireccionService direccionService, UsuarioRepository usuarioRepository,
            NubeNodoService nubeNodoService, NubeNodoRepository nubeNodoRepository,
            NubeNodoAccesoRepository nubeNodoAccesoRepository, EmailService emailService) {
        this.oficioRepository = oficioRepository;
        this.direccionRepository = direccionRepository;
        this.direccionService = direccionService;
        this.usuarioRepository = usuarioRepository;
        this.nubeNodoService = nubeNodoService;
        this.nubeNodoRepository = nubeNodoRepository;
        this.nubeNodoAccesoRepository = nubeNodoAccesoRepository;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public List<Oficio> listar(Long direccionId, String filtro) {
        List<Oficio> todos = oficioRepository.findByDireccionIdOrderByFechaDesc(direccionId);
        if (filtro == null || filtro.isBlank()) {
            return todos;
        }
        String f = normalizar(filtro.trim());
        return todos.stream()
                .filter(o -> normalizar(o.getNumero()).contains(f)
                        || normalizar(o.getAsunto()).contains(f)
                        || normalizar(nombreDestinatario(o)).contains(f))
                .toList();
    }

    private String nombreDestinatario(Oficio oficio) {
        return oficio.getDestinatarioDireccion() != null ? oficio.getDestinatarioDireccion().getNombre() : "";
    }

    @Transactional(readOnly = true)
    public List<Direccion> listarDireccionesActivas() {
        return direccionRepository.findByActivaTrueOrderByNombreAsc();
    }

    private String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase();
    }

    @Transactional(readOnly = true)
    public Oficio obtenerPorId(Long direccionId, Long id) {
        return oficioRepository.findByIdAndDireccionId(id, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Oficio no encontrado"));
    }

    public Oficio crear(Long direccionId, Long usuarioId, String asunto, Long destinatarioDireccionId,
            String numeroCircular) {
        if (asunto == null || asunto.isBlank()) {
            throw new IllegalArgumentException("El asunto es obligatorio");
        }

        Direccion direccion = direccionRepository.findById(direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Dirección no encontrada"));

        Oficio oficio = new Oficio();
        oficio.setDireccion(direccion);
        oficio.setNumero(generarNumero(direccionId));
        oficio.setAsunto(asunto.trim());
        oficio.setDestinatarioDireccion(resolverDireccionDestinataria(destinatarioDireccionId));
        oficio.setNumeroCircular(numeroCircular != null && !numeroCircular.isBlank() ? numeroCircular.trim() : null);
        oficio.setEstado("BORRADOR");
        oficio.setFecha(LocalDate.now());
        if (usuarioId != null) {
            usuarioRepository.findById(usuarioId).ifPresent(oficio::setRedactadoPor);
        }

        Oficio guardado = oficioRepository.save(oficio);
        log.info("Oficio creado: id={}, numero={}", guardado.getId(), guardado.getNumero());
        return guardado;
    }

    /** Edita el asunto/destinatario/circular de un oficio existente. El número, estado y documento no cambian aquí. */
    public Oficio actualizar(Long direccionId, Long id, String asunto, Long destinatarioDireccionId,
            String numeroCircular) {
        if (asunto == null || asunto.isBlank()) {
            throw new IllegalArgumentException("El asunto es obligatorio");
        }

        Oficio oficio = obtenerPorId(direccionId, id);
        oficio.setAsunto(asunto.trim());
        oficio.setDestinatarioDireccion(resolverDireccionDestinataria(destinatarioDireccionId));
        oficio.setNumeroCircular(numeroCircular != null && !numeroCircular.isBlank() ? numeroCircular.trim() : null);

        Oficio guardado = oficioRepository.save(oficio);
        log.info("Oficio editado: id={}, numero={}", guardado.getId(), guardado.getNumero());
        return guardado;
    }

    private Direccion resolverDireccionDestinataria(Long destinatarioDireccionId) {
        if (destinatarioDireccionId == null) {
            throw new IllegalArgumentException("La dirección destinataria es obligatoria");
        }
        return direccionRepository.findById(destinatarioDireccionId)
                .orElseThrow(() -> new IllegalArgumentException("Dirección destinataria no encontrada"));
    }

    /** Registro rápido de una dirección destinataria que aún no existe, desde el propio formulario de oficio. */
    public Direccion registrarDireccionDestinataria(String nombre, String cedula, String email) {
        Direccion direccion = new Direccion();
        direccion.setNombre(nombre);
        direccion.setCedula(cedula);
        direccion.setEmail(email != null && !email.isBlank() ? email.trim() : null);
        return direccionService.save(direccion);
    }

    private String generarNumero(Long direccionId) {
        String prefijo = Year.now().getValue() + "-";
        long consecutivo = oficioRepository.countByDireccionIdAndNumeroStartingWith(direccionId, prefijo) + 1;
        return prefijo + "%03d".formatted(consecutivo);
    }

    public Oficio subirDocumento(Long direccionId, Long oficioId, Long usuarioId, MultipartFile archivo)
            throws IOException {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío o es nulo");
        }

        Oficio oficio = obtenerPorId(direccionId, oficioId);

        // Si ya había un documento adjunto (se está reemplazando), se elimina primero del todo
        // (archivo físico + registro + accesos), no solo se sobrescribe.
        if (oficio.getNubeNodo() != null) {
            nubeNodoService.eliminarNodoDefinitivamente(oficio.getNubeNodo().getId());
            oficio.setNubeNodo(null);
        }

        NubeNodo carpeta = obtenerOCrearCarpetaOficios(direccionId, usuarioId);
        NubeNodo nodo = nubeNodoService.subirArchivo(archivo, carpeta.getId(), direccionId, usuarioId);

        oficio.setNubeNodo(nodo);
        oficio.setEstado("PENDIENTE");

        Oficio guardado = oficioRepository.save(oficio);
        log.info("Documento subido para oficio: id={}, numero={}, nubeNodoId={}",
                guardado.getId(), guardado.getNumero(), nodo.getId());
        return guardado;
    }

    /**
     * Marca el oficio como emitido y envía el documento por correo al destinatario. Si el envío
     * falla, la excepción se propaga antes de tocar el estado — el oficio se queda en PENDIENTE,
     * nada queda a medias.
     */
    public Oficio emitir(Long direccionId, Long id) {
        Oficio oficio = obtenerPorId(direccionId, id);
        if (oficio.getNubeNodo() == null) {
            throw new IllegalStateException("El oficio no tiene ningún documento adjunto para emitir");
        }
        if (!"PENDIENTE".equals(oficio.getEstado())) {
            throw new IllegalStateException("Solo se puede emitir un oficio en estado Pendiente");
        }

        String destinatarioEmail = oficio.getDestinatarioDireccion().getEmail();
        String destinatarioNombre = oficio.getDestinatarioDireccion().getNombre();
        if (destinatarioEmail == null || destinatarioEmail.isBlank()) {
            throw new IllegalStateException("El destinatario no tiene un correo electrónico configurado");
        }

        NubeNodo documento = oficio.getNubeNodo();
        byte[] pdf;
        try {
            Path rutaArchivo = Paths.get(nubeNodoService.getRutaRecursos()).resolve(documento.getUrlArchivo());
            pdf = Files.readAllBytes(rutaArchivo);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el documento del oficio: " + e.getMessage());
        }

        try {
            emailService.enviarOficioEmitido(destinatarioEmail, destinatarioNombre,
                    oficio.getDireccion().getNombre(), oficio.getNumero(), oficio.getAsunto(),
                    pdf, documento.getNombre());
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException("No se pudo enviar el correo: " + e.getMessage());
        }

        oficio.setEstado("EMITIDO");
        Oficio guardado = oficioRepository.save(oficio);
        log.info("Oficio emitido y enviado por correo: id={}, numero={}, destinatario={}",
                guardado.getId(), guardado.getNumero(), destinatarioEmail);
        return guardado;
    }

    /**
     * Carpeta raíz "Oficios" de la dirección, en Nube Nexa: se crea la primera vez que hace falta.
     * Como Oficios también lo gestionan los ROLE_DIRECTOR (no solo ROLE_ADMIN, el único rol con acceso
     * automático en Nube Nexa), se comparte explícitamente con todo el personal admin/director activo
     * cada vez que se usa — así un Director agregado después queda cubierto en la próxima subida.
     */
    private NubeNodo obtenerOCrearCarpetaOficios(Long direccionId, Long usuarioId) {
        NubeNodo carpeta = nubeNodoRepository
                .findByNombreAndTipoAndDireccionIdAndPadreIsNullAndFechaEliminacionIsNull(
                        CARPETA_OFICIOS, TipoNodo.CARPETA, direccionId)
                .orElseGet(() -> nubeNodoService.crearCarpeta(CARPETA_OFICIOS, null, usuarioId, direccionId));

        sincronizarAccesoAdminDirector(carpeta, direccionId);
        return carpeta;
    }

    private void sincronizarAccesoAdminDirector(NubeNodo carpeta, Long direccionId) {
        List<Usuario> personal = usuarioRepository.findActivosByDireccionIdAndRolIn(direccionId,
                ROLES_CON_ACCESO_CARPETA);
        for (Usuario usuario : personal) {
            if (!nubeNodoAccesoRepository.existsByNodoIdAndUsuarioId(carpeta.getId(), usuario.getId())) {
                NubeNodoAcceso acceso = new NubeNodoAcceso();
                acceso.setNodo(carpeta);
                acceso.setUsuario(usuario);
                acceso.setNivel(NubeNodoAcceso.NivelAcceso.LECTOR);
                nubeNodoAccesoRepository.save(acceso);
            }
        }
    }

    public void eliminar(Long direccionId, Long id) {
        Oficio oficio = obtenerPorId(direccionId, id);
        if ("EMITIDO".equals(oficio.getEstado())) {
            throw new IllegalStateException("No se puede eliminar un oficio ya emitido");
        }
        if (oficio.getNubeNodo() != null) {
            nubeNodoService.eliminarNodoDefinitivamente(oficio.getNubeNodo().getId());
        }
        oficioRepository.delete(oficio);
        log.info("Oficio eliminado: id={}", id);
    }

    public String getRutaRecursos() {
        return nubeNodoService.getRutaRecursos();
    }
}
