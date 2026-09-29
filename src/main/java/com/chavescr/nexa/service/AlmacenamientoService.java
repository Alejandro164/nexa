package com.chavescr.nexa.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.Institucion;

/**
 * Almacenamiento físico de archivos en {@code ruta.recursos}. Todo se agrupa por institución:
 * {@code <ruta.recursos>/<cédula-institución>/<módulo>/...}, así las direcciones (Preescolar,
 * Primaria, Secundaria) de una misma institución comparten carpeta.
 */
@Service
@Transactional
public class AlmacenamientoService {

    @Value("${ruta.recursos}")
    private String rutaRecursos;

    private final InstitucionService institucionService;

    public AlmacenamientoService(InstitucionService institucionService) {
        this.institucionService = institucionService;
    }

    // Sin cédula se falla explícitamente en vez de usar una carpeta genérica que mezclaría instituciones.
    @Transactional(rollbackFor = Exception.class)
    public String carpetaInstitucion(Direccion direccion) {
        if (direccion.getInstitucion() == null) {
            institucionService.asegurarInstituciones();
        }
        String carpeta = carpetaInstitucionOpcional(direccion);
        if (carpeta == null) {
            throw new IllegalArgumentException("La institución no tiene cédula registrada. "
                    + "Regístrela en la configuración de la institución antes de subir archivos.");
        }
        return carpeta;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public String carpetaInstitucionOpcional(Direccion direccion) {
        Institucion institucion = direccion.getInstitucion();
        if (institucion == null || institucion.getCedula() == null || institucion.getCedula().isBlank()) {
            return null;
        }
        return sanitizar(institucion.getCedula());
    }

    /**
     * Guarda el archivo en {@code <cédula>/<subcarpetas...>/<uuid>_<nombre>} y devuelve esa ruta
     * relativa a {@code ruta.recursos}, que es lo que se persiste en la entidad.
     */
    @Transactional(rollbackFor = Exception.class)
    public String guardar(MultipartFile archivo, Direccion direccion, String... subcarpetas) throws IOException {
        StringBuilder relativa = new StringBuilder(carpetaInstitucion(direccion));
        for (String subcarpeta : subcarpetas) {
            relativa.append('/').append(sanitizar(subcarpeta));
        }
        Path directorio = resolver(relativa.toString());
        Files.createDirectories(directorio);

        String nombre = UUID.randomUUID() + "_" + nombreSeguro(archivo.getOriginalFilename());
        Files.copy(archivo.getInputStream(), directorio.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);
        return relativa.append('/').append(nombre).toString();
    }

    // Resuelve una ruta relativa guardada en BD, sin permitir salir de ruta.recursos (../).
    public Path resolver(String rutaRelativa) {
        Path raiz = Paths.get(rutaRecursos).toAbsolutePath().normalize();
        Path ruta = raiz.resolve(rutaRelativa).normalize();
        if (!ruta.startsWith(raiz)) {
            throw new IllegalArgumentException("Ruta de archivo inválida");
        }
        return ruta;
    }

    public void eliminar(String rutaRelativa) {
        if (rutaRelativa == null) {
            return;
        }
        try {
            Files.deleteIfExists(resolver(rutaRelativa));
        } catch (IOException e) {
            // Un archivo huérfano en disco no debe impedir borrar el registro
        }
    }

    private String sanitizar(String nombre) {
        return nombre.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    // Algunos navegadores mandan la ruta completa del cliente; se conserva solo el nombre.
    private String nombreSeguro(String original) {
        if (original == null || original.isBlank()) {
            return "archivo";
        }
        String nombre = original.substring(Math.max(original.lastIndexOf('/'), original.lastIndexOf('\\')) + 1);
        nombre = nombre.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").trim();
        return nombre.isEmpty() ? "archivo" : nombre;
    }
}
