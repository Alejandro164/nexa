package com.chavescr.nexa.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.WhatsAppConfiguracion;
import com.chavescr.nexa.repository.DireccionRepository;
import com.chavescr.nexa.repository.WhatsAppConfiguracionRepository;

/** Configuración por dirección de la conexión con WhatsApp Business Cloud API (Meta). */
@Service
@Transactional
public class WhatsAppConfiguracionService {

    /** Credenciales ya descifradas, listas para llamar a la Graph API. Solo debe vivir en memoria. */
    public record CredencialesWhatsApp(String phoneNumberId, String accessToken) {
    }

    private final WhatsAppConfiguracionRepository configuracionRepository;
    private final DireccionRepository direccionRepository;
    private final CifradoService cifradoService;

    public WhatsAppConfiguracionService(WhatsAppConfiguracionRepository configuracionRepository,
            DireccionRepository direccionRepository, CifradoService cifradoService) {
        this.configuracionRepository = configuracionRepository;
        this.direccionRepository = direccionRepository;
        this.cifradoService = cifradoService;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public WhatsAppConfiguracion obtener(Long direccionId) {
        return configuracionRepository.findByDireccionId(direccionId)
                .orElseGet(() -> WhatsAppConfiguracion.predeterminada(null));
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public boolean estaActivo(Long direccionId) {
        return Boolean.TRUE.equals(obtener(direccionId).getActivo());
    }

    @Transactional(rollbackFor = Exception.class)
    public WhatsAppConfiguracion guardar(Long direccionId, String phoneNumberId, String businessAccountId,
            String numeroMostrar, String nuevoAccessToken, boolean activo) {
        if (phoneNumberId == null || phoneNumberId.isBlank()) {
            throw new IllegalArgumentException("Indica el Phone Number ID de WhatsApp Business");
        }
        WhatsAppConfiguracion config = configuracionRepository.findByDireccionId(direccionId)
                .orElseGet(() -> WhatsAppConfiguracion.predeterminada(direccionDe(direccionId)));

        config.setPhoneNumberId(phoneNumberId.trim());
        config.setBusinessAccountId(blankToNull(businessAccountId));
        config.setNumeroMostrar(blankToNull(numeroMostrar));
        if (nuevoAccessToken != null && !nuevoAccessToken.isBlank()) {
            config.setAccessTokenCifrado(cifradoService.cifrar(nuevoAccessToken.trim()));
        }
        if (activo && !config.tieneToken()) {
            throw new IllegalArgumentException("Ingresa el token de acceso antes de activar el envío");
        }
        config.setActivo(activo);
        config.setFechaActualizacion(LocalDateTime.now());
        return configuracionRepository.save(config);
    }

    /** Credenciales descifradas para probar la conexión, sin exigir que el envío esté activado. */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public CredencialesWhatsApp obtenerCredenciales(Long direccionId) {
        WhatsAppConfiguracion config = configuracionRepository.findByDireccionId(direccionId)
                .orElseThrow(() -> new IllegalStateException("WhatsApp no está configurado para esta dirección"));
        if (config.getPhoneNumberId() == null || config.getPhoneNumberId().isBlank() || !config.tieneToken()) {
            throw new IllegalStateException("Falta completar la configuración de WhatsApp (Phone Number ID y token)");
        }
        return new CredencialesWhatsApp(config.getPhoneNumberId(), cifradoService.descifrar(config.getAccessTokenCifrado()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void registrarPruebaExitosa(Long direccionId, String numeroDetectado) {
        configuracionRepository.findByDireccionId(direccionId).ifPresent(config -> {
            config.setUltimaPruebaExitosa(LocalDateTime.now());
            if (numeroDetectado != null && !numeroDetectado.isBlank()) {
                config.setNumeroMostrar(numeroDetectado);
            }
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
