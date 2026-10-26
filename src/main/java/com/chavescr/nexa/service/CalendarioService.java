package com.chavescr.nexa.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.chavescr.nexa.dto.DiaCalendarioDTO;
import com.chavescr.nexa.dto.EventoCalendarioDTO;
import com.chavescr.nexa.dto.EventoMepDTO;
import com.chavescr.nexa.dto.SemanaCalendarioDTO;
import com.chavescr.nexa.entity.Recordatorio;
import com.chavescr.nexa.entity.TareaProyecto;

/**
 * Agrega Tareas, Recordatorios y Actividades Institucionales en un
 * único modelo de eventos para las vistas del calendario (mes, semana, día, agenda).
 */
@Service
public class CalendarioService {

    private final ProyectoService proyectoService;
    private final RecordatorioService recordatorioService;
    private final ActividadInstitucionalService actividadInstitucionalService;

    public CalendarioService(ProyectoService proyectoService,
            RecordatorioService recordatorioService,
            ActividadInstitucionalService actividadInstitucionalService) {
        this.proyectoService = proyectoService;
        this.recordatorioService = recordatorioService;
        this.actividadInstitucionalService = actividadInstitucionalService;
    }

    public List<SemanaCalendarioDTO> construirMes(Long direccionId, Long usuarioId, LocalDate referencia) {
        LocalDate primerDiaMes = referencia.withDayOfMonth(1);
        LocalDate ultimoDiaMes = referencia.withDayOfMonth(referencia.lengthOfMonth());
        LocalDate inicioGrid = primerDiaMes.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate finGrid = ultimoDiaMes.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        Map<LocalDate, List<EventoCalendarioDTO>> eventosPorDia =
                agruparEventos(direccionId, usuarioId, inicioGrid, finGrid);

        List<SemanaCalendarioDTO> semanas = new ArrayList<>();
        List<DiaCalendarioDTO> diasSemana = new ArrayList<>();
        for (LocalDate cursor = inicioGrid; !cursor.isAfter(finGrid); cursor = cursor.plusDays(1)) {
            boolean otroMes = cursor.getMonthValue() != referencia.getMonthValue()
                    || cursor.getYear() != referencia.getYear();
            diasSemana.add(new DiaCalendarioDTO(cursor, otroMes, cursor.equals(LocalDate.now()),
                    eventosPorDia.getOrDefault(cursor, List.of())));
            if (diasSemana.size() == 7) {
                semanas.add(new SemanaCalendarioDTO(diasSemana));
                diasSemana = new ArrayList<>();
            }
        }
        return semanas;
    }

    /** Grilla de días simple (sin eventos) para el mini-calendario del sidebar. */
    public List<List<DiaCalendarioDTO>> construirMesSimple(LocalDate referencia) {
        LocalDate primerDiaMes = referencia.withDayOfMonth(1);
        LocalDate ultimoDiaMes = referencia.withDayOfMonth(referencia.lengthOfMonth());
        LocalDate inicioGrid = primerDiaMes.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate finGrid = ultimoDiaMes.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        LocalDate inicioSemanaRef = referencia.with(DayOfWeek.MONDAY);
        LocalDate finSemanaRef = referencia.with(DayOfWeek.SUNDAY);

        List<List<DiaCalendarioDTO>> semanas = new ArrayList<>();
        List<DiaCalendarioDTO> semanaActual = new ArrayList<>();
        for (LocalDate cursor = inicioGrid; !cursor.isAfter(finGrid); cursor = cursor.plusDays(1)) {
            boolean otroMes = cursor.getMonthValue() != referencia.getMonthValue()
                    || cursor.getYear() != referencia.getYear();
            boolean enSemanaActual = !cursor.isBefore(inicioSemanaRef) && !cursor.isAfter(finSemanaRef);
            semanaActual.add(new DiaCalendarioDTO(cursor, otroMes, cursor.equals(LocalDate.now()), List.of(),
                    enSemanaActual));
            if (semanaActual.size() == 7) {
                semanas.add(semanaActual);
                semanaActual = new ArrayList<>();
            }
        }
        return semanas;
    }

