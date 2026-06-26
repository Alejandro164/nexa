package com.chavescr.nexa.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.entity.MiembroProyecto;
import com.chavescr.nexa.entity.Proyecto;
import com.chavescr.nexa.entity.TareaProyecto;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.InstitucionRepository;
import com.chavescr.nexa.repository.MiembroProyectoRepository;
import com.chavescr.nexa.repository.ProyectoRepository;
import com.chavescr.nexa.repository.TareaProyectoRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class ProyectoService {

    private static final Logger log = LoggerFactory.getLogger(ProyectoService.class);

    private final ProyectoRepository proyectoRepository;
    private final MiembroProyectoRepository miembroRepository;
    private final TareaProyectoRepository tareaRepository;
    private final InstitucionRepository institucionRepository;
    private final UsuarioRepository usuarioRepository;

    public ProyectoService(ProyectoRepository proyectoRepository,
                           MiembroProyectoRepository miembroRepository,
                           TareaProyectoRepository tareaRepository,
                           InstitucionRepository institucionRepository,
                           UsuarioRepository usuarioRepository) {
        this.proyectoRepository = proyectoRepository;
        this.miembroRepository = miembroRepository;
        this.tareaRepository = tareaRepository;
        this.institucionRepository = institucionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<Proyecto> listarProyectos(Long institucionId) {
        return proyectoRepository.findByInstitucionIdOrderByFechaCreacionDesc(institucionId);
    }

    @Transactional(readOnly = true)
    public Proyecto obtenerProyecto(Long institucionId, Long id) {
        return proyectoRepository.findByIdAndInstitucionId(id, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<Proyecto> buscarProyectos(Long institucionId, String filtro) {
        return proyectoRepository.findByInstitucionIdOrderByFechaCreacionDesc(institucionId)
                .stream()
                .filter(p -> p.getNombre().toLowerCase().contains(filtro.toLowerCase()))
                .toList();
    }

    public Proyecto guardarProyecto(Long institucionId, Proyecto datos) {
        if (datos.getNombre() == null || datos.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del proyecto es obligatorio");
        }
        if (datos.getFechaFin().isBefore(datos.getFechaInicio())) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la fecha inicial");
        }
        Proyecto proyecto = datos.getId() == null
                ? new Proyecto()
                : obtenerProyecto(institucionId, datos.getId());
        proyecto.setNombre(datos.getNombre().trim());
        proyecto.setDescripcion(datos.getDescripcion() != null ? datos.getDescripcion().trim() : null);
        proyecto.setObjetivo(datos.getObjetivo() != null ? datos.getObjetivo().trim() : "");
        proyecto.setFechaInicio(datos.getFechaInicio());
        proyecto.setFechaFin(datos.getFechaFin());
        proyecto.setInstitucion(obtenerInstitucion(institucionId));
        if (datos.getEstado() != null) {
            proyecto.setEstado(datos.getEstado());
        }
        Proyecto guardado = proyectoRepository.save(proyecto);
        log.info("Proyecto guardado: id={}, nombre={}", guardado.getId(), guardado.getNombre());
        return guardado;
    }

    public void eliminarProyecto(Long institucionId, Long id) {
        Proyecto proyecto = obtenerProyecto(institucionId, id);
        miembroRepository.deleteByProyectoId(id);
        proyectoRepository.delete(proyecto);
        log.info("Proyecto eliminado: id={}", id);
    }

    public void toggleActivoProyecto(Long institucionId, Long id) {
        Proyecto proyecto = obtenerProyecto(institucionId, id);
        proyecto.setActivo(!proyecto.getActivo());
        proyectoRepository.save(proyecto);
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarPersonalActivo(Long institucionId) {
        return usuarioRepository.findActivosByInstitucionId(institucionId);
    }

    public void sincronizarMiembrosFormulario(Long institucionId, Long proyectoId, List<Long> usuarioIds) {
        if (usuarioIds == null || usuarioIds.isEmpty()) return;
        for (Long uid : usuarioIds) {
            if (!miembroRepository.existsByProyectoIdAndUsuarioId(proyectoId, uid)) {
                agregarMiembro(institucionId, proyectoId, uid, "MIEMBRO");
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Long> listarIdsMiembros(Long proyectoId) {
        return listarMiembros(proyectoId).stream()
                .map(m -> m.getUsuario().getId()).toList();
    }

    @Transactional(readOnly = true)
    public List<MiembroProyecto> listarMiembros(Long proyectoId) {
        return miembroRepository.findByProyectoIdOrderByFechaAsignacionDesc(proyectoId);
    }

    public MiembroProyecto agregarMiembro(Long institucionId, Long proyectoId, Long usuarioId, String rol) {
        Proyecto proyecto = obtenerProyecto(institucionId, proyectoId);
        if (miembroRepository.existsByProyectoIdAndUsuarioId(proyectoId, usuarioId)) {
            throw new IllegalArgumentException("El usuario ya es miembro del proyecto");
        }
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        MiembroProyecto miembro = new MiembroProyecto();
        miembro.setProyecto(proyecto);
        miembro.setUsuario(usuario);
        miembro.setRol(rol != null ? MiembroProyecto.RolProyecto.valueOf(rol) : MiembroProyecto.RolProyecto.MIEMBRO);
        MiembroProyecto guardado = miembroRepository.save(miembro);
        log.info("Miembro agregado al proyecto: miembroId={}, proyectoId={}", guardado.getId(), proyectoId);
        return guardado;
    }

    public void eliminarMiembro(Long institucionId, Long proyectoId, Long miembroId) {
        MiembroProyecto miembro = miembroRepository.findByIdAndProyectoId(miembroId, proyectoId)
                .orElseThrow(() -> new IllegalArgumentException("Miembro no encontrado"));
        tareaRepository.deleteByMiembroId(miembroId);
        miembroRepository.delete(miembro);
        log.info("Miembro eliminado: miembroId={}", miembroId);
    }

    @Transactional(readOnly = true)
    public List<TareaProyecto> listarTareasDeMiembro(Long miembroId) {
        return tareaRepository.findByMiembroIdOrderByFechaLimiteAsc(miembroId);
    }

    @Transactional(readOnly = true)
    public TareaProyecto obtenerTarea(Long proyectoId, Long tareaId) {
        return tareaRepository.findByIdAndProyectoId(tareaId, proyectoId)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada"));
    }

    public TareaProyecto guardarTarea(Long proyectoId, Long miembroId, TareaProyecto datos) {
        if (datos.getTitulo() == null || datos.getTitulo().isBlank()) {
            throw new IllegalArgumentException("El título es obligatorio");
        }
        MiembroProyecto miembro = miembroRepository.findByIdAndProyectoId(miembroId, proyectoId)
                .orElseThrow(() -> new IllegalArgumentException("Miembro no encontrado"));
        Proyecto proyecto = miembro.getProyecto();
        TareaProyecto tarea = datos.getId() == null ? new TareaProyecto() : obtenerTarea(proyectoId, datos.getId());
        tarea.setTitulo(datos.getTitulo().trim());
        tarea.setDescripcion(datos.getDescripcion() != null ? datos.getDescripcion().trim() : null);
        tarea.setFechaLimite(datos.getFechaLimite());
        tarea.setCalificacion(datos.getCalificacion());
        tarea.setMiembro(miembro);
        tarea.setProyecto(proyecto);
        if (datos.getEstado() != null) {
            tarea.setEstado(datos.getEstado());
            if (datos.getEstado() == TareaProyecto.EstadoTarea.COMPLETADA && tarea.getFechaCompletada() == null) {
                tarea.setFechaCompletada(LocalDateTime.now());
            }
        }
        TareaProyecto guardada = tareaRepository.save(tarea);
        log.info("Tarea guardada: id={}, titulo={}", guardada.getId(), guardada.getTitulo());
        return guardada;
    }

    public void cambiarEstadoTarea(Long proyectoId, Long tareaId, TareaProyecto.EstadoTarea nuevoEstado) {
        TareaProyecto tarea = obtenerTarea(proyectoId, tareaId);
        tarea.setEstado(nuevoEstado);
        if (nuevoEstado == TareaProyecto.EstadoTarea.COMPLETADA) {
            tarea.setFechaCompletada(LocalDateTime.now());
        } else {
            tarea.setFechaCompletada(null);
        }
        tareaRepository.save(tarea);
    }

    public void eliminarTarea(Long proyectoId, Long tareaId) {
        TareaProyecto tarea = obtenerTarea(proyectoId, tareaId);
        tareaRepository.delete(tarea);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerDashboard(Long institucionId, Long proyectoId) {
        Proyecto proyecto = obtenerProyecto(institucionId, proyectoId);
        List<MiembroProyecto> miembros = miembroRepository.findByProyectoIdOrderByFechaAsignacionDesc(proyectoId);

        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("proyectoNombre", proyecto.getNombre());
        dashboard.put("objetivo", proyecto.getObjetivo());
        dashboard.put("estado", proyecto.getEstado().name());
        dashboard.put("fechaInicio", proyecto.getFechaInicio());
        dashboard.put("fechaFin", proyecto.getFechaFin());
        dashboard.put("totalMiembros", miembros.size());

        int totalTareas = 0;
        int tareasCompletadas = 0;
        int tareasVencidas = 0;
        double sumaCalificaciones = 0;
        int tareasConCalificacion = 0;
        int tareasATiempo = 0;
        int tareasTarde = 0;

        List<Map<String, Object>> rendimientoMiembros = new ArrayList<>();
        for (MiembroProyecto miembro : miembros) {
            List<TareaProyecto> tareas = tareaRepository.findByMiembroIdOrderByFechaLimiteAsc(miembro.getId());
            int completadas = 0, vencidas = 0, pendientes = 0, enProgreso = 0;
            double sumaNotas = 0;
            int conNota = 0, aTiempo = 0, tarde = 0;

            for (TareaProyecto t : tareas) {
                totalTareas++;
                switch (t.getEstado()) {
                    case COMPLETADA -> {
                        completadas++; tareasCompletadas++;
                        if (t.getFechaCompletada() != null && !t.getFechaCompletada().toLocalDate().isAfter(t.getFechaLimite())) {
                            aTiempo++; tareasATiempo++;
                        } else { tarde++; tareasTarde++; }
                    }
                    case VENCIDA -> { vencidas++; tareasVencidas++; tarde++; tareasTarde++; }
                    case EN_PROGRESO -> enProgreso++;
                    default -> pendientes++;
                }
                if (t.getCalificacion() != null && t.getEstado() == TareaProyecto.EstadoTarea.COMPLETADA) {
                    sumaNotas += t.getCalificacion(); conNota++;
                    sumaCalificaciones += t.getCalificacion(); tareasConCalificacion++;
                }
            }

            Map<String, Object> rm = new LinkedHashMap<>();
            rm.put("miembroId", miembro.getId());
            rm.put("nombre", miembro.getUsuario().getNombre());
            rm.put("email", miembro.getUsuario().getEmail());
            rm.put("rol", miembro.getRol().name());
            String rolUsuario = miembro.getUsuario().getRoles().isEmpty() ? "Sin rol"
                    : miembro.getUsuario().getRoles().iterator().next().getNombre().replace("ROLE_", "");
            rm.put("rolUsuario", rolUsuario);
            rm.put("totalTareas", tareas.size());
            rm.put("completadas", completadas);
            rm.put("enProgreso", enProgreso);
            rm.put("pendientes", pendientes);
            rm.put("vencidas", vencidas);
            rm.put("tasaCumplimiento", tareas.isEmpty() ? 0 : Math.round((double) completadas / tareas.size() * 100.0));
            rm.put("promedioCalificacion", conNota == 0 ? null : Math.round(sumaNotas / conNota * 10.0) / 10.0);
            rm.put("tareasATiempo", aTiempo);
            rm.put("tareasTarde", tarde);
            rendimientoMiembros.add(rm);
        }

        dashboard.put("totalTareas", totalTareas);
        dashboard.put("tareasCompletadas", tareasCompletadas);
        dashboard.put("tasaCumplimientoGlobal", totalTareas == 0 ? 0 : Math.round((double) tareasCompletadas / totalTareas * 100.0));
        dashboard.put("promedioGeneral", tareasConCalificacion == 0 ? null : Math.round(sumaCalificaciones / tareasConCalificacion * 10.0) / 10.0);
        dashboard.put("tareasATiempo", tareasATiempo);
        dashboard.put("tareasTarde", tareasTarde);
        dashboard.put("tasaEficiencia", totalTareas == 0 ? 0 : Math.round((double) tareasATiempo / totalTareas * 100.0));
        dashboard.put("miembros", rendimientoMiembros);
        return dashboard;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarUsuariosDisponibles(Long institucionId, Long proyectoId) {
        List<Long> idsMiembros = miembroRepository.findByProyectoIdOrderByFechaAsignacionDesc(proyectoId)
                .stream().map(m -> m.getUsuario().getId()).toList();
        return usuarioRepository.findActivosByInstitucionId(institucionId).stream()
                .filter(u -> !idsMiembros.contains(u.getId())).toList();
    }

    private Institucion obtenerInstitucion(Long institucionId) {
        return institucionRepository.findById(institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Institución no encontrada"));
    }
}
