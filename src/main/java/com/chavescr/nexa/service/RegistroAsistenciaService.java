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

    public RegistroAsistencia registrar(Long usuarioId, RegistroAsistencia.TipoRegistro tipo,
            String observaciones, Long institucionId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Institucion institucion = institucionRepository.findById(institucionId)
                .orElseThrow(() -> new RuntimeException("Institución no encontrada"));

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
        List<RegistroAsistencia> registros = registroAsistenciaRepository
                .findByInstitucionIdAndFechaHoraBetweenOrderByFechaHoraDesc(institucionId, inicio, fin);

        Map<Long, RegistroAsistencia.TipoRegistro> ultimoPorUsuario = new HashMap<>();
        for (RegistroAsistencia r : registros) {
            ultimoPorUsuario.putIfAbsent(r.getUsuario().getId(), r.getTipo());
        }

        long presentes = ultimoPorUsuario.values().stream()
                .filter(t -> t == RegistroAsistencia.TipoRegistro.ENTRADA).count();

        Map<String, Long> conteo = new HashMap<>();
        conteo.put("presentes", presentes);
        conteo.put("totalRegistros", (long) registros.size());
        return conteo;
    }
}