    public SemanaCalendarioDTO construirSemana(Long direccionId, Long usuarioId, LocalDate referencia) {
        LocalDate inicioSemana = referencia.with(DayOfWeek.MONDAY);
        LocalDate finSemana = referencia.with(DayOfWeek.SUNDAY);
        Map<LocalDate, List<EventoCalendarioDTO>> eventosPorDia =
                agruparEventos(direccionId, usuarioId, inicioSemana, finSemana);

        List<DiaCalendarioDTO> dias = new ArrayList<>();
        for (LocalDate cursor = inicioSemana; !cursor.isAfter(finSemana); cursor = cursor.plusDays(1)) {
            dias.add(new DiaCalendarioDTO(cursor, false, cursor.equals(LocalDate.now()),
                    eventosPorDia.getOrDefault(cursor, List.of())));
        }
        return new SemanaCalendarioDTO(dias);
    }

    public DiaCalendarioDTO construirDia(Long direccionId, Long usuarioId, LocalDate referencia) {
        Map<LocalDate, List<EventoCalendarioDTO>> eventosPorDia =
                agruparEventos(direccionId, usuarioId, referencia, referencia);
        return new DiaCalendarioDTO(referencia, false, referencia.equals(LocalDate.now()),
                eventosPorDia.getOrDefault(referencia, List.of()));
    }

    public List<DiaCalendarioDTO> construirAgenda(Long direccionId, Long usuarioId, LocalDate referencia) {
        return construirMes(direccionId, usuarioId, referencia).stream()
                .flatMap(semana -> semana.getDias().stream())
                .filter(dia -> !dia.isOtroMes())
                .filter(dia -> !dia.getEventos().isEmpty())
                .toList();
    }

    private Map<LocalDate, List<EventoCalendarioDTO>> agruparEventos(Long direccionId, Long usuarioId,
            LocalDate desde, LocalDate hasta) {
        Map<LocalDate, List<EventoCalendarioDTO>> mapa = new LinkedHashMap<>();

        for (TareaProyecto t : proyectoService.listarTareasDireccion(direccionId)) {
            LocalDate f = t.getFechaLimite();
            if (f != null && !f.isBefore(desde) && !f.isAfter(hasta)) {
                mapa.computeIfAbsent(f, k -> new ArrayList<>())
                        .add(EventoCalendarioDTO.deTarea(t.getTitulo(), t.getDescripcion(), f));
            }
        }

        for (Recordatorio r : recordatorioService.listarRecordatorios(direccionId, usuarioId)) {
            LocalDate f = r.getFechaLimite().toLocalDate();
            if (!f.isBefore(desde) && !f.isAfter(hasta)) {
                mapa.computeIfAbsent(f, k -> new ArrayList<>())
                        .add(EventoCalendarioDTO.deRecordatorio(r.getTitulo(), r.getDescripcion(), f,
                                r.getFechaLimite().toLocalTime()));
            }
        }

        for (EventoMepDTO ev : actividadInstitucionalService.listarEventosEnRango(direccionId, desde, hasta)) {
            // Un evento de varios días aparece como una etiqueta en cada día que dura (dentro del rango visible).
            String descripcion = descripcionEvento(ev);
            LocalDate primero = ev.getFechaInicio().isBefore(desde) ? desde : ev.getFechaInicio();
            LocalDate ultimo = ev.getFechaFin().isAfter(hasta) ? hasta : ev.getFechaFin();
            for (LocalDate dia = primero; !dia.isAfter(ultimo); dia = dia.plusDays(1)) {
                mapa.computeIfAbsent(dia, k -> new ArrayList<>())
                        .add(EventoCalendarioDTO.deInstitucional(ev.getTituloCalendario(), descripcion, dia,
                                ev.getLink(), ev.getFechaInicio(), ev.getFechaFin()));
            }
        }

        for (List<EventoCalendarioDTO> lista : mapa.values()) {
            lista.sort(Comparator.comparing(EventoCalendarioDTO::getHora,
                    Comparator.nullsFirst(Comparator.naturalOrder())));
        }

        return mapa;
    }

    /** Si la institución le cambió el título, el título original del MEP pasa a la descripción del detalle. */
    private String descripcionEvento(EventoMepDTO ev) {
        boolean tieneDescripcion = ev.getDescripcion() != null && !ev.getDescripcion().isBlank();
        if (!ev.isConTituloPersonalizado() || ev.isPropia()) {
            return ev.getDescripcion();
        }
        return tieneDescripcion ? ev.getTituloVisible() + "\n\n" + ev.getDescripcion() : ev.getTituloVisible();
    }
}
