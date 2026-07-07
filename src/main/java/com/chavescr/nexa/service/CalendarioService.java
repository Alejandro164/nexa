package com.chavescr.nexa.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.chavescr.nexa.dto.BandaEventoDTO;
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

    public List<SemanaCalendarioDTO> construirMes(Long institucionId, Long usuarioId, LocalDate referencia) {
        LocalDate primerDiaMes = referencia.withDayOfMonth(1);
        LocalDate ultimoDiaMes = referencia.withDayOfMonth(referencia.lengthOfMonth());
        LocalDate inicioGrid = primerDiaMes.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate finGrid = ultimoDiaMes.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        Map<LocalDate, List<EventoCalendarioDTO>> eventosPorDia =
                agruparEventos(institucionId, usuarioId, inicioGrid, finGrid);

        List<SemanaCalendarioDTO> semanas = new ArrayList<>();
        List<DiaCalendarioDTO> diasSemana = new ArrayList<>();
        LocalDate inicioSemanaActual = inicioGrid;
        for (LocalDate cursor = inicioGrid; !cursor.isAfter(finGrid); cursor = cursor.plusDays(1)) {
            boolean otroMes = cursor.getMonthValue() != referencia.getMonthValue()
                    || cursor.getYear() != referencia.getYear();
            diasSemana.add(new DiaCalendarioDTO(cursor, otroMes, cursor.equals(LocalDate.now()),
                    eventosPorDia.getOrDefault(cursor, List.of())));
            if (diasSemana.size() == 7) {
                LocalDate finSemanaActual = inicioSemanaActual.plusDays(6);
                semanas.add(new SemanaCalendarioDTO(diasSemana,
                        calcularBandas(institucionId, inicioSemanaActual, finSemanaActual)));
                diasSemana = new ArrayList<>();
                inicioSemanaActual = cursor.plusDays(1);
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

    public SemanaCalendarioDTO construirSemana(Long institucionId, Long usuarioId, LocalDate referencia) {
        LocalDate inicioSemana = referencia.with(DayOfWeek.MONDAY);
        LocalDate finSemana = referencia.with(DayOfWeek.SUNDAY);
        Map<LocalDate, List<EventoCalendarioDTO>> eventosPorDia =
                agruparEventos(institucionId, usuarioId, inicioSemana, finSemana);

        List<DiaCalendarioDTO> dias = new ArrayList<>();
        for (LocalDate cursor = inicioSemana; !cursor.isAfter(finSemana); cursor = cursor.plusDays(1)) {
            dias.add(new DiaCalendarioDTO(cursor, false, cursor.equals(LocalDate.now()),
                    eventosPorDia.getOrDefault(cursor, List.of())));
        }
        return new SemanaCalendarioDTO(dias, calcularBandas(institucionId, inicioSemana, finSemana));
    }

    public DiaCalendarioDTO construirDia(Long institucionId, Long usuarioId, LocalDate referencia) {
        Map<LocalDate, List<EventoCalendarioDTO>> eventosPorDia =
                agruparEventos(institucionId, usuarioId, referencia, referencia);
        return new DiaCalendarioDTO(referencia, false, referencia.equals(LocalDate.now()),
                eventosPorDia.getOrDefault(referencia, List.of()));
    }

    public List<DiaCalendarioDTO> construirAgenda(Long institucionId, Long usuarioId, LocalDate referencia) {
        return construirMes(institucionId, usuarioId, referencia).stream()
                .flatMap(semana -> semana.getDias().stream())
                .filter(dia -> !dia.isOtroMes())
                .filter(dia -> !dia.getEventos().isEmpty())
                .toList();
    }

    /**
     * Bandas horizontales (estilo Outlook/Teams) para los eventos institucionales de varios días
     * que cruzan la semana [inicioSemana, finSemana], con apilado (fila) para no solaparse entre sí.
     * Se devuelven todas las filas; la vista solo muestra {@link SemanaCalendarioDTO#FILAS_VISIBLES_POR_DEFECTO}
     * de entrada y permite expandir el resto.
     */
    private List<BandaEventoDTO> calcularBandas(Long institucionId, LocalDate inicioSemana, LocalDate finSemana) {
        List<EventoMepDTO> multiDia = actividadInstitucionalService
                .listarEventosEnRango(institucionId, inicioSemana, finSemana)
                .stream()
                .filter(e -> e.getFechaFin().isAfter(e.getFechaInicio()))
                .sorted(Comparator.comparing(EventoMepDTO::getFechaInicio))
                .toList();

        List<BandaEventoDTO> bandas = new ArrayList<>();
        List<Integer> finColumnaPorFila = new ArrayList<>();

        for (EventoMepDTO ev : multiDia) {
            LocalDate inicioClip = ev.getFechaInicio().isBefore(inicioSemana) ? inicioSemana : ev.getFechaInicio();
            LocalDate finClip = ev.getFechaFin().isAfter(finSemana) ? finSemana : ev.getFechaFin();
            int columnaInicio = (int) ChronoUnit.DAYS.between(inicioSemana, inicioClip) + 1;
            int columnaFin = (int) ChronoUnit.DAYS.between(inicioSemana, finClip) + 1;

            int fila = -1;
            for (int f = 0; f < finColumnaPorFila.size(); f++) {
                if (finColumnaPorFila.get(f) < columnaInicio) {
                    fila = f;
                    break;
                }
            }
            if (fila == -1) {
                fila = finColumnaPorFila.size();
                finColumnaPorFila.add(0);
            }
            finColumnaPorFila.set(fila, columnaFin);

            bandas.add(new BandaEventoDTO(ev.getTitulo(), ev.getDescripcion(), ev.getLink(),
                    columnaInicio, columnaFin, fila, ev.getFechaInicio(), ev.getFechaFin()));
        }
        return bandas;
    }

    private Map<LocalDate, List<EventoCalendarioDTO>> agruparEventos(Long institucionId, Long usuarioId,
            LocalDate desde, LocalDate hasta) {
        Map<LocalDate, List<EventoCalendarioDTO>> mapa = new LinkedHashMap<>();

        for (TareaProyecto t : proyectoService.listarTareasInstitucion(institucionId)) {
            LocalDate f = t.getFechaLimite();
            if (f != null && !f.isBefore(desde) && !f.isAfter(hasta)) {
                mapa.computeIfAbsent(f, k -> new ArrayList<>())
                        .add(EventoCalendarioDTO.deTarea(t.getTitulo(), t.getDescripcion(), f));
            }
        }

        for (Recordatorio r : recordatorioService.listarRecordatorios(institucionId, usuarioId)) {
            LocalDate f = r.getFechaLimite().toLocalDate();
            if (!f.isBefore(desde) && !f.isAfter(hasta)) {
                mapa.computeIfAbsent(f, k -> new ArrayList<>())
                        .add(EventoCalendarioDTO.deRecordatorio(r.getTitulo(), r.getDescripcion(), f,
                                r.getFechaLimite().toLocalTime()));
            }
        }

        for (EventoMepDTO ev : actividadInstitucionalService.listarEventosEnRango(institucionId, desde, hasta)) {
            // Se ancla al día de inicio (o al primer día visible si ya estaba en curso). Los eventos
            // de varios días también se muestran como banda en la grilla (ver calcularBandas); aquí
            // se guardan igual para que las vistas de lista (Día/Agenda) los sigan mostrando completos.
            boolean multiDia = ev.getFechaFin().isAfter(ev.getFechaInicio());
            LocalDate diaAncla = ev.getFechaInicio().isBefore(desde) ? desde : ev.getFechaInicio();
            String descripcion = multiDia
                    ? "(" + ev.getFechaInicio() + " a " + ev.getFechaFin() + ") " + ev.getDescripcion()
                    : ev.getDescripcion();
            mapa.computeIfAbsent(diaAncla, k -> new ArrayList<>())
                    .add(EventoCalendarioDTO.deInstitucional(ev.getTitulo(), descripcion, diaAncla, ev.getLink(),
                            multiDia));
        }

        for (List<EventoCalendarioDTO> lista : mapa.values()) {
            lista.sort(Comparator.comparing(EventoCalendarioDTO::getHora,
                    Comparator.nullsFirst(Comparator.naturalOrder())));
        }

        return mapa;
    }
}
