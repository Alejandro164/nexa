package com.chavescr.nexa.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.chavescr.nexa.service.NubeNodoService;

/**
 * Al arrancar, reubica los archivos de Nube Nexa guardados con el esquema anterior
 * ({@code <código-presupuestario>/nube-nexa/...}) en la carpeta de su institución
 * ({@code <cédula>/nube-nexa/...}). Es idempotente, así que corre en todos los perfiles.
 * Va después de {@link DataInitializer} para que en dev las cédulas semilla ya existan.
 */
@Component
@Order(20)
public class NubeNexaCedulaMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(NubeNexaCedulaMigration.class);

    private final NubeNodoService nubeNodoService;

    public NubeNexaCedulaMigration(NubeNodoService nubeNodoService) {
        this.nubeNodoService = nubeNodoService;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Un fallo aquí no debe impedir que la aplicación arranque; la migración se reintenta
        // en el próximo arranque (reconoce los archivos que ya se movieron físicamente).
        try {
            nubeNodoService.migrarArchivosACarpetaInstitucion();
        } catch (Exception e) {
            log.error("Nube Nexa: falló la migración de archivos a carpetas por cédula", e);
        }
    }
}
