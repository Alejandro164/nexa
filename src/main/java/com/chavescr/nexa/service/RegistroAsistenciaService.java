package com.chavescr.nexa.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.entity.RegistroAsistencia;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.InstitucionRepository;
import com.chavescr.nexa.repository.RegistroAsistenciaRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
public class RegistroAsistenciaService {

    @Autowired
    private RegistroAsistenciaRepository registroAsistenciaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private InstitucionRepository institucionRepository;

    public List<RegistroAsistencia> obtenerRegistrosDelDia(Long institucionId) {
        LocalDateTime inicio = LocalDate.now().atStartOfDay();
        LocalDateTime fin = LocalDate.now().atTime(LocalTime.MAX);
        return registroAsistenciaRepository.findByInstitucionIdAndFechaHoraBetweenOrderByFechaHoraDesc(
                institucionId, inicio, fin);
    }

    private static final List<String> ROLES_STAFF = List.of("ROLE_ADMIN", "ROLE_DIRECTOR", "ROLE_DOCENTE");

    public RegistroAsistencia registrar(Long usuarioId, RegistroAsistencia.TipoRegistro tipo,
            String observaciones, Long institucionId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Institucion institucion = institucionRepository.findById(institucionId)
                .orElseThrow(() -> new RuntimeException("Institución no encontrada"));

        List<RegistroAsistencia> anteriores = registroAsistenciaRepository
                .findByUsuarioIdAndInstitucionIdOrderByFechaHoraDesc(usuarioId, institucionId);
        if (!anteriores.isEmpty() && anteriores.get(0).getTipo() == tipo) {
            String mensaje = tipo == RegistroAsistencia.TipoRegistro.ENTRADA
                    ? "Ya hay una entrada registrada sin salida — registrá la salida primero."
                    : "Ya hay una salida registrada sin una entrada nueva — registrá la entrada primero.";
            throw new IllegalStateException(mensaje);
        }

        RegistroAsistencia registro = new RegistroAsistencia();
        registro.setUsuario(usuario);
        registro.setInstitucion(institucion);
        registro.setTipo(tipo);
        registro.setObservaciones(observaciones);
        return registroAsistenciaRepository.save(registro);
    }

    public Map<String, Long> obtenerConteoPersonalPresente(Long institucionId) {
        LocalDateTime inicio = LocalDate.now().atStartOfDay();
        LocalDateTime fin = LocalDate.now().atTime(LocalTime.MAX);
        List<RegistroAsistencia> registrosDelDia = registroAsistenciaRepository
                .findByInstitucionIdAndFechaHoraBetweenOrderByFechaHoraDesc(institucionId, inicio, fin);
        List<RegistroAsistencia> registrosStaff = registroAsistenciaRepository
                .findByInstitucionIdAndFechaHoraBetweenAndRolesOrderByFechaHoraDesc(institucionId, inicio, fin,
                        ROLES_STAFF);

        Map<Long, RegistroAsistencia.TipoRegistro> ultimoPorUsuario = new HashMap<>();
        for (RegistroAsistencia r : registrosStaff) {
            ultimoPorUsuario.putIfAbsent(r.getUsuario().getId(), r.getTipo());
        }

        long presentes = ultimoPorUsuario.values().stream()
                .filter(t -> t == RegistroAsistencia.TipoRegistro.ENTRADA).count();

        Map<String, Long> conteo = new HashMap<>();
        conteo.put("presentes", presentes);
        conteo.put("totalRegistros", (long) registrosDelDia.size());
        return conteo;
    }
}
