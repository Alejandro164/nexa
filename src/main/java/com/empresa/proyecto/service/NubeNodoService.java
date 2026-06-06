package com.empresa.proyecto.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.empresa.proyecto.entity.NubeNodo;
import com.empresa.proyecto.entity.TipoNodo;
import com.empresa.proyecto.repository.NubeNodoRepository;

@Service
public class NubeNodoService {

    @Value("${ruta.recursos}")
    private String rutaRecursos;

    @Autowired
    private NubeNodoRepository repository;

    public String getRutaRecursos() {
        return rutaRecursos;
    }

    public List<NubeNodo> obtenerNodosRaiz() {
        return repository.findByPadreIsNullOrderByTipoAscNombreAsc();
    }

    public List<NubeNodo> obtenerNodosPorPadre(Long padreId) {
        return repository.findByPadreIdOrderByTipoAscNombreAsc(padreId);
    }

    public Optional<NubeNodo> obtenerNodo(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public NubeNodo crearCarpeta(String nombre, Long padreId) {
        NubeNodo carpeta = new NubeNodo();
        carpeta.setNombre(nombre);
        carpeta.setTipo(TipoNodo.CARPETA);

        if (padreId != null) {
            NubeNodo padre = repository.findById(padreId)
                    .orElseThrow(() -> new IllegalArgumentException("Carpeta padre no encontrada"));
            carpeta.setPadre(padre);
        }

        return repository.save(carpeta);
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
    public NubeNodo subirArchivo(MultipartFile archivo, Long padreId) throws IOException {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío o es nulo");
        }

        Path directorioBase = Paths.get(rutaRecursos);
        if (!Files.exists(directorioBase)) {
            Files.createDirectories(directorioBase);
        }

        String nombreOriginal = archivo.getOriginalFilename();
        String extension = "";
        if (nombreOriginal != null && nombreOriginal.contains(".")) {
            extension = nombreOriginal.substring(nombreOriginal.lastIndexOf(".") + 1).toUpperCase();
        }

        String nombreArchivoUnico = UUID.randomUUID().toString() + "_" + nombreOriginal;
        Path rutaDestino = directorioBase.resolve(nombreArchivoUnico);

        Files.copy(archivo.getInputStream(), rutaDestino, StandardCopyOption.REPLACE_EXISTING);

        NubeNodo nodoArchivo = new NubeNodo();
        nodoArchivo.setNombre(nombreOriginal);
        nodoArchivo.setTipo(TipoNodo.ARCHIVO);
        nodoArchivo.setExtension(extension);
        nodoArchivo.setTamanoBytes(archivo.getSize());
        nodoArchivo.setUrlArchivo(nombreArchivoUnico);

        if (padreId != null) {
            NubeNodo padre = repository.findById(padreId)
                    .orElseThrow(() -> new IllegalArgumentException("Carpeta padre no encontrada"));
            nodoArchivo.setPadre(padre);
        }

        return repository.save(nodoArchivo);
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
        
        eliminarFisicamente(nodo);
        repository.delete(nodo);
    }

    private void eliminarFisicamente(NubeNodo nodo) {
        if (nodo.getTipo() == TipoNodo.ARCHIVO && nodo.getUrlArchivo() != null) {
            try {
                Path rutaFisica = Paths.get(rutaRecursos).resolve(nodo.getUrlArchivo());
                Files.deleteIfExists(rutaFisica);
            } catch (IOException e) {
                // Loguear pero no interrumpir la eliminación en BD si el archivo ya no existe
                e.printStackTrace();
            }
        } else if (nodo.getTipo() == TipoNodo.CARPETA) {
            for (NubeNodo hijo : nodo.getHijos()) {
                eliminarFisicamente(hijo);
            }
        }
    }
}
