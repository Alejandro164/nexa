package com.chavescr.nexa.dto;

import java.util.List;
import java.util.Map;

import com.chavescr.nexa.entity.HorarioLeccion;
import com.chavescr.nexa.entity.Jornada;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.Usuario;

/** Semana consultable: horario de un docente o de un estudiante. */
public class ConsultaHorario {

    public static final String VISTA_DOCENTES = "docentes";
    public static final String VISTA_ESTUDIANTES = "estudiantes";

    private final String vista;
    private final boolean puedeVerDocentes;
    private final boolean puedeVerEstudiantes;
    private final boolean puedeElegirUsuario;
    private final String titular;
    private final String detalle;
    private final String ayuda;
    private final String mensaje;
    private final List<PeriodoAcademico> periodos;
    private final PeriodoAcademico periodo;
    private final List<Usuario> usuarios;
    private final Long usuarioId;
    private final List<String> dias;
    private final List<Integer> lecciones;
    private final Map<Integer, Jornada.FranjaHoraria> franjas;
    private final Map<Integer, Jornada.FranjaHoraria> recreos;
    private final Map<Integer, Jornada.FranjaHoraria> almuerzos;
    private final Map<String, List<HorarioLeccion>> horario;
    private final int totalLecciones;
    private final String diaHoy;

    public ConsultaHorario(String vista, boolean puedeVerDocentes, boolean puedeVerEstudiantes,
            boolean puedeElegirUsuario, String titular, String detalle, String ayuda, String mensaje,
            List<PeriodoAcademico> periodos, PeriodoAcademico periodo, List<Usuario> usuarios, Long usuarioId,
            List<String> dias, List<Integer> lecciones, Map<Integer, Jornada.FranjaHoraria> franjas,
            Map<Integer, Jornada.FranjaHoraria> recreos, Map<Integer, Jornada.FranjaHoraria> almuerzos,
            Map<String, List<HorarioLeccion>> horario, int totalLecciones, String diaHoy) {
        this.vista = vista;
        this.puedeVerDocentes = puedeVerDocentes;
        this.puedeVerEstudiantes = puedeVerEstudiantes;
        this.puedeElegirUsuario = puedeElegirUsuario;
        this.titular = titular;
        this.detalle = detalle;
        this.ayuda = ayuda;
        this.mensaje = mensaje;
        this.periodos = periodos;
        this.periodo = periodo;
        this.usuarios = usuarios;
        this.usuarioId = usuarioId;
        this.dias = dias;
        this.lecciones = lecciones;
        this.franjas = franjas;
        this.recreos = recreos;
        this.almuerzos = almuerzos;
        this.horario = horario;
        this.totalLecciones = totalLecciones;
        this.diaHoy = diaHoy;
    }

    public boolean isVistaDocentes() {
        return VISTA_DOCENTES.equals(vista);
    }

    public String getEtiquetaUsuario() {
        return isVistaDocentes() ? "Docente" : "Estudiante";
    }

    public String getVista() {
        return vista;
    }

    public boolean isPuedeVerDocentes() {
        return puedeVerDocentes;
    }

    public boolean isPuedeVerEstudiantes() {
        return puedeVerEstudiantes;
    }

    public boolean isPuedeElegirUsuario() {
        return puedeElegirUsuario;
    }

    public String getTitular() {
        return titular;
    }

    public String getDetalle() {
        return detalle;
    }

    public String getAyuda() {
        return ayuda;
    }

    public String getMensaje() {
        return mensaje;
    }

    public List<PeriodoAcademico> getPeriodos() {
        return periodos;
    }

    public PeriodoAcademico getPeriodo() {
        return periodo;
    }

    public Long getPeriodoId() {
        return periodo != null ? periodo.getId() : null;
    }

    public List<Usuario> getUsuarios() {
        return usuarios;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public List<String> getDias() {
        return dias;
    }

    public List<Integer> getLecciones() {
        return lecciones;
    }

    public Map<Integer, Jornada.FranjaHoraria> getFranjas() {
        return franjas;
    }

    public Map<Integer, Jornada.FranjaHoraria> getRecreos() {
        return recreos;
    }

    public Map<Integer, Jornada.FranjaHoraria> getAlmuerzos() {
        return almuerzos;
    }

    public Map<String, List<HorarioLeccion>> getHorario() {
        return horario;
    }

    public int getTotalLecciones() {
        return totalLecciones;
    }

    public String getDiaHoy() {
        return diaHoy;
    }
}
