package com.chavescr.nexa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chavescr.nexa.dto.FilaNotaConducta;
import com.chavescr.nexa.dto.VistaEscala;
import com.chavescr.nexa.entity.AsistenciaEstudiante.EstadoAsistencia;
import com.chavescr.nexa.entity.IncidenteConducta;
import com.chavescr.nexa.entity.IncidenteConducta.TipoIncidente;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.TipoRebaja;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.AsistenciaEstudianteRepository;
import com.chavescr.nexa.repository.DireccionRepository;
import com.chavescr.nexa.repository.IncidenteConductaRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.NotaConductaRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class NotaConductaServiceTest {

    private static final Long DIRECCION = 2L;
    private static final Long PERIODO = 5L;
    private static final Long ESTUDIANTE = 10L;

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PeriodoAcademicoRepository periodoRepository;
    @Mock
    private NivelAcademicoRepository nivelRepository;
    @Mock
    private IncidenteConductaRepository incidenteRepository;
    @Mock
    private NotaConductaRepository notaRepository;
    @Mock
    private DireccionRepository direccionRepository;
    @Mock
    private DocenteGuiaService docenteGuiaService;
    @Mock
    private AlcanceDocenteService alcanceDocenteService;
    @Mock
    private NotificacionService notificacionService;
    @Mock
    private EscalaNotasService escalaNotasService;
    @Mock
    private RebajaConductaService rebajaConductaService;
    @Mock
    private AsistenciaEstudianteRepository asistenciaRepository;

    private NotaConductaService service;
    private PeriodoAcademico periodo;
    private Usuario estudiante;

    @BeforeEach
    void setUp() {
        service = new NotaConductaService(usuarioRepository, periodoRepository, nivelRepository, incidenteRepository,
                notaRepository, direccionRepository, docenteGuiaService, alcanceDocenteService, notificacionService,
                escalaNotasService, rebajaConductaService, asistenciaRepository);

        NivelAcademico nivel = new NivelAcademico();
        nivel.setGrado(7);
        nivel.setSeccion("A");

        estudiante = new Usuario();
        estudiante.setId(ESTUDIANTE);
        estudiante.setNombre("Ana Solís");
        estudiante.setNivelAcademico(nivel);

        periodo = new PeriodoAcademico();
        periodo.setId(PERIODO);
        periodo.setCodigo("2026-II");
        periodo.setActivo(true);
        periodo.setFechaInicio(LocalDate.of(2026, 7, 1));
        periodo.setFechaFin(LocalDate.of(2026, 11, 30));
    }

    @Test
    void lasAusenciasYTardiasRebajanLaNotaJuntoConLasBoletas() {
        prepararPanel(calculo(true));
        IncidenteConducta boleta = new IncidenteConducta();
        boleta.setTipo(TipoIncidente.BOLETA);
        boleta.setPuntosDescontados(10);
        when(incidenteRepository.findDeEstudiantes(DIRECCION, PERIODO, List.of(ESTUDIANTE)))
                .thenReturn(List.<Object[]>of(new Object[] { ESTUDIANTE, boleta }));
        when(asistenciaRepository.contarEstadosPorEstudiante(eq(DIRECCION), eq(List.of(ESTUDIANTE)),
                eq(periodo.getFechaInicio()), eq(periodo.getFechaFin()), eq(EstadoAsistencia.PRESENTE)))
                .thenReturn(List.of(
                        new Object[] { ESTUDIANTE, EstadoAsistencia.AUSENTE, 2L },
                        new Object[] { ESTUDIANTE, EstadoAsistencia.TARDIA, 1L }));

        FilaNotaConducta fila = service.cargarPanel(DIRECCION, PERIODO, null, null, null).getFilas().get(0);

        assertEquals(78, fila.getNota());
        assertEquals("Conducta deficiente. 1 boleta por falta grave. Debe mejorar urgentemente su comportamiento."
                + " Las ausencias y tardías rebajan 12 puntos.", fila.getObservaciones());
    }

    @Test
    void unaLlamadaBajaLosPuntosDeLaRegla() {
        prepararPanel(calculo(true));
        IncidenteConducta llamada = new IncidenteConducta();
        llamada.setTipo(TipoIncidente.LLAMADA_ATENCION);
        llamada.setPuntosDescontados(0);
        when(incidenteRepository.findDeEstudiantes(DIRECCION, PERIODO, List.of(ESTUDIANTE)))
                .thenReturn(List.<Object[]>of(new Object[] { ESTUDIANTE, llamada }));
        when(asistenciaRepository.contarEstadosPorEstudiante(any(), any(), any(), any(), any()))
                .thenReturn(List.<Object[]>of());

        FilaNotaConducta fila = service.cargarPanel(DIRECCION, PERIODO, null, null, null).getFilas().get(0);

        assertEquals(95, fila.getNota());
    }

    @Test
    void sinBoletasLaNotaExplicaLosPuntosDeAsistencia() {
        prepararPanel(calculo(true));
        when(incidenteRepository.findDeEstudiantes(DIRECCION, PERIODO, List.of(ESTUDIANTE))).thenReturn(List.of());
        when(asistenciaRepository.contarEstadosPorEstudiante(any(), any(), any(), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[] { ESTUDIANTE, EstadoAsistencia.AUSENTE, 1L }));

        FilaNotaConducta fila = service.cargarPanel(DIRECCION, PERIODO, null, null, null).getFilas().get(0);

        assertEquals(95, fila.getNota());
        assertEquals("Las ausencias y tardías rebajan 5 puntos de la nota.", fila.getObservaciones());
    }

    @Test
    void siRebajanElComponenteLaConductaNoConsultaLaAsistencia() {
        prepararPanel(calculo(false));
        when(incidenteRepository.findDeEstudiantes(DIRECCION, PERIODO, List.of(ESTUDIANTE))).thenReturn(List.of());

        FilaNotaConducta fila = service.cargarPanel(DIRECCION, PERIODO, null, null, null).getFilas().get(0);

        assertEquals(100, fila.getNota());
        verifyNoInteractions(asistenciaRepository);
    }

    private void prepararPanel(CalculoRebaja calculo) {
        when(periodoRepository.findByDireccionIdOrderByFechaInicioDesc(DIRECCION)).thenReturn(List.of(periodo));
        when(nivelRepository.findByDireccionIdAndActivoTrueOrderByGradoAscSeccionAsc(DIRECCION)).thenReturn(List.of());
        when(escalaNotasService.vista(DIRECCION)).thenReturn(new VistaEscala(List.of(
                new VistaEscala.Tramo("Excelente", 90, "excelente"),
                new VistaEscala.Tramo("Bueno", 80, "bueno"),
                new VistaEscala.Tramo("Regular", 70, "regular"),
                new VistaEscala.Tramo("Aplazado", 0, "aplazado"))));
        when(usuarioRepository.findEstudiantesActivosConNivel(DIRECCION, null, null)).thenReturn(List.of(estudiante));
        when(notaRepository.findEstudianteIdsEnviados(DIRECCION, PERIODO, List.of(ESTUDIANTE))).thenReturn(List.of());
        when(rebajaConductaService.calculo(DIRECCION)).thenReturn(calculo);
    }

    private static CalculoRebaja calculo(boolean enConducta) {
        Map<TipoRebaja, int[]> reglas = new EnumMap<>(TipoRebaja.class);
        for (TipoRebaja tipo : TipoRebaja.values()) {
            reglas.put(tipo, new int[] { tipo.cada(), tipo.puntos() });
        }
        return new CalculoRebaja(reglas, enConducta);
    }
}
