package com.chavescr.nexa.service;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.Centro;
import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.entity.OfertaEducativa;
import com.chavescr.nexa.repository.CentroRepository;
import com.chavescr.nexa.repository.InstitucionRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class CentroService {

    private final CentroRepository centroRepository;
    private final InstitucionRepository institucionRepository;
    private final UsuarioRepository usuarioRepository;
    private final NivelAcademicoRepository nivelAcademicoRepository;

    public CentroService(CentroRepository centroRepository, InstitucionRepository institucionRepository,
            UsuarioRepository usuarioRepository, NivelAcademicoRepository nivelAcademicoRepository) {
        this.centroRepository = centroRepository;
        this.institucionRepository = institucionRepository;
        this.usuarioRepository = usuarioRepository;
        this.nivelAcademicoRepository = nivelAcademicoRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<Centro> listar() {
        if (institucionRepository.existsByCentroIsNull()) {
            asegurarCentros();
        }
        return centroRepository.findAllConInstituciones();
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Centro obtener(Long id) {
        return centroRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Centro no encontrado"));
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<Institucion> ofertas(Long centroId) {
        return institucionRepository.findByCentroId(centroId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void asegurarCentros() {
        for (Institucion institucion : institucionRepository.findByCentroIsNull()) {
            Centro centro = new Centro();
            centro.setNombre(institucion.getNombre());
            centro.setCedula(cedulaLibre(texto(institucion.getCedula())));
            centro.setDireccion(institucion.getDireccion());
            centro.setTelefono(institucion.getTelefono());
            centro.setEmail(institucion.getEmail());
            centro.setActiva(institucion.getActiva() == null || institucion.getActiva());
            centroRepository.save(centro);
            institucion.setCentro(centro);
            institucionRepository.save(institucion);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Centro guardar(Long centroId, String cedula, String nombre, String direccion, String telefono,
            String email, boolean activa, List<String> ofertas, Map<String, String> codigos) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del centro es obligatorio.");
        }
        LinkedHashSet<OfertaEducativa> elegidas = ofertasSeleccionadas(ofertas);
        if (elegidas.isEmpty()) {
            throw new IllegalArgumentException("Seleccione al menos una dirección: Preescolar, Primaria o Secundaria.");
        }

        Centro centro = centroId == null ? new Centro() : obtener(centroId);
        String cedulaNormalizada = texto(cedula);
        if (cedulaNormalizada != null) {
            centroRepository.findByCedula(cedulaNormalizada)
                    .filter(existente -> centro.getId() == null || !existente.getId().equals(centro.getId()))
                    .ifPresent(existente -> {
                        throw new IllegalArgumentException("Ya existe un centro con esa cédula.");
                    });
        }
        centro.setCedula(cedulaNormalizada);
        centro.setNombre(nombre.trim());
        centro.setDireccion(texto(direccion));
        centro.setTelefono(texto(telefono));
        centro.setEmail(texto(email));
        centro.setActiva(activa);
        centroRepository.save(centro);

        List<Institucion> actuales = institucionRepository.findByCentroId(centro.getId());
        Institucion sinClasificar = actuales.stream().filter(inst -> inst.getOferta() == null).findFirst().orElse(null);
        Set<Long> conservadas = new HashSet<>();

        for (OfertaEducativa oferta : OfertaEducativa.values()) {
            if (!elegidas.contains(oferta)) {
                continue;
            }
            String codigo = texto(codigos.get(oferta.name()));
            if (codigo == null) {
                throw new IllegalArgumentException("El código presupuestario de " + oferta.getEtiqueta() + " es obligatorio.");
            }
            Institucion institucion = actuales.stream()
                    .filter(inst -> oferta == inst.getOferta())
                    .findFirst()
                    .orElse(null);
            if (institucion == null) {
                institucion = sinClasificar;
                sinClasificar = null;
            }
            if (institucion == null) {
                institucion = new Institucion(centro.getNombre(), codigo);
            }
            validarCodigoLibre(codigo, institucion.getId());
            copiarCentro(institucion, centro, oferta, codigo);
            institucionRepository.save(institucion);
            conservadas.add(institucion.getId());
        }

        for (Institucion institucion : actuales) {
            if (conservadas.contains(institucion.getId())) {
                continue;
            }
            if (tieneDatos(institucion.getId())) {
                String cual = institucion.getOferta() == null ? "la institución actual" : institucion.getOferta().getEtiqueta();
                throw new IllegalArgumentException("No se puede quitar " + cual + " porque ya tiene personas o secciones.");
            }
            institucionRepository.delete(institucion);
        }
        return centro;
    }

    @Transactional(rollbackFor = Exception.class)
    public void eliminar(Long centroId) {
        Centro centro = obtener(centroId);
        for (Institucion institucion : institucionRepository.findByCentroId(centroId)) {
            if (tieneDatos(institucion.getId())) {
                throw new IllegalArgumentException("No se puede eliminar el centro porque " + institucion.getPresentacion()
                        + " ya tiene personas o secciones.");
            }
            institucionRepository.delete(institucion);
        }
        centroRepository.delete(centro);
    }

    private void copiarCentro(Institucion institucion, Centro centro, OfertaEducativa oferta, String codigo) {
        institucion.setCentro(centro);
        institucion.setOferta(oferta);
        institucion.setNombre(centro.getNombre());
        institucion.setCodigo(codigo);
        institucion.setDireccion(centro.getDireccion());
        institucion.setTelefono(centro.getTelefono());
        institucion.setEmail(centro.getEmail());
        institucion.setActiva(centro.getActiva());
    }

    private boolean tieneDatos(Long institucionId) {
        return usuarioRepository.countByInstitucionId(institucionId) > 0
                || nivelAcademicoRepository.existsByInstitucionId(institucionId);
    }

    private void validarCodigoLibre(String codigo, Long institucionId) {
        institucionRepository.findByCodigo(codigo)
                .filter(existente -> institucionId == null || !existente.getId().equals(institucionId))
                .ifPresent(existente -> {
                    throw new IllegalArgumentException("El código " + codigo + " ya está en uso.");
                });
    }

    private String cedulaLibre(String cedula) {
        if (cedula == null || centroRepository.findByCedula(cedula).isPresent()) {
            return null;
        }
        return cedula;
    }

    private static LinkedHashSet<OfertaEducativa> ofertasSeleccionadas(List<String> ofertas) {
        LinkedHashSet<OfertaEducativa> elegidas = new LinkedHashSet<>();
        if (ofertas == null) {
            return elegidas;
        }
        for (String valor : ofertas) {
            if (valor != null && !valor.isBlank()) {
                elegidas.add(OfertaEducativa.valueOf(valor.trim()));
            }
        }
        return elegidas;
    }

    private static String texto(String valor) {
        if (valor == null) {
            return null;
        }
        String recortado = valor.trim();
        return recortado.isEmpty() ? null : recortado;
    }
}
