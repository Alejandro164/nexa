package com.chavescr.nexa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chavescr.nexa.dto.EventoCalendarioDTO;
import com.chavescr.nexa.dto.NotasCatalogo.EstudianteOpcion;
import com.chavescr.nexa.dto.NotasCatalogo.MateriaNota;
import com.chavescr.nexa.dto.NotasCatalogo.NotaPeriodo;
import com.chavescr.nexa.dto.PaginaEntregas;
import com.chavescr.nexa.dto.PanelInicio;
import com.chavescr.nexa.dto.PanelInicio.Leccion;
import com.chavescr.nexa.dto.PanelInicio.Rendimiento;
import com.chavescr.nexa.entity.EscalaNotas;
import com.chavescr.nexa.entity.HorarioLeccion;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.AsistenciaEstudianteRepository;
import com.chavescr.nexa.repository.EntregaAcademicaDocenteRepository;
import com.chavescr.nexa.repository.HorarioLeccionRepository;
import com.chavescr.nexa.repository.IncidenteConductaRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final Long DIRECCION = 1L;
    private static final Long DOCENTE = 10L;
    private static final Long OTRO_DOCENTE = 11L;

    @Mock
    private AsistenciaEstudianteRepository asistenciaRepository;
    @Mock
    private HorarioLeccionRepository horarioRepository;
    @Mock
    private IncidenteConductaRepository incidenteRepository;
    @Mock
    private EntregaAcademicaDocenteRepository entregaDocenteRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ConfiguracionAcademicaService configuracionAcademicaService;
    @Mock
    private RegistroAsistenciaService registroAsistenciaService;
    @Mock
    private VisitaService visitaService;
    @Mock
    private RetiroEstudianteService retiroService;
    @Mock
    private EntregaAcademicaService entregaAcademicaService;
    @Mock
    private ComunicadoService comunicadoService;
    @Mock
    private CalendarioService calendarioService;
    @Mock
    private NotasConsultaService notasConsultaService;
    @Mock
    private EscalaNotasService escalaNotasService;
    @Mock
    private DocenteGuiaService docenteGuiaService;

    @InjectMocks
    private DashboardService service;

    private PeriodoAcademico periodo;
    private NivelAcademico ocho;

    @BeforeEach
    void preparar() {
        LocalDate hoy = LocalDate.now(DashboardService.ZONA);
        periodo = new PeriodoAcademico();
        periodo.setId(5L);
        periodo.setCodigo("III-2026");
        periodo.setFechaInicio(hoy.minusDays(30));
        periodo.setFechaFin(hoy.plusDays(30));
        periodo.setActivo(true);
        lenient().when(configuracionAcademicaService.listarPeriodosActivos(DIRECCION)).thenReturn(List.of(periodo));

        ocho = new NivelAcademico();
        ocho.setId(80L);
        ocho.setGrado(8);
        ocho.setSeccion("2");
        ocho.setActivo(true);
        lenient().when(escalaNotasService.obtener(eq(DIRECCION), any())).thenReturn(EscalaNotas.predeterminada());
    }

    @Test
    void rendimientoMarcaMateriasBajoLaNotaYDejaLaConductaFueraDelPromedio() {
        when(configuracionAcademicaService.listarNivelesActivos(DIRECCION)).thenReturn(List.of(ocho));
        when(usuarioRepository.findEstudiantesActivosConNivelEn(eq(DIRECCION), any(), any(), any()))
                .thenReturn(List.of());
        EstudianteOpcion ana = estudiante(1L, "Ana", List.of(
                materia(100L, "Matemática", 60.0),
                materia(101L, "Ciencias", 80.0),
                materia(NotasConsultaService.CONDUCTA_ID, "Conducta", 100.0)));
        EstudianteOpcion beto = estudiante(2L, "Beto", List.of(
                materia(100L, "Matemática", 90.0),
                materia(NotasConsultaService.CONDUCTA_ID, "Conducta", 50.0)));
        EstudianteOpcion sinNotas = estudiante(3L, "Carla", List.of(materia(100L, "Matemática", null)));
        when(notasConsultaService.resumenDelPeriodo(eq(DIRECCION), any(), eq(periodo)))
                .thenReturn(List.of(ana, beto, sinNotas));

        Rendimiento r = service.rendimiento(DIRECCION);

        assertNull(r.aviso());
        assertEquals(2, r.estudiantesConNotas(), "Carla no tiene calificaciones en el período");
        assertEquals(2, r.totalEnRiesgo());
        // Los dos tienen una sola materia bajo 70: primero la nota más baja (conducta 50 de Beto).
        assertEquals("Beto", r.enRiesgo().get(0).nombre());
        assertEquals(List.of("Conducta"), r.enRiesgo().get(0).materias());
        assertEquals("Ana", r.enRiesgo().get(1).nombre());
        assertEquals(List.of("Matemática"), r.enRiesgo().get(1).materias());
        assertEquals(60.0, r.enRiesgo().get(1).peorNota());

        // Promedio de la sección sin conducta: Ana (60+80)/2 = 70 y Beto 90, promedio 80.
        assertEquals(1, r.seccionesMasBajas().size());
        assertEquals("8-2", r.seccionesMasBajas().get(0).seccion());
        assertEquals(80.0, r.seccionesMasBajas().get(0).promedio());
        assertEquals(2, r.seccionesMasBajas().get(0).enRiesgo());
    }

    @Test
    void rendimientoSinPeriodoActivoAvisa() {
        when(configuracionAcademicaService.listarPeriodosActivos(DIRECCION)).thenReturn(List.of());

        Rendimiento r = service.rendimiento(DIRECCION);

        assertEquals("No hay un período activo.", r.aviso());
        assertTrue(r.enRiesgo().isEmpty());
    }

    @Test
    void docenteVeSoloSusLeccionesYLasDadasSinListaQuedanPendientes() {
        String dia = DashboardService.diaDeHoy();
        assumeFalse(dia.isEmpty(), "Los domingos no hay horario");
        Materia mate = new Materia();
        mate.setId(100L);
        mate.setNombre("Matemática");
        Materia ciencias = new Materia();
        ciencias.setId(101L);
        ciencias.setNombre("Ciencias");
        HorarioLeccion dada = leccion(1, LocalTime.MIN, LocalTime.MIN.plusMinutes(1), mate, DOCENTE);
        HorarioLeccion conLista = leccion(2, LocalTime.MIN, LocalTime.MIN.plusMinutes(1), ciencias, DOCENTE);
        HorarioLeccion proxima = leccion(9, LocalTime.MAX.minusMinutes(2), LocalTime.MAX, mate, DOCENTE);
        HorarioLeccion ajena = leccion(3, LocalTime.MIN, LocalTime.MIN.plusMinutes(1), mate, OTRO_DOCENTE);
        when(horarioRepository.findDelDia(DIRECCION, periodo.getId(), dia))
                .thenReturn(List.of(dada, conLista, ajena, proxima));
        List<Object[]> leccionesConLista = new ArrayList<>();
        leccionesConLista.add(new Object[] { ocho.getId(), ciencias.getId(), 2 });
        when(asistenciaRepository.findLeccionesConLista(eq(DIRECCION), any())).thenReturn(leccionesConLista);
        when(entregaAcademicaService.listar(eq(DIRECCION), eq(DOCENTE), anyBoolean(), anyBoolean(), any(), eq(1)))
                .thenReturn(new PaginaEntregas(List.of(), 1, 1, 0));
        when(comunicadoService.sinLeer(DIRECCION, DOCENTE)).thenReturn(2L);
        when(docenteGuiaService.listarSecciones(DIRECCION, DOCENTE)).thenReturn(List.of());
        when(calendarioService.eventosEntre(eq(DIRECCION), eq(DOCENTE), any(), any(), eq(true)))
                .thenReturn(new TreeMap<>());

        PanelInicio.Docente panel = service.docente(DIRECCION, DOCENTE);

        assertEquals(3, panel.clases().size(), "La lección del otro docente no aparece");
        Leccion primera = panel.clases().get(0);
        assertEquals(Leccion.Estado.TERMINADA, primera.estado());
        assertFalse(primera.listaPasada());
        assertTrue(primera.isSinLista());
        assertTrue(panel.clases().get(1).listaPasada());
        assertEquals(Leccion.Estado.PROXIMA, panel.clases().get(2).estado());
        assertFalse(panel.clases().get(2).isSinLista(), "Una lección que no ha empezado no debe lista todavía");

        assertEquals("Lecciones de hoy sin pasar lista", panel.pendientes().get(0).titulo());
        assertEquals(1, panel.pendientes().get(0).cantidad());
        assertEquals("Comunicados sin leer", panel.pendientes().get(1).titulo());
        assertEquals(2, panel.pendientes().size(), "Sin entregas pendientes no aparece esa fila");
        assertNull(panel.diaSinClases());
        assertNotNull(panel.periodo());
    }

    @Test
    void laAgendaMuestraUnaVezLosEventosDeVariosDias() {
        LocalDate hoy = LocalDate.now(DashboardService.ZONA);
        Map<LocalDate, List<EventoCalendarioDTO>> eventos = new TreeMap<>();
        for (int i = 0; i < 3; i++) {
            LocalDate dia = hoy.plusDays(i);
            eventos.computeIfAbsent(dia, k -> new ArrayList<>()).add(EventoCalendarioDTO.deInstitucional(
                    "Semana de la ciencia", null, dia, null, hoy, hoy.plusDays(2)));
        }
        eventos.computeIfAbsent(hoy.plusDays(1), k -> new ArrayList<>())
                .add(EventoCalendarioDTO.deTarea("Entregar actas", null, hoy.plusDays(1)));
        when(calendarioService.eventosEntre(eq(DIRECCION), anyLong(), any(), any(), eq(false))).thenReturn(eventos);
        when(usuarioRepository.findEstudiantesByPadreId(20L)).thenReturn(List.of());
        when(comunicadoService.sinLeer(DIRECCION, 20L)).thenReturn(0L);

        PanelInicio.Familia panel = service.familia(DIRECCION, 20L, false);

        assertEquals(2, panel.agenda().size());
        PanelInicio.EventoAgenda semana = panel.agenda().get(0);
        assertEquals("Semana de la ciencia", semana.titulo());
        assertEquals(hoy, semana.fecha());
        assertEquals(hoy.plusDays(2), semana.fin());
        assertTrue(semana.isVariosDias());
        assertTrue(panel.pendientes().isEmpty());
        assertTrue(panel.estudiantes().isEmpty());
    }

    @Test
    void elDocenteDeMuestraEsElQueTieneMasLecciones() {
        Usuario pocas = new Usuario();
        pocas.setId(DOCENTE);
        Usuario muchas = new Usuario();
        muchas.setId(OTRO_DOCENTE);
        when(configuracionAcademicaService.listarDocentes(DIRECCION)).thenReturn(List.of(pocas, muchas));
        List<Object[]> conteo = new ArrayList<>();
        conteo.add(new Object[] { DOCENTE, 3L });
        conteo.add(new Object[] { OTRO_DOCENTE, 12L });
        when(horarioRepository.countLeccionesGroupedByDocente(DIRECCION, periodo.getId())).thenReturn(conteo);

        assertEquals(OTRO_DOCENTE, service.docenteDeMuestra(DIRECCION).orElseThrow().getId());
    }

    private HorarioLeccion leccion(int numero, LocalTime inicio, LocalTime fin, Materia materia, Long docenteId) {
        Usuario docente = new Usuario();
        docente.setId(docenteId);
        docente.setNombre("Docente " + docenteId);
        HorarioLeccion h = new HorarioLeccion();
        h.setNumeroLeccion(numero);
        h.setHoraInicio(inicio);
        h.setHoraFin(fin);
        h.setMateria(materia);
        h.setNivel(ocho);
        h.setDocente(docente);
        return h;
    }

    private EstudianteOpcion estudiante(Long id, String nombre, List<MateriaNota> materias) {
        return new EstudianteOpcion(id, nombre, "", 8, "2", ocho.getId(), nombre.substring(0, 1), "#2d5a87",
                materias, List.of(), List.of(), List.of());
    }

    private MateriaNota materia(Long id, String nombre, Double nota) {
        return new MateriaNota(id, nombre, "", List.of(periodo.getId()),
                List.of(new NotaPeriodo(periodo.getId(), nota, null, null)));
    }
}
