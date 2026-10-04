package com.chavescr.nexa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chavescr.nexa.dto.NotasDesgloseFila;
import com.chavescr.nexa.entity.AsistenciaEstudiante.EstadoAsistencia;
import com.chavescr.nexa.entity.ClaveComponente;
import com.chavescr.nexa.entity.Componente;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.ResultadoComponente;
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
class NotasConsultaServiceTest {

    private static final Long DIRECCION = 2L;
    private static final Long NIVEL = 3L;
    private static final Long MATERIA = 4L;
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
    private NivelAcademico nivel;
    private Materia materia;

    @BeforeEach
    void setUp() {
        service = new NotasConsultaService(periodoRepository, nivelRepository, usuarioRepository, docenteGuiaService,
                horarioRepository, componenteRepository, resultadoRepository, distribucionRepository,
                asistenciaRepository, tipoComponenteRepository, incidenteRepository, observacionRepository,
                materiaRepository, rebajaConductaService);

        nivel = new NivelAcademico();
        nivel.setId(NIVEL);

        materia = new Materia();
        materia.setId(MATERIA);
        materia.setNombre("Ciencias");

        Usuario estudiante = new Usuario();
        estudiante.setId(ESTUDIANTE);
        estudiante.setNivelAcademico(nivel);

        periodo = new PeriodoAcademico();
        periodo.setId(PERIODO);
        periodo.setFechaInicio(LocalDate.of(2026, 2, 1));
        periodo.setFechaFin(LocalDate.of(2026, 6, 30));

        when(usuarioRepository.findEstudianteActivoConNivel(ESTUDIANTE, DIRECCION))
                .thenReturn(Optional.of(estudiante));
        when(periodoRepository.findByIdAndDireccionId(PERIODO, DIRECCION)).thenReturn(Optional.of(periodo));
        when(periodoRepository.findByDireccionIdOrderByFechaInicioDesc(DIRECCION)).thenReturn(List.of(periodo));
        when(horarioRepository.findAsignacionesNivelMateria(DIRECCION, List.of(PERIODO), List.of(NIVEL)))
                .thenReturn(List.<Object[]>of(new Object[] { NIVEL, MATERIA, "Ciencias", "Prof. Laura", PERIODO }));
    }

    @Test
    void elDesgloseIncluyeLaAsistenciaCuandoRebajaElComponente() {
        conCotidianoDe80();
        when(rebajaConductaService.asistenciaRebajaComponente(DIRECCION)).thenReturn(true);
        when(asistenciaRepository.findEstadosDeEstudiante(DIRECCION, ESTUDIANTE, periodo.getFechaInicio(),
                periodo.getFechaFin())).thenReturn(List.of(
                        new Object[] { MATERIA, EstadoAsistencia.PRESENTE },
                        new Object[] { MATERIA, EstadoAsistencia.AUSENTE }));

        NotasDesgloseFila fila = service.desglose(DIRECCION, null, true, ESTUDIANTE, PERIODO).get(0);

        assertEquals(80, fila.getCotidiano());
        assertEquals(50, fila.getAsistencia());
        assertEquals(76.7, fila.getPromedio());
    }

    @Test
    void elDesgloseDejaFueraLaAsistenciaCuandoRebajaLaConducta() {
        conCotidianoDe80();
        when(rebajaConductaService.asistenciaRebajaComponente(DIRECCION)).thenReturn(false);

        NotasDesgloseFila fila = service.desglose(DIRECCION, null, true, ESTUDIANTE, PERIODO).get(0);

        assertEquals(80, fila.getCotidiano());
        assertNull(fila.getAsistencia());
        assertEquals(80.0, fila.getPromedio());
        verifyNoInteractions(asistenciaRepository);
    }

    @Test
    void elDetalleDeAsistenciaNoSeAbreCuandoRebajaLaConducta() {
        when(materiaRepository.findByIdAndDireccionId(MATERIA, DIRECCION)).thenReturn(Optional.of(materia));
        when(rebajaConductaService.asistenciaRebajaComponente(DIRECCION)).thenReturn(false);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.detalleMateria(DIRECCION, null, true, ESTUDIANTE, PERIODO, MATERIA, "ASISTENCIA"));

        assertEquals("La asistencia no forma parte de la nota: las ausencias y tardías rebajan la conducta",
                error.getMessage());
        verifyNoInteractions(asistenciaRepository);
    }

    private void conCotidianoDe80() {
        Componente rubro = new Componente();
        rubro.setId(20L);
        rubro.setClave(ClaveComponente.COTIDIANO);
        rubro.setNivel(nivel);
        rubro.setMateria(materia);

        ResultadoComponente resultado = new ResultadoComponente();
        resultado.setComponente(rubro);
        resultado.setCalificacion(80);

        when(resultadoRepository.findDeEstudianteEnPeriodo(DIRECCION, NIVEL, ESTUDIANTE, PERIODO))
                .thenReturn(List.of(resultado));
        when(componenteRepository.findParaNotas(DIRECCION, List.of(NIVEL))).thenReturn(List.of(rubro));
    }
}
