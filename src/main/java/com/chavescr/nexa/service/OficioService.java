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

import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.entity.NubeNodo;
import com.chavescr.nexa.entity.NubeNodoAcceso;
import com.chavescr.nexa.entity.Oficio;
import com.chavescr.nexa.entity.TipoNodo;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.InstitucionRepository;
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
    private final InstitucionRepository institucionRepository;
    private final UsuarioRepository usuarioRepository;
    private final NubeNodoService nubeNodoService;
    private final NubeNodoRepository nubeNodoRepository;
    private final NubeNodoAccesoRepository nubeNodoAccesoRepository;
    private final EmailService emailService;

    public OficioService(OficioRepository oficioRepository, InstitucionRepository institucionRepository,
            UsuarioRepository usuarioRepository, NubeNodoService nubeNodoService,
            NubeNodoRepository nubeNodoRepository, NubeNodoAccesoRepository nubeNodoAccesoRepository,
            EmailService emailService) {
        this.oficioRepository = oficioRepository;
        this.institucionRepository = institucionRepository;
        this.usuarioRepository = usuarioRepository;
        this.nubeNodoService = nubeNodoService;
        this.nubeNodoRepository = nubeNodoRepository;
        this.nubeNodoAccesoRepository = nubeNodoAccesoRepository;
        this.emailService = emailService;
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
                        || normalizar(nombreDestinatario(o)).contains(f))
                .toList();
    }

    private String nombreDestinatario(Oficio oficio) {
        if (oficio.getDestinatarioUsuario() != null) {
            return oficio.getDestinatarioUsuario().getNombre();
        }
        if (oficio.getDestinatarioInstitucion() != null) {
            return oficio.getDestinatarioInstitucion().getNombre();
        }
        return "";
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarUsuariosActivos() {
        return usuarioRepository.findByActivoTrueOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public List<Institucion> listarInstitucionesActivas() {
        return institucionRepository.findByActivaTrueOrderByNombreAsc();
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

    public Oficio crear(Long institucionId, Long usuarioId, String asunto, String tipoDestinatario,
            Long destinatarioId, String numeroCircular) {
        if (asunto == null || asunto.isBlank()) {
            throw new IllegalArgumentException("El asunto es obligatorio");
        }

        Institucion institucion = institucionRepository.findById(institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Institución no encontrada"));

        Oficio oficio = new Oficio();
        oficio.setInstitucion(institucion);
        oficio.setNumero(generarNumero(institucionId));
        oficio.setAsunto(asunto.trim());
        aplicarDestinatario(oficio, tipoDestinatario, destinatarioId);
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
    public Oficio actualizar(Long institucionId, Long id, String asunto, String tipoDestinatario,
            Long destinatarioId, String numeroCircular) {
        if (asunto == null || asunto.isBlank()) {
            throw new IllegalArgumentException("El asunto es obligatorio");
        }

        Oficio oficio = obtenerPorId(institucionId, id);
        oficio.setAsunto(asunto.trim());
        aplicarDestinatario(oficio, tipoDestinatario, destinatarioId);
        oficio.setNumeroCircular(numeroCircular != null && !numeroCircular.isBlank() ? numeroCircular.trim() : null);

        Oficio guardado = oficioRepository.save(oficio);
        log.info("Oficio editado: id={}, numero={}", guardado.getId(), guardado.getNumero());
        return guardado;
    }

    /** El destinatario es, exactamente, un usuario registrado o una institución — nunca ambos ni ninguno. */
    private void aplicarDestinatario(Oficio oficio, String tipoDestinatario, Long destinatarioId) {
        if (destinatarioId == null || tipoDestinatario == null) {
            throw new IllegalArgumentException("El destinatario es obligatorio");
        }
        if ("USUARIO".equals(tipoDestinatario)) {
            Usuario usuario = usuarioRepository.findById(destinatarioId)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario destinatario no encontrado"));
            oficio.setDestinatarioUsuario(usuario);
            oficio.setDestinatarioInstitucion(null);
        } else if ("INSTITUCION".equals(tipoDestinatario)) {
            Institucion institucionDestino = institucionRepository.findById(destinatarioId)
                    .orElseThrow(() -> new IllegalArgumentException("Institución destinataria no encontrada"));
            oficio.setDestinatarioInstitucion(institucionDestino);
            oficio.setDestinatarioUsuario(null);
        } else {
            throw new IllegalArgumentException("Tipo de destinatario inválido");
        }
    }

    private String generarNumero(Long institucionId) {
        String prefijo = Year.now().getValue() + "-";
        long consecutivo = oficioRepository.countByInstitucionIdAndNumeroStartingWith(institucionId, prefijo) + 1;
        return prefijo + "%03d".formatted(consecutivo);
    }

    public Oficio subirDocumento(Long institucionId, Long oficioId, Long usuarioId, MultipartFile archivo)
            throws IOException {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío o es nulo");
        }

        Oficio oficio = obtenerPorId(institucionId, oficioId);

        // Si ya había un documento adjunto (se está reemplazando), se elimina primero del todo
        // (archivo físico + registro + accesos), no solo se sobrescribe.
        if (oficio.getNubeNodo() != null) {
            nubeNodoService.eliminarNodoDefinitivamente(oficio.getNubeNodo().getId());
            oficio.setNubeNodo(null);
        }

        NubeNodo carpeta = obtenerOCrearCarpetaOficios(institucionId, usuarioId);
        NubeNodo nodo = nubeNodoService.subirArchivo(archivo, carpeta.getId(), institucionId, usuarioId);

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
    public Oficio emitir(Long institucionId, Long id) {
        Oficio oficio = obtenerPorId(institucionId, id);
        if (oficio.getNubeNodo() == null) {
            throw new IllegalStateException("El oficio no tiene ningún documento adjunto para emitir");
        }
        if (!"PENDIENTE".equals(oficio.getEstado())) {
            throw new IllegalStateException("Solo se puede emitir un oficio en estado Pendiente");
        }

        String destinatarioEmail;
        String destinatarioNombre;
        if (oficio.getDestinatarioUsuario() != null) {
            destinatarioEmail = oficio.getDestinatarioUsuario().getEmail();
            destinatarioNombre = oficio.getDestinatarioUsuario().getNombre();
        } else if (oficio.getDestinatarioInstitucion() != null) {
            destinatarioEmail = oficio.getDestinatarioInstitucion().getEmail();
            destinatarioNombre = oficio.getDestinatarioInstitucion().getNombre();
        } else {
            throw new IllegalStateException("El oficio no tiene destinatario");
        }
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
                    oficio.getInstitucion().getNombre(), oficio.getNumero(), oficio.getAsunto(),
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
     * Carpeta raíz "Oficios" de la institución, en Nube Nexa: se crea la primera vez que hace falta.
     * Como Oficios también lo gestionan los ROLE_DIRECTOR (no solo ROLE_ADMIN, el único rol con acceso
     * automático en Nube Nexa), se comparte explícitamente con todo el personal admin/director activo
     * cada vez que se usa — así un Director agregado después queda cubierto en la próxima subida.
     */
    private NubeNodo obtenerOCrearCarpetaOficios(Long institucionId, Long usuarioId) {
        NubeNodo carpeta = nubeNodoRepository
                .findByNombreAndTipoAndInstitucionIdAndPadreIsNullAndFechaEliminacionIsNull(
                        CARPETA_OFICIOS, TipoNodo.CARPETA, institucionId)
                .orElseGet(() -> nubeNodoService.crearCarpeta(CARPETA_OFICIOS, null, usuarioId, institucionId));

        sincronizarAccesoAdminDirector(carpeta, institucionId);
        return carpeta;
    }

    private void sincronizarAccesoAdminDirector(NubeNodo carpeta, Long institucionId) {
        List<Usuario> personal = usuarioRepository.findActivosByInstitucionIdAndRolIn(institucionId,
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

    public void eliminar(Long institucionId, Long id) {
        Oficio oficio = obtenerPorId(institucionId, id);
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
