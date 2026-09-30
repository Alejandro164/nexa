package com.chavescr.nexa.service;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.CorreoConfiguracion;
import com.chavescr.nexa.entity.CorreoConfiguracion.Seguridad;
import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.repository.CorreoConfiguracionRepository;
import com.chavescr.nexa.repository.DireccionRepository;

/** Configuración por dirección del servidor SMTP para las notificaciones por correo. */
@Service
@Transactional
public class CorreoConfiguracionService {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    /** Credenciales ya descifradas, listas para abrir la conexión SMTP. Solo debe vivir en memoria. */
    public record CredencialesCorreo(String host, int puerto, Seguridad seguridad, String usuario, String password,
            String remitenteEmail, String remitenteNombre) {
    }

    private final CorreoConfiguracionRepository configuracionRepository;
    private final DireccionRepository direccionRepository;
    private final CifradoService cifradoService;

    public CorreoConfiguracionService(CorreoConfiguracionRepository configuracionRepository,
            DireccionRepository direccionRepository, CifradoService cifradoService) {
        this.configuracionRepository = configuracionRepository;
        this.direccionRepository = direccionRepository;
        this.cifradoService = cifradoService;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public CorreoConfiguracion obtener(Long direccionId) {
        return configuracionRepository.findByDireccionId(direccionId)
                .orElseGet(() -> CorreoConfiguracion.predeterminada(null));
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public boolean estaActivo(Long direccionId) {
        return Boolean.TRUE.equals(obtener(direccionId).getActivo());
    }

    @Transactional(rollbackFor = Exception.class)
    public CorreoConfiguracion guardar(Long direccionId, String host, Integer puerto, Seguridad seguridad,
            String usuario, String nuevaPassword, boolean borrarPassword, String remitenteEmail,
            String remitenteNombre, boolean activo) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Indica el servidor SMTP");
        }
        if (puerto == null || puerto < 1 || puerto > 65535) {
            throw new IllegalArgumentException("El puerto debe estar entre 1 y 65535");
        }
        if (remitenteEmail == null || !EMAIL.matcher(remitenteEmail.trim()).matches()) {
            throw new IllegalArgumentException("Indica un correo remitente válido");
        }
        CorreoConfiguracion config = configuracionRepository.findByDireccionId(direccionId)
                .orElseGet(() -> CorreoConfiguracion.predeterminada(direccionDe(direccionId)));

        config.setHost(host.trim());
        config.setPuerto(puerto);
        config.setSeguridad(seguridad != null ? seguridad : Seguridad.STARTTLS);
        config.setUsuario(blankToNull(usuario));
        config.setRemitenteEmail(remitenteEmail.trim());
        config.setRemitenteNombre(blankToNull(remitenteNombre));
        if (borrarPassword || config.getUsuario() == null) {
            config.setPasswordCifrado(null);
        } else if (nuevaPassword != null && !nuevaPassword.isBlank()) {
            // Sin trim: una contraseña puede empezar o terminar con espacios
            config.setPasswordCifrado(cifradoService.cifrar(nuevaPassword));
        }
        if (activo && config.getUsuario() != null && !config.tienePassword()) {
            throw new IllegalArgumentException("Ingresa la contraseña del usuario SMTP antes de activar el envío");
        }
        config.setActivo(activo);
        config.setFechaActualizacion(LocalDateTime.now());
        return configuracionRepository.save(config);
    }

    /** Credenciales descifradas para probar la conexión, sin exigir que el envío esté activado. */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public CredencialesCorreo obtenerCredenciales(Long direccionId) {
        CorreoConfiguracion config = configuracionRepository.findByDireccionId(direccionId)
                .orElseThrow(() -> new IllegalStateException("El correo no está configurado para esta dirección"));
        if (config.getHost() == null || config.getPuerto() == null || config.getRemitenteEmail() == null) {
            throw new IllegalStateException("Falta completar la configuración del correo (servidor, puerto y remitente)");
        }
        String password = config.tienePassword() ? cifradoService.descifrar(config.getPasswordCifrado()) : null;
        String nombre = config.getRemitenteNombre() != null ? config.getRemitenteNombre()
                : config.getDireccion().getNombre();
        return new CredencialesCorreo(config.getHost(), config.getPuerto(), config.getSeguridad(),
                config.getUsuario(), password, config.getRemitenteEmail(), nombre);
    }

    @Transactional(rollbackFor = Exception.class)
    public void registrarPruebaExitosa(Long direccionId) {
        configuracionRepository.findByDireccionId(direccionId).ifPresent(config -> {
            config.setUltimaPruebaExitosa(LocalDateTime.now());
            configuracionRepository.save(config);
        });
    }

    private String blankToNull(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

    private Direccion direccionDe(Long direccionId) {
        return direccionRepository.findById(direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Dirección no encontrada"));
    }
}
