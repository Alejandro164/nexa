package com.chavescr.nexa.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.AsistenteConfiguracion;
import com.chavescr.nexa.repository.AsistenteConfiguracionRepository;

/** Conexión del Chat Asistente con Grok (xAI): una sola para el sistema, administrada por System Config. */
@Service
@Transactional(readOnly = true, rollbackFor = Exception.class)
public class AsistenteConfiguracionService {

    /** Modelos que se pueden elegir, con su descripción para la pantalla de configuración. */
    public static final Map<String, String> MODELOS = modelos();

    private final AsistenteConfiguracionRepository repository;
    private final CifradoService cifradoService;

    public AsistenteConfiguracionService(AsistenteConfiguracionRepository repository, CifradoService cifradoService) {
        this.repository = repository;
        this.cifradoService = cifradoService;
    }

    private static Map<String, String> modelos() {
        Map<String, String> modelos = new LinkedHashMap<>();
        modelos.put("grok-4.7", "Grok 4.7 — el más reciente (recomendado)");
        modelos.put("grok-4.6", "Grok 4.6");
        modelos.put("grok-4.5", "Grok 4.5");
        return modelos;
    }

    public AsistenteConfiguracion obtener() {
        return repository.findFirstByOrderByIdAsc().orElseGet(AsistenteConfiguracion::new);
    }

    /** Si el botón del chat se muestra a los usuarios. */
    public boolean estaDisponible() {
        return repository.findFirstByOrderByIdAsc().map(AsistenteConfiguracion::isDisponible).orElse(false);
    }

    /** La llave descifrada; solo para llamar a la API. */
    public String apiKey() {
        AsistenteConfiguracion config = obtener();
        if (!config.tieneApiKey()) {
            throw new IllegalStateException("El asistente todavía no tiene una API key configurada.");
        }
        return cifradoService.descifrar(config.getApiKeyCifrada());
    }

    /**
     * @param nuevaApiKey en blanco mantiene la llave guardada.
     */
    @Transactional(rollbackFor = Exception.class)
    public AsistenteConfiguracion guardar(String nuevaApiKey, String modelo, boolean activo) {
        if (modelo == null || !MODELOS.containsKey(modelo)) {
            throw new IllegalArgumentException("Elige uno de los modelos disponibles.");
        }
        AsistenteConfiguracion config = obtener();
        if (nuevaApiKey != null && !nuevaApiKey.isBlank()) {
            String llave = nuevaApiKey.trim();
            if (!llave.startsWith("xai-")) {
                throw new IllegalArgumentException("La API key de xAI empieza con \"xai-\".");
            }
            config.setApiKeyCifrada(cifradoService.cifrar(llave));
            config.setUltimaPruebaExitosa(null);
        }
        if (activo && !config.tieneApiKey()) {
            throw new IllegalArgumentException("Ingresa la API key antes de activar el asistente.");
        }
        config.setModelo(modelo);
        config.setActivo(activo);
        config.setFechaActualizacion(LocalDateTime.now());
        return repository.save(config);
    }

    @Transactional(rollbackFor = Exception.class)
    public void registrarPruebaExitosa() {
        repository.findFirstByOrderByIdAsc().ifPresent(config -> {
            config.setUltimaPruebaExitosa(LocalDateTime.now());
            repository.save(config);
        });
    }
}
