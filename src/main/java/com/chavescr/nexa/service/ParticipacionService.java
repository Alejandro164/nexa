package com.chavescr.nexa.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.MiembroProyecto;
import com.chavescr.nexa.entity.TareaProyecto;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.MiembroProyectoRepository;
import com.chavescr.nexa.repository.TareaProyectoRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional(readOnly = true)
public class ParticipacionService {

    private final UsuarioRepository usuarioRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final TareaProyectoRepository tareaProyectoRepository;

    public ParticipacionService(UsuarioRepository usuarioRepository,
                                MiembroProyectoRepository miembroProyectoRepository,
                                TareaProyectoRepository tareaProyectoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.tareaProyectoRepository = tareaProyectoRepository;
    }

    public List<Map<String, Object>> listarParticipantes(Long institucionId, String filtro,
                                                          String rol, String estado) {
        List<Usuario> usuarios = usuarioRepository.findActivosByInstitucionId(institucionId);
        if (filtro != null && !filtro.isBlank()) {
            String f = filtro.toLowerCase();
            usuarios = usuarios.stream()
                    .filter(u -> u.getNombre().toLowerCase().contains(f)
                            || u.getEmail().toLowerCase().contains(f))
                    .toList();
        }
        if (rol != null && !rol.isBlank()) {
            usuarios = usuarios.stream()
                    .filter(u -> u.getRoles().stream().anyMatch(r -> r.getNombre().equalsIgnoreCase(rol)))
                    .toList();
        }
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Usuario u : usuarios) {
            resultado.add(calcularStatsUsuario(u));
        }
        if (estado != null && !estado.isBlank()) {
            if ("alto".equalsIgnoreCase(estado)) {
                resultado = resultado.stream().filter(m -> toDouble(m.get("tasaCumplimiento")) >= 70).toList();
            } else if ("medio".equalsIgnoreCase(estado)) {
                resultado = resultado.stream().filter(m -> {
                    double t = toDouble(m.get("tasaCumplimiento"));
                    return t >= 40 && t < 70;
                }).toList();
            } else if ("bajo".equalsIgnoreCase(estado)) {
                resultado = resultado.stream().filter(m -> toDouble(m.get("tasaCumplimiento")) < 40).toList();
            }
        }
        return resultado;
    }

    public Map<String, Object> obtenerDetalle(Long institucionId, Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        Map<String, Object> detalle = calcularStatsUsuario(usuario);

        List<MiembroProyecto> membresias = miembroProyectoRepository.findByUsuarioId(usuarioId);
        List<TareaProyecto> todasTareas = new ArrayList<>();
        for (MiembroProyecto mp : membresias) {
            todasTareas.addAll(tareaProyectoRepository.findByMiembroIdOrderByFechaLimiteAsc(mp.getId()));
        }

        List<Map<String, Object>> tareasList = todasTareas.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("titulo", t.getTitulo());
            m.put("estado", t.getEstado().name());
            m.put("fechaLimite", t.getFechaLimite());
            m.put("calificacion", t.getCalificacion());
            m.put("proyecto", t.getProyecto().getNombre());
            m.put("fechaCompletada", t.getFechaCompletada());
            return m;
        }).toList();

        List<Map<String, Object>> proyectos = membresias.stream().map(mp -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", mp.getProyecto().getId());
            m.put("nombre", mp.getProyecto().getNombre());
            m.put("rol", mp.getRol().name());
            m.put("fechaAsignacion", mp.getFechaAsignacion());
            return m;
        }).toList();

        List<Map<String, Object>> historial = todasTareas.stream()
                .filter(t -> t.getFechaCompletada() != null || t.getCalificacion() != null)
                .map(t -> {
                    Map<String, Object> h = new LinkedHashMap<>();
                    h.put("tipo", "Tarea de proyecto");
                    h.put("titulo", t.getTitulo());
                    h.put("detalle", t.getEstado() == TareaProyecto.EstadoTarea.COMPLETADA
                            ? "Completada" : "Calificada: " + t.getCalificacion());
                    h.put("fecha", t.getFechaCompletada() != null ? t.getFechaCompletada() : t.getFechaAsignacion());
                    return h;
                })
                .sorted(Comparator.<Map<String, Object>, java.time.LocalDateTime>comparing(
                        m -> (java.time.LocalDateTime) m.get("fecha"),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10).toList();

        detalle.put("tareas", tareasList);
        detalle.put("proyectos", proyectos);
        detalle.put("historial", historial);
        return detalle;
    }

    private Map<String, Object> calcularStatsUsuario(Usuario u) {
        List<MiembroProyecto> membresias = miembroProyectoRepository.findByUsuarioId(u.getId());
        int totalTareas = 0, completadas = 0;
        double sumaNotas = 0;
        int conNota = 0;

        for (MiembroProyecto mp : membresias) {
            List<TareaProyecto> tareas = tareaProyectoRepository.findByMiembroIdOrderByFechaLimiteAsc(mp.getId());
            for (TareaProyecto t : tareas) {
                totalTareas++;
                if (t.getEstado() == TareaProyecto.EstadoTarea.COMPLETADA) completadas++;
                if (t.getCalificacion() != null && t.getEstado() == TareaProyecto.EstadoTarea.COMPLETADA) {
                    sumaNotas += t.getCalificacion();
                    conNota++;
                }
            }
        }

        String rol = u.getRoles().stream().findFirst()
                .map(r -> r.getNombre().replace("ROLE_", "")).orElse("Sin rol");

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("usuarioId", u.getId());
        stats.put("nombre", u.getNombre());
        stats.put("email", u.getEmail());
        stats.put("rol", rol);
        stats.put("activo", u.getActivo());
        stats.put("totalTareas", totalTareas);
        stats.put("completadas", completadas);
        stats.put("tasaCumplimiento", totalTareas == 0 ? 0
                : Math.round((double) completadas / totalTareas * 100.0));
        stats.put("promedioCalificacion", conNota == 0 ? null
                : Math.round(sumaNotas / conNota * 10.0) / 10.0);
        stats.put("proyectosCount", membresias.size());
        return stats;
    }

    private double toDouble(Object val) {
        if (val instanceof Number n) return n.doubleValue();
        return 0;
    }
}
