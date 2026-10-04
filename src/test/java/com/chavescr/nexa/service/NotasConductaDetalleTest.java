package com.chavescr.nexa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import com.chavescr.nexa.dto.NotasConductaDetalle;
import com.chavescr.nexa.dto.NotasConductaDetalle.Linea;
import com.chavescr.nexa.dto.NotasConductaDetalle.Registro;
import com.chavescr.nexa.entity.AsistenciaEstudiante.EstadoAsistencia;
import com.chavescr.nexa.entity.IncidenteConducta;
import com.chavescr.nexa.entity.IncidenteConducta.TipoIncidente;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.TipoRebaja;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.AsistenciaEstudianteRepository;
import com.chavescr.nexa.repository.ComponenteRepository;
import com.chavescr.nexa.repository.DistribucionPorcentualRepository;
import com.chavescr.nexa.repository.HorarioLeccionRepository;
import com.chavescr.nexa.repository.IncidenteConductaRepository;
import com.chavescr.nexa.repository.MateriaRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.ObservacionGuiaRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;
import com.chavescr.nexa.repository.ResultadoComponenteRepository;
import com.chavescr.nexa.repository.TipoComponenteRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class NotasConductaDetalleTest {

    private static final Long DIRECCION = 2L;
    private static final Long PERIODO = 1L;
    private static final Long ESTUDIANTE = 10L;

    @Mock
    private PeriodoAcademicoRepository periodoRepository;
    @Mock
    private NivelAcademicoRepository nivelRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private DocenteGuiaService docenteGuiaService;
    @Mock
    private HorarioLeccionRepository horarioRepository;
    @Mock
    private ComponenteRepository componenteRepository;
    @Mock
    private ResultadoComponenteRepository resultadoRepository;
    @Mock
    private DistribucionPorcentualRepository distribucionRepository;
    @Mock
    private AsistenciaEstudianteRepository asistenciaRepository;
    @Mock
    private TipoComponenteRepository tipoComponenteRepository;
    @Mock
    private IncidenteConductaRepository incidenteRepository;
    @Mock
    private ObservacionGuiaRepository observacionRepository;
    @Mock
    private MateriaRepository materiaRepository;
    @Mock
    private RebajaConductaService rebajaConductaService;

    private NotasConsultaService service;
    private PeriodoAcademico periodo;

    @BeforeEach
    void setUp() {
        service = new NotasConsultaService(periodoRepository, nivelRepository, usuarioRepository, docenteGuiaService,
                horarioRepository, componenteRepository, resultadoRepository, distribucionRepository,
                asistenciaRepository, tipoComponenteRepository, incidenteRepository, observacionRepository,
                materiaRepository, rebajaConductaService);

        NivelAcademico nivel = new NivelAcademico();
        nivel.setId(3L);
        Usuario estudiante = new Usuario();
        estudiante.setId(ESTUDIANTE);
        estudiante.setNivelAcademico(nivel);
        periodo = new PeriodoAcademico();
        periodo.setId(PERIODO);
        periodo.setCodigo("2026-I");
        periodo.setFechaInicio(LocalDate.of(2026, 2, 1));
        periodo.setFechaFin(LocalDate.of(2026, 6, 30));

        when(usuarioRepository.findEstudianteActivoConNivel(ESTUDIANTE, DIRECCION)).thenReturn(Optional.of(estudiante));
    }

    @Test
    void cadaAusenciaYTardiaMuestraLosPuntosQueCarga() {
        when(periodoRepository.findByIdAndDireccionId(PERIODO, DIRECCION)).thenReturn(Optional.of(periodo));
        when(rebajaConductaService.calculo(DIRECCION)).thenReturn(predeterminada(true));
        when(incidenteRepository.findByDireccionIdAndPeriodoIdAndEstudianteId(DIRECCION, PERIODO, ESTUDIANTE))
                .thenReturn(List.of(boleta(10), llamada()));
        when(asistenciaRepository.findAusenciasDeEstudiante(DIRECCION, ESTUDIANTE, periodo.getFechaInicio(),
                periodo.getFechaFin(), EstadoAsistencia.PRESENTE)).thenReturn(List.of(
                        fila(LocalDate.of(2026, 3, 2), EstadoAsistencia.AUSENTE, "Ciencias"),
                        fila(LocalDate.of(2026, 3, 3), EstadoAsistencia.AUSENTE, "Ciencias"),
                        fila(LocalDate.of(2026, 3, 4), EstadoAsistencia.TARDIA, "Matemática"),
                        fila(LocalDate.of(2026, 3, 5), EstadoAsistencia.JUSTIFICADA, "Ciencias")));

        NotasConductaDetalle detalle = service.detalleConducta(DIRECCION, null, true, ESTUDIANTE, PERIODO, null);

        assertEquals("73.00", detalle.getNota());
        assertTrue(detalle.isAsistenciaEnConducta());
        assertEquals(10, puntosDe(detalle, "Boletas"));
        assertEquals(5, puntosDe(detalle, "Llamadas de atención"));
        assertEquals(10, puntosDe(detalle, "Ausencias injustificadas"));
        assertEquals(0, puntosDe(detalle, "Ausencias justificadas"));
        assertEquals(2, puntosDe(detalle, "Tardías injustificadas"));
        List<Registro> registros = detalle.getCuentas().get(0).getRegistros();
        assertEquals(List.of(5, 5, 2, 0), registros.stream().map(Registro::getPuntos).toList());
        assertEquals("Matemática", registros.get(2).getMateria());
    }

    @Test
    void siRebajanElComponenteLaCuentaNoIncluyeAusencias() {
        when(periodoRepository.findByIdAndDireccionId(PERIODO, DIRECCION)).thenReturn(Optional.of(periodo));
        when(rebajaConductaService.calculo(DIRECCION)).thenReturn(predeterminada(false));
        when(incidenteRepository.findByDireccionIdAndPeriodoIdAndEstudianteId(DIRECCION, PERIODO, ESTUDIANTE))
                .thenReturn(List.of(boleta(10)));

        NotasConductaDetalle detalle = service.detalleConducta(DIRECCION, null, true, ESTUDIANTE, PERIODO, null);

        assertEquals("90.00", detalle.getNota());
        assertFalse(detalle.isAsistenciaEnConducta());
        assertEquals(List.of("Boletas", "Llamadas de atención"),
                detalle.getCuentas().get(0).getLineas().stream().map(Linea::getConcepto).toList());
        assertTrue(detalle.getCuentas().get(0).getRegistros().isEmpty());
        verifyNoInteractions(asistenciaRepository);
    }

    @Test
    void elAnioPromediaLaNotaDeCadaPeriodo() {
        PeriodoAcademico segundo = new PeriodoAcademico();
        segundo.setId(6L);
        segundo.setCodigo("2026-II");
        segundo.setFechaInicio(LocalDate.of(2026, 7, 1));
        segundo.setFechaFin(LocalDate.of(2026, 11, 30));
        when(periodoRepository.findByDireccionIdOrderByFechaInicioDesc(DIRECCION))
                .thenReturn(List.of(segundo, periodo));
        when(rebajaConductaService.calculo(DIRECCION)).thenReturn(predeterminada(false));
        when(incidenteRepository.findByDireccionIdAndPeriodoIdAndEstudianteId(DIRECCION, PERIODO, ESTUDIANTE))
                .thenReturn(List.of());
        when(incidenteRepository.findByDireccionIdAndPeriodoIdAndEstudianteId(DIRECCION, 6L, ESTUDIANTE))
                .thenReturn(List.of(llamada()));

        NotasConductaDetalle detalle = service.detalleConducta(DIRECCION, null, true, ESTUDIANTE, null, 2026);

        assertTrue(detalle.isAnio());
        assertEquals("97.50", detalle.getNota());
        assertEquals(List.of("2026-I", "2026-II"),
                detalle.getCuentas().stream().map(c -> c.getPeriodo()).toList());
    }

    private static int puntosDe(NotasConductaDetalle detalle, String concepto) {
        return detalle.getCuentas().get(0).getLineas().stream()
                .filter(linea -> concepto.equals(linea.getConcepto()))
                .mapToInt(Linea::getPuntos)
                .findFirst()
                .orElseThrow();
    }

    private static Object[] fila(LocalDate fecha, EstadoAsistencia estado, String materia) {
        return new Object[] { fecha, 1, estado, materia, null, null };
    }

    private static IncidenteConducta boleta(int puntos) {
        IncidenteConducta incidente = new IncidenteConducta();
        incidente.setTipo(TipoIncidente.BOLETA);
        incidente.setPuntosDescontados(puntos);
        return incidente;
    }

    private static IncidenteConducta llamada() {
        IncidenteConducta incidente = new IncidenteConducta();
        incidente.setTipo(TipoIncidente.LLAMADA_ATENCION);
        incidente.setPuntosDescontados(0);
        return incidente;
    }

    private static CalculoRebaja predeterminada(boolean enConducta) {
        Map<TipoRebaja, int[]> reglas = new EnumMap<>(TipoRebaja.class);
        for (TipoRebaja tipo : TipoRebaja.values()) {
            reglas.put(tipo, new int[] { tipo.cada(), tipo.puntos() });
        }
        return new CalculoRebaja(reglas, enConducta);
    }
}
