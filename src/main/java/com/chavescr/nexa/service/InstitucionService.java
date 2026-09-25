package com.chavescr.nexa.service;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.OfertaEducativa;
import com.chavescr.nexa.repository.InstitucionRepository;
import com.chavescr.nexa.repository.DireccionRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class InstitucionService {

    private final InstitucionRepository institucionRepository;
    private final DireccionRepository direccionRepository;
    private final UsuarioRepository usuarioRepository;
    private final NivelAcademicoRepository nivelAcademicoRepository;

    public InstitucionService(InstitucionRepository institucionRepository, DireccionRepository direccionRepository,
            UsuarioRepository usuarioRepository, NivelAcademicoRepository nivelAcademicoRepository) {
        this.institucionRepository = institucionRepository;
        this.direccionRepository = direccionRepository;
        this.usuarioRepository = usuarioRepository;
        this.nivelAcademicoRepository = nivelAcademicoRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<Institucion> listar() {
        if (direccionRepository.existsByInstitucionIsNull()) {
            asegurarInstituciones();
        }
        return institucionRepository.findAllConDirecciones();
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Institucion obtener(Long id) {
        return institucionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Institución no encontrada"));
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<Direccion> ofertas(Long institucionId) {
        return direccionRepository.findByInstitucionId(institucionId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void asegurarInstituciones() {
        for (Direccion direccion : direccionRepository.findByInstitucionIsNull()) {
            Institucion institucion = new Institucion();
            institucion.setNombre(direccion.getNombre());
            institucion.setCedula(cedulaLibre(texto(direccion.getCedula())));
            institucion.setDireccion(direccion.getDireccion());
            institucion.setTelefono(direccion.getTelefono());
            institucion.setEmail(direccion.getEmail());
            institucion.setActiva(direccion.getActiva() == null || direccion.getActiva());
            institucionRepository.save(institucion);
            direccion.setInstitucion(institucion);
            direccionRepository.save(direccion);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Institucion guardar(Long institucionId, String cedula, String nombre, String direccion, String telefono,
            String email, boolean activa, List<String> ofertas, Map<String, String> codigos) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la institución es obligatorio.");
        }
        LinkedHashSet<OfertaEducativa> elegidas = ofertasSeleccionadas(ofertas);
        if (elegidas.isEmpty()) {
            throw new IllegalArgumentException("Seleccione al menos una dirección: Preescolar, Primaria o Secundaria.");
        }

        Institucion institucion = institucionId == null ? new Institucion() : obtener(institucionId);
        String cedulaNormalizada = texto(cedula);
        if (cedulaNormalizada != null) {
            institucionRepository.findByCedula(cedulaNormalizada)
                    .filter(existente -> institucion.getId() == null || !existente.getId().equals(institucion.getId()))
                    .ifPresent(existente -> {
                        throw new IllegalArgumentException("Ya existe una institución con esa cédula.");
                    });
        }
        institucion.setCedula(cedulaNormalizada);
        institucion.setNombre(nombre.trim());
        institucion.setDireccion(texto(direccion));
        institucion.setTelefono(texto(telefono));
        institucion.setEmail(texto(email));
        institucion.setActiva(activa);
        institucionRepository.save(institucion);

        List<Direccion> actuales = direccionRepository.findByInstitucionId(institucion.getId());
        Direccion sinClasificar = actuales.stream().filter(inst -> inst.getOferta() == null).findFirst().orElse(null);
        Set<Long> conservadas = new HashSet<>();

        for (OfertaEducativa oferta : OfertaEducativa.values()) {
            if (!elegidas.contains(oferta)) {
                continue;
            }
            String codigo = texto(codigos.get(oferta.name()));
            if (codigo == null) {
                throw new IllegalArgumentException("El código presupuestario de " + oferta.getEtiqueta() + " es obligatorio.");
            }
            Direccion registro = actuales.stream()
                    .filter(inst -> oferta == inst.getOferta())
                    .findFirst()
                    .orElse(null);
            if (registro == null) {
                registro = sinClasificar;
                sinClasificar = null;
            }
            if (registro == null) {
                registro = new Direccion(institucion.getNombre(), codigo);
            }
            validarCodigoLibre(codigo, registro.getId());
            copiarInstitucion(registro, institucion, oferta, codigo);
            direccionRepository.save(registro);
            conservadas.add(registro.getId());
        }

        for (Direccion registro : actuales) {
            if (conservadas.contains(registro.getId())) {
                continue;
            }
            if (tieneDatos(registro.getId())) {
                String cual = registro.getOferta() == null ? "la dirección actual" : registro.getOferta().getEtiqueta();
                throw new IllegalArgumentException("No se puede quitar " + cual + " porque ya tiene personas o secciones.");
            }
            direccionRepository.delete(registro);
        }
        return institucion;
    }

    @Transactional(rollbackFor = Exception.class)
    public void eliminar(Long institucionId) {
        Institucion institucion = obtener(institucionId);
        for (Direccion direccion : direccionRepository.findByInstitucionId(institucionId)) {
            if (tieneDatos(direccion.getId())) {
                throw new IllegalArgumentException("No se puede eliminar la institución porque " + direccion.getPresentacion()
                        + " ya tiene personas o secciones.");
            }
            direccionRepository.delete(direccion);
        }
        institucionRepository.delete(institucion);
    }

    private void copiarInstitucion(Direccion direccion, Institucion institucion, OfertaEducativa oferta, String codigo) {
        direccion.setInstitucion(institucion);
        direccion.setOferta(oferta);
        direccion.setNombre(institucion.getNombre());
        direccion.setCodigo(codigo);
        direccion.setDireccion(institucion.getDireccion());
        direccion.setTelefono(institucion.getTelefono());
        direccion.setEmail(institucion.getEmail());
        direccion.setActiva(institucion.getActiva());
    }

    private boolean tieneDatos(Long direccionId) {
        return usuarioRepository.countByDireccionId(direccionId) > 0
                || nivelAcademicoRepository.existsByDireccionId(direccionId);
    }

    private void validarCodigoLibre(String codigo, Long direccionId) {
        direccionRepository.findByCodigo(codigo)
                .filter(existente -> direccionId == null || !existente.getId().equals(direccionId))
                .ifPresent(existente -> {
                    throw new IllegalArgumentException("El código " + codigo + " ya está en uso.");
                });
    }

    private String cedulaLibre(String cedula) {
        if (cedula == null || institucionRepository.findByCedula(cedula).isPresent()) {
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
