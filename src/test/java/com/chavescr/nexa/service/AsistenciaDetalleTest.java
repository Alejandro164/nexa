package com.chavescr.nexa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chavescr.nexa.dto.DetalleAsistencia;
import com.chavescr.nexa.dto.DetalleAsistencia.Cuadro;
import com.chavescr.nexa.dto.DetalleAsistencia.Fila;
import com.chavescr.nexa.entity.AsistenciaEstudiante.EstadoAsistencia;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.TipoRebaja;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.AsistenciaEstudianteRepository;
import com.chavescr.nexa.repository.HorarioLeccionRepository;
import com.chavescr.nexa.repository.MateriaRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AsistenciaDetalleTest {

    private static final Long DIRECCION = 2L;
    private static final Long ESTUDIANTE = 10L;

    @Mock
    private AsistenciaEstudianteRepository asistenciaRepository;
    @Mock
    private NivelAcademicoRepository nivelAcademicoRepository;
    @Mock
    private MateriaRepository materiaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PeriodoAcademicoRepository periodoRepository;
    @Mock
    private HorarioLeccionRepository horarioLeccionRepository;
    @Mock
    private AlmacenamientoService almacenamientoService;
    @Mock
    private AlcanceDocenteService alcanceDocenteService;
    @Mock
    private RebajaConductaService rebajaConductaService;

    private AsistenciaService service;
    private PeriodoAcademico primero;
    private PeriodoAcademico segundo;

    @BeforeEach
    void setUp() {
        service = new AsistenciaService(asistenciaRepository, nivelAcademicoRepository, materiaRepository,
                usuarioRepository, periodoRepository, horarioLeccionRepository, almacenamientoService,
                alcanceDocenteService, rebajaConductaService);
        primero = periodo(1L, "2026-I", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 6, 30));
        segundo = periodo(6L, "2026-II", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 11, 30));
    }

    @Test
    void reparteTardiasYAusenciasPorPeriodoConLosPuntosDeLaInstitucion() {
        when(usuarioRepository.findEstudianteActivoConNivel(ESTUDIANTE, DIRECCION)).thenReturn(Optional.of(estudiante()));
        when(periodoRepository.findByDireccionIdAndActivoTrueOrderByFechaInicioDesc(DIRECCION))
                .thenReturn(List.of(primero));
        when(periodoRepository.findByDireccionIdOrderByFechaInicioDesc(DIRECCION))
                .thenReturn(List.of(segundo, primero));
        when(rebajaConductaService.calculo(DIRECCION)).thenReturn(predeterminada());
        when(asistenciaRepository.findAusenciasDeEstudiante(DIRECCION, ESTUDIANTE, primero.getFechaInicio(),
                segundo.getFechaFin(), EstadoAsistencia.PRESENTE)).thenReturn(List.of(
                        fila(LocalDate.of(2026, 3, 12), 2, EstadoAsistencia.TARDIA, "Matemáticas", null, "María González"),
                        fila(LocalDate.of(2026, 3, 18), 1, EstadoAsistencia.TARDIA_JUSTIFICADA, "Español",
                                "Cita médica", "María González"),
                        fila(LocalDate.of(2026, 4, 2), 3, EstadoAsistencia.AUSENTE, "Ciencias", null, "Carlos López"),
                        fila(LocalDate.of(2026, 8, 4), 2, EstadoAsistencia.TARDIA, "Inglés", null, "Carlos López")));

        DetalleAsistencia detalle = service.detalleEstudiante(DIRECCION, ESTUDIANTE, null);

        assertEquals("Ana Rodríguez", detalle.getNombre());
        assertEquals("AR", detalle.getIniciales());
        assertEquals("4-5678-9012 · Sección 7 - A", detalle.getMeta());
        assertEquals(List.of("2026-I", "2026-II"),
                detalle.getPeriodos().stream().map(DetalleAsistencia.Periodo::getCodigo).toList());

        DetalleAsistencia.Periodo uno = detalle.getPeriodos().get(0);
        assertEquals(1, cantidad(uno, "Ausencias injustificadas"));
        assertEquals(5, puntos(uno, "Ausencias injustificadas"));
        assertEquals(0, puntos(uno, "Ausencias justificadas"));
        assertEquals(2, puntos(uno, "Tardías injustificadas"));
        assertEquals(0, puntos(uno, "Tardías justificadas"));
        Fila cita = uno.getFilas().get(1);
        assertEquals("1", cita.getLeccion());
        assertEquals("Cita médica", cita.getDetalle());
        assertEquals("María González", cita.getDocente());
        assertEquals("Tardía justificada", cita.getEstado());

        DetalleAsistencia.Periodo dos = detalle.getPeriodos().get(1);
        assertEquals(1, cantidad(dos, "Tardías injustificadas"));
        assertEquals(2, puntos(dos, "Tardías injustificadas"));
        assertEquals(0, cantidad(dos, "Ausencias injustificadas"));
        verifyNoInteractions(alcanceDocenteService);
    }

    @Test
    void unDocenteNoVeEstudiantesDeOtraSeccion() {
        when(usuarioRepository.findEstudianteActivoConNivel(ESTUDIANTE, DIRECCION)).thenReturn(Optional.of(estudiante()));
        NivelAcademico otra = new NivelAcademico();
        otra.setId(99L);
        when(alcanceDocenteService.nivelesVisibles(DIRECCION, 7L)).thenReturn(List.of(otra));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.detalleEstudiante(DIRECCION, ESTUDIANTE, 7L));

        assertEquals("No puede consultar la asistencia de este estudiante", error.getMessage());
        verifyNoInteractions(asistenciaRepository);
    }

    @Test
    void elPeriodoPorDefectoEsElQueContieneHoyOElMasCercano() {
        assertEquals("2026-II", AsistenciaService.periodoPorDefecto(List.of(primero, segundo),
                LocalDate.of(2026, 10, 4)).getCodigo());
        assertEquals("2026-I", AsistenciaService.periodoPorDefecto(List.of(primero, segundo),
                LocalDate.of(2026, 1, 15)).getCodigo());
        assertEquals("2026-II", AsistenciaService.periodoPorDefecto(List.of(primero, segundo),
                LocalDate.of(2026, 12, 20)).getCodigo());
        assertEquals("2026-II", AsistenciaService.periodoPorDefecto(List.of(primero, segundo),
                LocalDate.of(2026, 7, 1)).getCodigo());
        PeriodoAcademico tercero = periodo(8L, "2026-III", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 30));
        assertEquals("2026-I", AsistenciaService.periodoPorDefecto(List.of(primero, tercero),
                LocalDate.of(2026, 7, 15)).getCodigo());
        assertEquals("2026-III", AsistenciaService.periodoPorDefecto(List.of(primero, tercero),
                LocalDate.of(2026, 8, 20)).getCodigo());
    }

    private static int cantidad(DetalleAsistencia.Periodo periodo, String etiqueta) {
        return cuadro(periodo, etiqueta).getCantidad();
    }

    private static int puntos(DetalleAsistencia.Periodo periodo, String etiqueta) {
        return cuadro(periodo, etiqueta).getPuntos();
    }

    private static Cuadro cuadro(DetalleAsistencia.Periodo periodo, String etiqueta) {
        return periodo.getCuadros().stream().filter(c -> etiqueta.equals(c.getEtiqueta())).findFirst().orElseThrow();
    }

    private static Usuario estudiante() {
        NivelAcademico nivel = new NivelAcademico();
        nivel.setId(3L);
        nivel.setGrado(7);
        nivel.setSeccion("A");
        Usuario estudiante = new Usuario();
        estudiante.setId(ESTUDIANTE);
        estudiante.setNombre("Ana Rodríguez");
        estudiante.setCedula("4-5678-9012");
        estudiante.setNivelAcademico(nivel);
        return estudiante;
    }

    private static PeriodoAcademico periodo(Long id, String codigo, LocalDate inicio, LocalDate fin) {
        PeriodoAcademico periodo = new PeriodoAcademico();
        periodo.setId(id);
        periodo.setCodigo(codigo);
        periodo.setFechaInicio(inicio);
        periodo.setFechaFin(fin);
        return periodo;
    }

    private static Object[] fila(LocalDate fecha, int leccion, EstadoAsistencia estado, String materia,
            String detalle, String docente) {
        return new Object[] { fecha, leccion, estado, materia, detalle, docente };
    }

    private static CalculoRebaja predeterminada() {
        Map<TipoRebaja, int[]> reglas = new EnumMap<>(TipoRebaja.class);
        for (TipoRebaja tipo : TipoRebaja.values()) {
            reglas.put(tipo, new int[] { tipo.cada(), tipo.puntos() });
        }
        return new CalculoRebaja(reglas, true);
    }
}
