package com.chavescr.nexa.service;

import java.text.Normalizer;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.ContactoExterno;
import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.repository.ContactoExternoRepository;
import com.chavescr.nexa.repository.InstitucionRepository;

@Service
@Transactional
public class ContactoExternoService {

    private static final Logger log = LoggerFactory.getLogger(ContactoExternoService.class);

    private final ContactoExternoRepository contactoExternoRepository;
    private final InstitucionRepository institucionRepository;

    public ContactoExternoService(ContactoExternoRepository contactoExternoRepository,
            InstitucionRepository institucionRepository) {
        this.contactoExternoRepository = contactoExternoRepository;
        this.institucionRepository = institucionRepository;
    }

    @Transactional(readOnly = true)
    public List<ContactoExterno> listar(Long institucionId, String filtro) {
        List<ContactoExterno> todos = contactoExternoRepository.findByInstitucionIdAndActivoTrueOrderByNombreAsc(institucionId);
        if (filtro == null || filtro.isBlank()) {
            return todos;
        }
        String f = normalizar(filtro.trim());
        return todos.stream()
                .filter(c -> normalizar(c.getNombre()).contains(f))
                .toList();
    }

    private String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase();
    }

    @Transactional(readOnly = true)
    public ContactoExterno obtenerPorId(Long institucionId, Long id) {
        return contactoExternoRepository.findByIdAndInstitucionId(id, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Contacto no encontrado"));
    }

    public ContactoExterno guardar(Long institucionId, Long id, String nombre, String tipo, String direccion,
            String telefono, String email, String sitioWeb, boolean activo) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (tipo == null || tipo.isBlank()) {
            throw new IllegalArgumentException("El tipo es obligatorio");
        }

        ContactoExterno contacto = id == null ? new ContactoExterno() : obtenerPorId(institucionId, id);
        if (contacto.getId() == null) {
            Institucion institucion = institucionRepository.findById(institucionId)
                    .orElseThrow(() -> new IllegalArgumentException("Institución no encontrada"));
            contacto.setInstitucion(institucion);
        }
        contacto.setNombre(nombre.trim());
        contacto.setTipo(tipo.trim());
        contacto.setDireccion(direccion != null && !direccion.isBlank() ? direccion.trim() : null);
        contacto.setTelefono(telefono != null && !telefono.isBlank() ? telefono.trim() : null);
        contacto.setEmail(email != null && !email.isBlank() ? email.trim() : null);
        contacto.setSitioWeb(sitioWeb != null && !sitioWeb.isBlank() ? sitioWeb.trim() : null);
        contacto.setActivo(activo);

        ContactoExterno guardado = contactoExternoRepository.save(contacto);
        log.info("Contacto externo guardado: id={}, nombre={}", guardado.getId(), guardado.getNombre());
        return guardado;
    }

    public void eliminar(Long institucionId, Long id) {
        ContactoExterno contacto = obtenerPorId(institucionId, id);
        contactoExternoRepository.delete(contacto);
        log.info("Contacto externo eliminado: id={}", id);
    }
}
