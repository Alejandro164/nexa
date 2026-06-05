package com.empresa.proyecto.service;

import com.empresa.proyecto.entity.NubeNodo;
import com.empresa.proyecto.entity.TipoNodo;
import com.empresa.proyecto.repository.NubeNodoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class NubeNodoService {

    @Autowired
    private NubeNodoRepository repository;

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

    // TODO: Implementar lógica de subirArchivo (MultipartFile, padreId) en el futuro
}
