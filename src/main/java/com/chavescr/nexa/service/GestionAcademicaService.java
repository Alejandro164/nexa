package com.chavescr.nexa.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.dto.NombresRubros;
import com.chavescr.nexa.dto.PestanaRubro;
import com.chavescr.nexa.entity.ClaveComponente;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.TipoComponente;

/**
 * Arma la vista de Gestión Académica alrededor de una materia: primero se elige la materia y después se
 * muestran solo las pestañas de los rubros que su componente de evaluación califica.
 */
@Service
@Transactional
public class GestionAcademicaService {

    private final AlcanceDocenteService alcanceDocenteService;
    private final TipoComponenteService tipoComponenteService;
    private final ComponenteEvaluacionService componenteEvaluacionService;

    public GestionAcademicaService(AlcanceDocenteService alcanceDocenteService,
            TipoComponenteService tipoComponenteService, ComponenteEvaluacionService componenteEvaluacionService) {
        this.alcanceDocenteService = alcanceDocenteService;
        this.tipoComponenteService = tipoComponenteService;
        this.componenteEvaluacionService = componenteEvaluacionService;
    }

    /** Materias que el usuario puede calificar: todas las activas, o las de su horario si es solo docente. */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<Materia> materias(Long direccionId, Long docenteId) {
        return alcanceDocenteService.materiasVisibles(direccionId, docenteId);
    }

    /**
     * La materia pedida si está entre las visibles; si no, la recordada; si tampoco, la primera. Null si no
     * hay materias.
     */
    public Long elegirMateria(List<Materia> materias, Long pedida, Long recordada) {
        if (materias == null || materias.isEmpty()) {
            return null;
        }
        for (Long candidata : new Long[] { pedida, recordada }) {
            if (candidata != null && materias.stream().anyMatch(m -> candidata.equals(m.getId()))) {
                return candidata;
            }
        }
        return materias.get(0).getId();
    }

    /**
     * Pestañas de rubros de la materia, en el orden de los tipos de la dirección. Un rubro con 0% en el
     * componente de la materia no tiene pestaña. Usa listar (no solo lectura) porque crea los tipos del
     * sistema la primera vez.
     */
    @Transactional(rollbackFor = Exception.class)
    public List<PestanaRubro> pestanas(Long direccionId, Long materiaId) {
        if (materiaId == null) {
            return List.of();
        }
        int[] pesos = componenteEvaluacionService.pesosDeMateria(direccionId, materiaId);
        NombresRubros nombres = componenteEvaluacionService.nombresDeMateria(direccionId, materiaId);
        if (nombres == null) {
            nombres = NombresRubros.PREDETERMINADOS;
        }
        List<PestanaRubro> pestanas = new ArrayList<>();
        for (TipoComponente tipo : tipoComponenteService.listarActivos(direccionId)) {
            ClaveComponente clave = tipo.getClave();
            if (clave == null) {
                continue;
            }
            int peso = pesos == null ? 0 : pesos[indice(clave)];
            if (peso <= 0) {
                continue;
            }
            // El emoji que el componente eligió para el rubro; si no eligió, el de la institución.
            String emoji = nombres.emojiDe(clave) != null ? nombres.emojiDe(clave) : tipo.getEmoji();
            pestanas.add(new PestanaRubro(clave, nombres.de(clave), emoji, peso, url(clave), panelId(clave)));
        }
        return pestanas;
    }

    /** Posición del rubro en ComponenteEvaluacion.pesos(). */
    private static int indice(ClaveComponente clave) {
        return switch (clave) {
            case COTIDIANO -> 0;
            case TAREA -> 1;
            case PROYECTO -> 2;
            case EXAMEN -> 3;
        };
    }

    private static String url(ClaveComponente clave) {
        return switch (clave) {
            case COTIDIANO -> "/gestion-academica/cotidiano/indicadores";
            case TAREA -> "/gestion-academica/tareas/definiciones";
            case PROYECTO -> "/gestion-academica/proyectos";
            case EXAMEN -> "/gestion-academica/examenes";
        };
    }

    private static String panelId(ClaveComponente clave) {
        return switch (clave) {
            case COTIDIANO -> "indicadores-panel";
            case TAREA -> "tareas-definiciones-panel";
            case PROYECTO -> "proyectos-panel";
            case EXAMEN -> "examenes-panel";
        };
    }
}
