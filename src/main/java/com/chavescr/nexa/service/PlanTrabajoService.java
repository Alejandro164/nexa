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

import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.MiembroPlanTrabajo;
import com.chavescr.nexa.entity.PlanTrabajo;
import com.chavescr.nexa.entity.TareaPlanTrabajo;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.DireccionRepository;
import com.chavescr.nexa.repository.MiembroPlanTrabajoRepository;
import com.chavescr.nexa.repository.PlanTrabajoRepository;
import com.chavescr.nexa.repository.TareaPlanTrabajoRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class PlanTrabajoService {

    private static final Logger log = LoggerFactory.getLogger(PlanTrabajoService.class);

    private final PlanTrabajoRepository planTrabajoRepository;
    private final MiembroPlanTrabajoRepository miembroRepository;
    private final TareaPlanTrabajoRepository tareaRepository;
    private final DireccionRepository direccionRepository;
    private final UsuarioRepository usuarioRepository;

    public PlanTrabajoService(PlanTrabajoRepository planTrabajoRepository,
                           MiembroPlanTrabajoRepository miembroRepository,
                           TareaPlanTrabajoRepository tareaRepository,
                           DireccionRepository direccionRepository,
                           UsuarioRepository usuarioRepository) {
        this.planTrabajoRepository = planTrabajoRepository;
        this.miembroRepository = miembroRepository;
        this.tareaRepository = tareaRepository;
        this.direccionRepository = direccionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<PlanTrabajo> listarPlanesTrabajo(Long direccionId) {
        return planTrabajoRepository.findByDireccionIdOrderByFechaCreacionDesc(direccionId);
    }

    @Transactional(readOnly = true)
    public PlanTrabajo obtenerPlanTrabajo(Long direccionId, Long id) {
        return planTrabajoRepository.findByIdAndDireccionId(id, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Plan de trabajo no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<PlanTrabajo> buscarPlanesTrabajo(Long direccionId, String filtro) {
        return planTrabajoRepository.findByDireccionIdOrderByFechaCreacionDesc(direccionId)
                .stream()
                .filter(p -> p.getNombre().toLowerCase().contains(filtro.toLowerCase()))
                .toList();
    }

    public PlanTrabajo guardarPlanTrabajo(Long direccionId, PlanTrabajo datos) {
        if (datos.getNombre() == null || datos.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del plan de trabajo es obligatorio");
        }
        if (datos.getFechaFin().isBefore(datos.getFechaInicio())) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la fecha inicial");
        }
        PlanTrabajo planTrabajo = datos.getId() == null
                ? new PlanTrabajo()
                : obtenerPlanTrabajo(direccionId, datos.getId());
        planTrabajo.setNombre(datos.getNombre().trim());
        planTrabajo.setDescripcion(datos.getDescripcion() != null ? datos.getDescripcion().trim() : null);
        planTrabajo.setObjetivo(datos.getObjetivo() != null ? datos.getObjetivo().trim() : "");
        planTrabajo.setFechaInicio(datos.getFechaInicio());
        planTrabajo.setFechaFin(datos.getFechaFin());
        planTrabajo.setDireccion(obtenerDireccion(direccionId));
        if (datos.getEstado() != null) {
            planTrabajo.setEstado(datos.getEstado());
        }
        PlanTrabajo guardado = planTrabajoRepository.save(planTrabajo);
        log.info("Plan de trabajo guardado: id={}, nombre={}", guardado.getId(), guardado.getNombre());
        return guardado;
    }

    public void eliminarPlanTrabajo(Long direccionId, Long id) {
        PlanTrabajo planTrabajo = obtenerPlanTrabajo(direccionId, id);
        miembroRepository.deleteByPlanTrabajoId(id);
        planTrabajoRepository.delete(planTrabajo);
        log.info("Plan de trabajo eliminado: id={}", id);
    }

    public void toggleActivoPlanTrabajo(Long direccionId, Long id) {
        PlanTrabajo planTrabajo = obtenerPlanTrabajo(direccionId, id);
        planTrabajo.setActivo(!planTrabajo.getActivo());
        planTrabajoRepository.save(planTrabajo);
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarPersonalActivo(Long direccionId) {
        return usuarioRepository.findActivosByDireccionId(direccionId);
    }

    public void sincronizarMiembrosFormulario(Long direccionId, Long planTrabajoId, List<Long> usuarioIds) {
        if (usuarioIds == null || usuarioIds.isEmpty()) return;
        for (Long uid : usuarioIds) {
            if (!miembroRepository.existsByPlanTrabajoIdAndUsuarioId(planTrabajoId, uid)) {
                agregarMiembro(direccionId, planTrabajoId, uid, "MIEMBRO");
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Long> listarIdsMiembros(Long planTrabajoId) {
        return listarMiembros(planTrabajoId).stream()
                .map(m -> m.getUsuario().getId()).toList();
    }

    @Transactional(readOnly = true)
    public List<MiembroPlanTrabajo> listarMiembros(Long planTrabajoId) {
        return miembroRepository.findByPlanTrabajoIdOrderByFechaAsignacionDesc(planTrabajoId);
    }

    public MiembroPlanTrabajo agregarMiembro(Long direccionId, Long planTrabajoId, Long usuarioId, String rol) {
        PlanTrabajo planTrabajo = obtenerPlanTrabajo(direccionId, planTrabajoId);
        if (miembroRepository.existsByPlanTrabajoIdAndUsuarioId(planTrabajoId, usuarioId)) {
            throw new IllegalArgumentException("El usuario ya es miembro del plan de trabajo");
        }
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        MiembroPlanTrabajo miembro = new MiembroPlanTrabajo();
        miembro.setPlanTrabajo(planTrabajo);
        miembro.setUsuario(usuario);
        miembro.setRol(rol != null ? MiembroPlanTrabajo.RolPlanTrabajo.valueOf(rol) : MiembroPlanTrabajo.RolPlanTrabajo.MIEMBRO);
        MiembroPlanTrabajo guardado = miembroRepository.save(miembro);
        log.info("Miembro agregado al plan de trabajo: miembroId={}, planTrabajoId={}", guardado.getId(), planTrabajoId);
        return guardado;
    }

    public void eliminarMiembro(Long direccionId, Long planTrabajoId, Long miembroId) {
        MiembroPlanTrabajo miembro = miembroRepository.findByIdAndPlanTrabajoId(miembroId, planTrabajoId)
                .orElseThrow(() -> new IllegalArgumentException("Miembro no encontrado"));
        tareaRepository.deleteByMiembroId(miembroId);
        miembroRepository.delete(miembro);
        log.info("Miembro eliminado: miembroId={}", miembroId);
    }

    @Transactional(readOnly = true)
    public List<TareaPlanTrabajo> listarTareasDeMiembro(Long miembroId) {
        return tareaRepository.findByMiembroIdOrderByFechaLimiteAsc(miembroId);
    }

    @Transactional(readOnly = true)
    public MiembroPlanTrabajo obtenerMiembro(Long direccionId, Long miembroId) {
        MiembroPlanTrabajo miembro = miembroRepository.findById(miembroId)
                .orElseThrow(() -> new IllegalArgumentException("Miembro no encontrado"));
        if (!miembro.getPlanTrabajo().getDireccion().getId().equals(direccionId)) {
            throw new IllegalArgumentException("Miembro no encontrado");
        }
        return miembro;
    }

    @Transactional(readOnly = true)
    public TareaPlanTrabajo obtenerTarea(Long planTrabajoId, Long tareaId) {
        return tareaRepository.findByIdAndPlanTrabajoId(tareaId, planTrabajoId)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada"));
    }

    public void cambiarEstadoTarea(Long planTrabajoId, Long tareaId, TareaPlanTrabajo.EstadoTarea nuevoEstado) {
        TareaPlanTrabajo tarea = obtenerTarea(planTrabajoId, tareaId);
        tarea.setEstado(nuevoEstado);
        if (nuevoEstado == TareaPlanTrabajo.EstadoTarea.FINALIZADO) {
            tarea.setFechaCompletada(LocalDateTime.now());
        } else {
            tarea.setFechaCompletada(null);
        }
        tareaRepository.save(tarea);
    }

    public void eliminarTarea(Long planTrabajoId, Long tareaId) {
        TareaPlanTrabajo tarea = obtenerTarea(planTrabajoId, tareaId);
        tareaRepository.delete(tarea);
    }

    // ─── Tareas generales (vista agenda) ──────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TareaPlanTrabajo> listarTareasDireccion(Long direccionId) {
        return tareaRepository.findByDireccionIdOrderByFechaLimiteAsc(direccionId);
    }

    @Transactional(readOnly = true)
    public TareaPlanTrabajo obtenerTareaDireccion(Long direccionId, Long tareaId) {
        return tareaRepository.findByIdAndPlanTrabajo_Direccion_Id(tareaId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada"));
    }

    public TareaPlanTrabajo guardarTareaGeneral(Long direccionId, Long tareaId,
            Long planTrabajoId, Long usuarioId, Long miembroId, String titulo, String descripcion,
            java.time.LocalDate fechaLimite, TareaPlanTrabajo.EstadoTarea estado,
            TareaPlanTrabajo.PrioridadTarea prioridad) {
        TareaPlanTrabajo tarea;
        if (tareaId != null) {
            tarea = obtenerTareaDireccion(direccionId, tareaId);
            // Actualizar asignación si se proporcionó
            if (miembroId != null) {
                MiembroPlanTrabajo miembro = obtenerMiembro(direccionId, miembroId);
                tarea.setMiembro(miembro);
                tarea.setPlanTrabajo(miembro.getPlanTrabajo());
            } else if (planTrabajoId != null || usuarioId != null) {
                if (planTrabajoId != null) {
                    tarea.setPlanTrabajo(obtenerPlanTrabajo(direccionId, planTrabajoId));
                    tarea.setMiembro(null);
                } else {
                    PlanTrabajo planTrabajo = tarea.getPlanTrabajo(); // mantener plan de trabajo actual
                    Usuario usuario = usuarioRepository.findActivoByIdAndDireccionId(usuarioId, direccionId)
                            .orElseThrow(() -> new IllegalArgumentException("Personal no válido"));
                    MiembroPlanTrabajo miembro = miembroRepository
                            .findByPlanTrabajoIdOrderByFechaAsignacionDesc(planTrabajo.getId()).stream()
                            .filter(m -> m.getUsuario().getId().equals(usuarioId))
                            .findFirst()
                            .orElseGet(() -> {
                                MiembroPlanTrabajo nuevo = new MiembroPlanTrabajo();
                                nuevo.setPlanTrabajo(planTrabajo);
                                nuevo.setUsuario(usuario);
                                nuevo.setRol(MiembroPlanTrabajo.RolPlanTrabajo.MIEMBRO);
                                return miembroRepository.save(nuevo);
                            });
                    tarea.setMiembro(miembro);
                }
            }
        } else {
            if (planTrabajoId == null && usuarioId == null && miembroId == null)
                throw new IllegalArgumentException("Debes seleccionar un plan de trabajo o personal");
            tarea = new TareaPlanTrabajo();
            if (miembroId != null) {
                MiembroPlanTrabajo miembro = obtenerMiembro(direccionId, miembroId);
                tarea.setMiembro(miembro);
                tarea.setPlanTrabajo(miembro.getPlanTrabajo());
            } else if (planTrabajoId != null) {
                // Asignada a un plan de trabajo completo (sin miembro específico)
                PlanTrabajo planTrabajo = obtenerPlanTrabajo(direccionId, planTrabajoId);
                tarea.setPlanTrabajo(planTrabajo);
                tarea.setMiembro(null);
            } else {
                // Asignada a un usuario → busca o crea membresía en su primer plan de trabajo activo
                Usuario usuario = usuarioRepository.findActivosByDireccionId(direccionId).stream()
                        .filter(u -> u.getId().equals(usuarioId))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Personal no válido"));
                // Tomar el primer plan de trabajo activo donde participe, o el primero de la dirección
                PlanTrabajo planTrabajo = miembroRepository.findAll().stream()
                        .filter(m -> m.getUsuario().getId().equals(usuarioId))
                        .map(MiembroPlanTrabajo::getPlanTrabajo)
                        .filter(p -> p.getDireccion().getId().equals(direccionId))
                        .findFirst()
                        .orElseGet(() -> planTrabajoRepository
                                .findByDireccionIdOrderByFechaCreacionDesc(direccionId).stream()
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException("No hay planes de trabajo disponibles")));
                tarea.setPlanTrabajo(planTrabajo);
                MiembroPlanTrabajo miembro = miembroRepository
                        .findByPlanTrabajoIdOrderByFechaAsignacionDesc(planTrabajo.getId()).stream()
                        .filter(m -> m.getUsuario().getId().equals(usuarioId))
                        .findFirst()
                        .orElseGet(() -> {
                            MiembroPlanTrabajo nuevo = new MiembroPlanTrabajo();
                            nuevo.setPlanTrabajo(planTrabajo);
                            nuevo.setUsuario(usuario);
                            nuevo.setRol(MiembroPlanTrabajo.RolPlanTrabajo.MIEMBRO);
                            return miembroRepository.save(nuevo);
                        });
                tarea.setMiembro(miembro);
            }
        }
        tarea.setTitulo(titulo.trim());
        tarea.setDescripcion(descripcion != null ? descripcion.trim() : null);
        if (!fechaLimite.equals(tarea.getFechaLimite())) {
            tarea.setRecordatorioEnviado(false);
        }
        tarea.setFechaLimite(fechaLimite);
        tarea.setEstado(estado != null ? estado : TareaPlanTrabajo.EstadoTarea.PENDIENTE);
        if (!tarea.getEstado().isCerrado()) {
            tarea.setFechaCompletada(null);
        } else if (tarea.getFechaCompletada() == null) {
            tarea.setFechaCompletada(LocalDateTime.now());
        }
        tarea.setPrioridad(prioridad != null ? prioridad : TareaPlanTrabajo.PrioridadTarea.MEDIA);
        return tareaRepository.save(tarea);
    }

    public void eliminarTareaGeneral(Long direccionId, Long tareaId) {
        TareaPlanTrabajo tarea = obtenerTareaDireccion(direccionId, tareaId);
        tareaRepository.delete(tarea);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerDashboard(Long direccionId, Long planTrabajoId) {
        PlanTrabajo planTrabajo = obtenerPlanTrabajo(direccionId, planTrabajoId);
        List<MiembroPlanTrabajo> miembros = miembroRepository.findByPlanTrabajoIdOrderByFechaAsignacionDesc(planTrabajoId);

        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("planTrabajoNombre", planTrabajo.getNombre());
        dashboard.put("objetivo", planTrabajo.getObjetivo());
        dashboard.put("estado", planTrabajo.getEstado().name());
        dashboard.put("fechaInicio", planTrabajo.getFechaInicio());
        dashboard.put("fechaFin", planTrabajo.getFechaFin());
        dashboard.put("totalMiembros", miembros.size());

        int totalTareas = 0;
        int tareasCompletadas = 0;
        int tareasVencidas = 0;
        double sumaCalificaciones = 0;
        int tareasConCalificacion = 0;
        int tareasATiempo = 0;
        int tareasTarde = 0;

        List<Map<String, Object>> rendimientoMiembros = new ArrayList<>();
        for (MiembroPlanTrabajo miembro : miembros) {
            List<TareaPlanTrabajo> tareas = tareaRepository.findByMiembroIdOrderByFechaLimiteAsc(miembro.getId());
            int completadas = 0, vencidas = 0, pendientes = 0, enProgreso = 0;
            double sumaNotas = 0;
            int conNota = 0, aTiempo = 0, tarde = 0;

            for (TareaPlanTrabajo t : tareas) {
                totalTareas++;
                if (t.getEstado() == TareaPlanTrabajo.EstadoTarea.FINALIZADO) {
                    completadas++; tareasCompletadas++;
                    if (t.getFechaCompletada() != null && !t.getFechaCompletada().toLocalDate().isAfter(t.getFechaLimite())) {
                        aTiempo++; tareasATiempo++;
                    } else { tarde++; tareasTarde++; }
                } else if (t.isVencida()) {
                    vencidas++; tareasVencidas++; tarde++; tareasTarde++;
                } else if (t.getEstado() == TareaPlanTrabajo.EstadoTarea.PENDIENTE) {
                    pendientes++;
                } else {
                    // En ejecución, en revisión o en corrección: la tarea sigue en curso
                    enProgreso++;
                }
                if (t.getCalificacion() != null && t.getEstado() == TareaPlanTrabajo.EstadoTarea.FINALIZADO) {
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
    public List<Usuario> listarUsuariosDisponibles(Long direccionId, Long planTrabajoId) {
        List<Long> idsMiembros = miembroRepository.findByPlanTrabajoIdOrderByFechaAsignacionDesc(planTrabajoId)
                .stream().map(m -> m.getUsuario().getId()).toList();
        return usuarioRepository.findActivosByDireccionId(direccionId).stream()
                .filter(u -> !idsMiembros.contains(u.getId())).toList();
    }

    private Direccion obtenerDireccion(Long direccionId) {
        return direccionRepository.findById(direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Dirección no encontrada"));
    }
}
