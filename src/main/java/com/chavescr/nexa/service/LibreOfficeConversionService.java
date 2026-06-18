package com.chavescr.nexa.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LibreOfficeConversionService implements DocumentConversionService {

    private static final Logger log = LoggerFactory.getLogger(LibreOfficeConversionService.class);
    private static final long TIMEOUT_SECONDS = 30;

    @Override
    public Path convertirAPdf(Path archivoOriginal, Path rutaSalida) {
        Path directorioSalida = rutaSalida.getParent();
        Path directorioTemporal = null;

        try {
            directorioTemporal = Files.createTempDirectory("nexa-lo-");

            ProcessBuilder pb = new ProcessBuilder(
                    "libreoffice",
                    "--headless",
                    "--norestore",
                    "--convert-to", "pdf",
                    "--outdir", directorioTemporal.toString(),
                    archivoOriginal.toString());
            pb.redirectErrorStream(true);

            log.info("Iniciando conversión LibreOffice: {}", archivoOriginal.getFileName());
            Process process = pb.start();

            boolean terminado = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!terminado) {
                process.destroyForcibly();
                log.error("Timeout ({}s) en conversión LibreOffice para: {}", TIMEOUT_SECONDS,
                        archivoOriginal.getFileName());
                return null;
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                byte[] errorBytes = process.getInputStream().readAllBytes();
                log.error("LibreOffice falló con código {} para {}: {}", exitCode,
                        archivoOriginal.getFileName(), new String(errorBytes));
                return null;
            }

            Path pdfGenerado = Files.list(directorioTemporal)
                    .filter(p -> p.toString().endsWith(".pdf"))
                    .findFirst()
                    .orElse(null);

            if (pdfGenerado == null) {
                log.error("No se encontró PDF generado para: {}", archivoOriginal.getFileName());
                return null;
            }

            Path destinoFinal = rutaSalida;
            Files.move(pdfGenerado, destinoFinal, StandardCopyOption.REPLACE_EXISTING);

            log.info("Conversión exitosa: {} -> {}", archivoOriginal.getFileName(),
                    destinoFinal.getFileName());
            return destinoFinal;

        } catch (IOException e) {
            log.error("Error de E/S en conversión LibreOffice para {}: {}",
                    archivoOriginal.getFileName(), e.getMessage());
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Conversión interrumpida para: {}", archivoOriginal.getFileName());
            return null;
        } finally {
            limpiarDirectorioTemporal(directorioTemporal);
        }
    }

    private void limpiarDirectorioTemporal(Path directorio) {
        if (directorio == null) {
            return;
        }
        try {
            Files.walk(directorio)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }
}
