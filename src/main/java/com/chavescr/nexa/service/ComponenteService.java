package com.chavescr.nexa.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.dto.FilaNota;
import com.chavescr.nexa.entity.ClaveComponente;
import com.chavescr.nexa.entity.Componente;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.ResultadoComponente;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.ComponenteRepository;
import com.chavescr.nexa.repository.MateriaRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;
import com.chavescr.nexa.repository.ResultadoComponenteRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class ComponenteService {

    private final ComponenteRepository componenteRepository;
    private final PeriodoAcademicoRepository periodoRepository;
    private final NivelAcademicoRepository nivelRepository;
    private final MateriaRepository materiaRepository;
    private final ResultadoComponenteRepository resultadoRepository;
    private final UsuarioRepository usuarioRepository;

    public ComponenteService(ComponenteRepository componenteRepository, PeriodoAcademicoRepository periodoRepository,
            NivelAcademicoRepository nivelRepository, MateriaRepository materiaRepository,
            ResultadoComponenteRepository resultadoRepository, UsuarioRepository usuarioRepository) {
        this.componenteRepository = componenteRepository;
        this.periodoRepository = periodoRepository;
        this.nivelRepository = nivelRepository;
        this.materiaRepository = materiaRepository;
        this.resultadoRepository = resultadoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public PeriodoAcademico obtenerPeriodoActivo(Long direccionId) {
        return periodoRepository.findByDireccionIdAndActivoTrueOrderByFechaInicioDesc(direccionId).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No hay un período académico activo"));
    }

    @Transactional(readOnly = true)
    public PeriodoAcademico obtenerPeriodoActivoOpcional(Long direccionId) {
        return periodoRepository.findByDireccionIdAndActivoTrueOrderByFechaInicioDesc(direccionId).stream()
                .findFirst()
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Componente> listar(Long direccionId, ClaveComponente clave, Long nivelId, Long materiaId,
            Long periodoId) {
        if (exigePeriodo(clave)) {
            if (periodoId == null) {
                return List.of();
            }
            return componenteRepository.findByDireccionIdAndClaveAndNivelIdAndMateriaIdAndPeriodoIdOrderByIdAsc(
                    direccionId, clave, nivelId, materiaId, periodoId);
        }
        if (clave == ClaveComponente.TAREA) {
            return componenteRepository.findByDireccionIdAndClaveAndNivelIdAndMateriaIdOrderByFechaAsc(
                    direccionId, clave, nivelId, materiaId);
        }
        return componenteRepository.findByDireccionIdAndClaveAndNivelIdAndMateriaIdOrderByIdAsc(
                direccionId, clave, nivelId, materiaId);
    }

    @Transactional(readOnly = true)
    public Componente obtener(Long direccionId, Long id) {
        return componenteRepository.findByIdAndDireccionId(id, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Componente no encontrado"));
    }

    @Transactional(readOnly = true)
    public int contarEstudiantesActivos(Long nivelId) {
        return usuarioRepository.findEstudiantesActivosByNivelId(nivelId).size();
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> contarEvaluados(Long direccionId, ClaveComponente clave, List<Long> ids, Long periodoId) {
        return resultadosDe(clave, ids, periodoId).stream()
                .collect(Collectors.groupingBy(r -> r.getComponente().getId(), Collectors.counting()));
    }

    @Transactional(readOnly = true)
    public Map<Long, Double> calcularPromedio(Long direccionId, ClaveComponente clave, List<Long> ids,
            Long periodoId) {
        return resultadosDe(clave, ids, periodoId).stream()
                .collect(Collectors.groupingBy(r -> r.getComponente().getId(),
                        Collectors.averagingInt(ResultadoComponente::getCalificacion)));
    }

    @Transactional(readOnly = true)
    public Map<Long, Double> calcularPesosEfectivos(List<Componente> componentes) {
        Map<Long, Double> pesos = new LinkedHashMap<>();
        if (componentes == null || componentes.isEmpty()) {
            return pesos;
        }
        int sumaFijos = componentes.stream()
                .filter(c -> c.getPorcentaje() != null)
                .mapToInt(Componente::getPorcentaje)
                .sum();
        long ponderados = componentes.stream().filter(Componente::isPonderado).count();
        double resto = Math.max(0, 100 - sumaFijos);
        double pesoPonderado = ponderados == 0 ? 0 : resto / ponderados;
        for (Componente componente : componentes) {
            pesos.put(componente.getId(),
                    componente.isPonderado() ? pesoPonderado : componente.getPorcentaje().doubleValue());
        }
        return pesos;
    }

    public Componente guardar(Long direccionId, ClaveComponente clave, Long nivelId, Long materiaId,
            Componente datos) {
        NivelAcademico nivel = nivelRepository.findByIdAndDireccionId(nivelId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Sección no encontrada"));
        Materia materia = materiaRepository.findByIdAndDireccionId(materiaId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Materia no encontrada"));
        PeriodoAcademico periodoActivo = obtenerPeriodoActivo(direccionId);
        if (datos.getTitulo() == null || datos.getTitulo().isBlank()) {
            throw new IllegalArgumentException("Debes indicar el nombre");
        }
        if (datos.getFecha() == null) {
            throw new IllegalArgumentException("Debes indicar la fecha");
        }
        if (datos.getPuntosTotales() == null || datos.getPuntosTotales() < 1) {
            throw new IllegalArgumentException("Debes indicar los puntos totales");
        }

        List<Componente> existentes = listar(direccionId, clave, nivelId, materiaId,
                exigePeriodo(clave) ? periodoActivo.getId() : null);
        validarPorcentaje(datos, existentes);

        Componente componente = datos.getId() != null
                ? obtener(direccionId, datos.getId())
                : new Componente();
        componente.setClave(clave);
        componente.setDireccion(nivel.getDireccion());
        componente.setNivel(nivel);
        componente.setMateria(materia);
        if (exigePeriodo(clave) && componente.getPeriodo() == null) {
            componente.setPeriodo(periodoActivo);
        }
        componente.setTitulo(datos.getTitulo().trim());
        componente.setDescripcion(datos.getDescripcion() != null ? datos.getDescripcion().trim() : null);
        componente.setFecha(datos.getFecha());
        componente.setPorcentaje(datos.getPorcentaje());
        componente.setPuntosTotales(datos.getPuntosTotales());
        return componenteRepository.save(componente);
    }

    public void eliminar(Long direccionId, Long id) {
        Componente componente = obtener(direccionId, id);
        if (tieneCalificaciones(componente)) {
            throw new IllegalArgumentException(
                    "No se puede eliminar: ya tiene calificaciones registradas");
        }
        componenteRepository.delete(componente);
    }

    @Transactional(readOnly = true)
    public PeriodoAcademico periodoVisible(Long direccionId, Componente componente) {
        if (componente.getClave() == ClaveComponente.COTIDIANO || componente.getClave() == ClaveComponente.TAREA) {
            return obtenerPeriodoActivo(direccionId);
        }
        return componente.getPeriodo();
    }

    @Transactional(readOnly = true)
    public List<FilaNota> listarNotas(Long direccionId, Long componenteId) {
        Componente componente = obtener(direccionId, componenteId);
        Map<Long, NotaGuardada> notas = notasPorEstudiante(direccionId, componente);
        return usuarioRepository.findEstudiantesActivosByNivelId(componente.getNivel().getId()).stream()
                .map(estudiante -> {
                    NotaGuardada nota = notas.get(estudiante.getId());
                    return new FilaNota(estudiante,
                            nota != null ? nota.puntos() : null,
                            nota != null ? nota.calificacion() : null,
                            nota != null ? nota.observacion() : null);
                })
                .toList();
    }

    public FilaNota registrarNota(Long direccionId, Long componenteId, Long estudianteId, Integer calificacion,
            Integer puntosObtenidos, String observacion) {
        Componente componente = obtener(direccionId, componenteId);
        Usuario estudiante = usuarioRepository.findActivoByIdAndDireccionId(estudianteId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));
        NotaGuardada actual = notasPorEstudiante(direccionId, componente).get(estudianteId);
        NotaResuelta resuelta = resolverNota(componente,
                actual != null ? actual.puntos() : null,
                actual != null ? actual.calificacion() : null,
                actual != null ? actual.observacion() : null,
                puntosObtenidos, calificacion, observacion);
        guardarNota(direccionId, componente, estudiante, resuelta);
        return new FilaNota(estudiante, resuelta.puntos(), resuelta.calificacion(), resuelta.observacion());
    }

    private void validarPorcentaje(Componente datos, List<Componente> existentes) {
        int sumaFijosOtros = existentes.stream()
                .filter(c -> datos.getId() == null || !c.getId().equals(datos.getId()))
                .filter(c -> c.getPorcentaje() != null)
                .mapToInt(Componente::getPorcentaje)
                .sum();
        long ponderadosOtros = existentes.stream()
                .filter(c -> datos.getId() == null || !c.getId().equals(datos.getId()))
                .filter(Componente::isPonderado)
                .count();
        Integer porcentaje = datos.getPorcentaje();
        if (porcentaje != null) {
            if (porcentaje < 1 || porcentaje > 100) {
                throw new IllegalArgumentException("El porcentaje debe estar entre 1 y 100");
            }
            if (sumaFijosOtros + porcentaje > 100) {
                throw new IllegalArgumentException(
                        "La suma de los porcentajes fijos no puede superar 100% (disponible: "
                                + (100 - sumaFijosOtros) + "%)");
            }
            if (ponderadosOtros > 0 && sumaFijosOtros + porcentaje >= 100) {
                throw new IllegalArgumentException(
                        "Debes dejar un porcentaje disponible para los ponderados (máximo: "
                                + Math.max(0, 99 - sumaFijosOtros) + "%)");
            }
        } else if (sumaFijosOtros >= 100) {
            throw new IllegalArgumentException(
                    "El 100% ya está asignado con porcentaje fijo. Reduce uno para poder ponderar este.");
        }
    }

    private boolean tieneCalificaciones(Componente componente) {
        return resultadoRepository.existsByComponenteId(componente.getId());
    }

    private List<ResultadoComponente> resultadosDe(ClaveComponente clave, List<Long> ids, Long periodoId) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        if (clave == ClaveComponente.COTIDIANO || clave == ClaveComponente.TAREA) {
            if (periodoId == null) {
                return List.of();
            }
            return resultadoRepository.findByComponenteIdInAndPeriodoId(ids, periodoId);
        }
        return resultadoRepository.findByComponenteIdIn(ids);
    }

    private PeriodoAcademico periodoDe(Long direccionId, Componente componente) {
        if (componente.getPeriodo() != null) {
            return componente.getPeriodo();
        }
        return obtenerPeriodoActivo(direccionId);
    }

    private boolean exigePeriodo(ClaveComponente clave) {
        return clave == ClaveComponente.PROYECTO || clave == ClaveComponente.EXAMEN;
    }

    private Map<Long, NotaGuardada> notasPorEstudiante(Long direccionId, Componente componente) {
        Long periodoId = periodoDe(direccionId, componente).getId();
        return resultadoRepository.findByComponenteIdAndPeriodoId(componente.getId(), periodoId).stream()
                .collect(Collectors.toMap(n -> n.getEstudiante().getId(),
                        n -> new NotaGuardada(n.getPuntosObtenidos(), n.getCalificacion(), n.getObservacion())));
    }

    private void guardarNota(Long direccionId, Componente componente, Usuario estudiante, NotaResuelta nota) {
        PeriodoAcademico periodo = periodoDe(direccionId, componente);
        ResultadoComponente registro = resultadoRepository
                .findByComponenteIdAndEstudianteIdAndPeriodoId(componente.getId(), estudiante.getId(), periodo.getId())
                .orElseGet(ResultadoComponente::new);
        registro.setComponente(componente);
        registro.setEstudiante(estudiante);
        registro.setPeriodo(periodo);
        registro.setPuntosObtenidos(nota.puntos());
        registro.setCalificacion(nota.calificacion());
        registro.setObservacion(nota.observacion());
        resultadoRepository.save(registro);
    }

    private NotaResuelta resolverNota(Componente componente, Integer puntosActuales, Integer calificacionActual,
            String observacionActual, Integer puntosObtenidos, Integer calificacion, String observacion) {
        Integer puntosFinal = null;
        int calificacionFinal;
        if (componente.getPuntosTotales() != null) {
            puntosFinal = puntosObtenidos != null ? puntosObtenidos : puntosActuales;
            if (puntosFinal == null) {
                throw new IllegalArgumentException("Debes asignar primero los puntos obtenidos");
            }
            if (puntosFinal < 0 || puntosFinal > componente.getPuntosTotales()) {
                throw new IllegalArgumentException(
                        "Los puntos obtenidos deben estar entre 0 y " + componente.getPuntosTotales());
            }
            calificacionFinal = (int) Math.round(puntosFinal * 100.0 / componente.getPuntosTotales());
        } else {
            Integer calificacionIngresada = calificacion != null ? calificacion : calificacionActual;
            if (calificacionIngresada == null) {
                throw new IllegalArgumentException("Debes asignar primero una calificación");
            }
            if (calificacionIngresada < 0 || calificacionIngresada > 100) {
                throw new IllegalArgumentException("La calificación debe estar entre 0 y 100");
            }
            calificacionFinal = calificacionIngresada;
        }
        String observacionFinal = observacionActual;
        if (observacion != null) {
            observacionFinal = observacion.isBlank() ? null : observacion.trim();
        }
        return new NotaResuelta(puntosFinal, calificacionFinal, observacionFinal);
    }

    private record NotaGuardada(Integer puntos, Integer calificacion, String observacion) {
    }

    private record NotaResuelta(Integer puntos, int calificacion, String observacion) {
    }
}
