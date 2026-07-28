package com.chavescr.nexa.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.NubeNodo;
import com.chavescr.nexa.entity.NubeNodoAcceso;
import com.chavescr.nexa.entity.NubeNodoAcceso.NivelAcceso;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.NubeNodoAccesoRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
public class NubeAccesoService {

    public enum NivelEfectivo {
        SIN_ACCESO, LECTOR, EDITOR, PROPIETARIO
    }

    private final NubeNodoAccesoRepository accesoRepository;
    private final UsuarioRepository usuarioRepository;

    public NubeAccesoService(NubeNodoAccesoRepository accesoRepository, UsuarioRepository usuarioRepository) {
        this.accesoRepository = accesoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // Nivel efectivo de acceso de un usuario sobre un nodo, considerando accesos
    // heredados de las carpetas ancestras (compartir una carpeta comparte su contenido).
    public NivelEfectivo resolver(NubeNodo nodo, Usuario usuario, boolean esAdmin) {
        if (esAdmin) {
            return NivelEfectivo.PROPIETARIO;
        }
        if (usuario == null || nodo == null) {
            return NivelEfectivo.SIN_ACCESO;
        }
        if (nodo.getPropietario() != null && nodo.getPropietario().getId().equals(usuario.getId())) {
            return NivelEfectivo.PROPIETARIO;
        }

        NivelEfectivo mejor = NivelEfectivo.SIN_ACCESO;
        for (NubeNodo actual = nodo; actual != null; actual = actual.getPadre()) {
            NivelEfectivo r = accesoRepository.findByNodoIdAndUsuarioId(actual.getId(), usuario.getId())
                    .map(a -> a.getNivel() == NivelAcceso.EDITOR ? NivelEfectivo.EDITOR : NivelEfectivo.LECTOR)
                    .orElse(NivelEfectivo.SIN_ACCESO);
            if (r.ordinal() > mejor.ordinal()) {
                mejor = r;
            }
        }
        return mejor;
    }

    public boolean puedeVer(NubeNodo nodo, Usuario usuario, boolean esAdmin) {
        return resolver(nodo, usuario, esAdmin) != NivelEfectivo.SIN_ACCESO;
    }

    public boolean puedeEditar(NubeNodo nodo, Usuario usuario, boolean esAdmin) {
        return resolver(nodo, usuario, esAdmin).ordinal() >= NivelEfectivo.EDITOR.ordinal();
    }

    public boolean esPropietarioOAdmin(NubeNodo nodo, Usuario usuario, boolean esAdmin) {
        return resolver(nodo, usuario, esAdmin) == NivelEfectivo.PROPIETARIO;
    }

    public List<NubeNodoAcceso> listarAccesos(Long nodoId) {
        return accesoRepository.findByNodoIdOrderByFechaCompartidoDesc(nodoId);
    }

    // Elementos compartidos directamente con el usuario (no incluye lo heredado de carpetas
    // ancestras compartidas, ni lo que ya está en la papelera de su dueño).
    public List<NubeNodoAcceso> listarCompartidosConmigo(Usuario usuario) {
        if (usuario == null) {
            return List.of();
        }
        return accesoRepository.findByUsuarioIdOrderByFechaCompartidoDesc(usuario.getId()).stream()
                .filter(a -> a.getNodo().getFechaEliminacion() == null)
                .toList();
    }

    @Transactional
    public NubeNodoAcceso compartir(NubeNodo nodo, Long usuarioId, NivelAcceso nivel, Usuario compartidoPor) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        NubeNodoAcceso acceso = accesoRepository.findByNodoIdAndUsuarioId(nodo.getId(), usuarioId)
                .orElseGet(NubeNodoAcceso::new);
        acceso.setNodo(nodo);
        acceso.setUsuario(usuario);
        acceso.setNivel(nivel);
        acceso.setCompartidoPor(compartidoPor);
        return accesoRepository.save(acceso);
    }

    @Transactional
    public void quitarAcceso(Long nodoId, Long usuarioId) {
        accesoRepository.deleteByNodoIdAndUsuarioId(nodoId, usuarioId);
    }
}
