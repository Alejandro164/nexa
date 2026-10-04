package com.chavescr.nexa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chavescr.nexa.dto.FilaPromedio;
import com.chavescr.nexa.entity.AsistenciaEstudiante;
import com.chavescr.nexa.entity.AsistenciaEstudiante.EstadoAsistencia;
import com.chavescr.nexa.entity.ClaveComponente;
import com.chavescr.nexa.entity.Componente;
import com.chavescr.nexa.entity.DistribucionPorcentual;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.ResultadoComponente;
import com.chavescr.nexa.entity.TipoComponente;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.AsistenciaEstudianteRepository;
import com.chavescr.nexa.repository.MateriaRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;
import com.chavescr.nexa.repository.ResultadoComponenteRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class PromedioServiceTest {

    private static final Long DIRECCION = 2L;
    private static final Long NIVEL = 3L;
    private static final Long MATERIA = 4L;
    private static final Long PERIODO = 1L;
    private static final Long ESTUDIANTE = 10L;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private NivelAcademicoRepository nivelRepository;

    @Mock
    private MateriaRepository materiaRepository;

    @Mock
    private PeriodoAcademicoRepository periodoRepository;

    @Mock
    private ResultadoComponenteRepository resultadoRepository;

    @Mock
    private AsistenciaEstudianteRepository asistenciaRepository;

    @Mock
    private DistribucionPorcentualService distribucionService;

    @Mock
    private ComponenteService componenteService;

    @Mock
    private RebajaConductaService rebajaConductaService;

    private PromedioService service;
    private PeriodoAcademico periodo;
    private TipoComponente cotidiano;

    @BeforeEach
    void setUp() {
        service = new PromedioService(usuarioRepository, nivelRepository, materiaRepository, periodoRepository,
                resultadoRepository, asistenciaRepository, distribucionService, componenteService,
                rebajaConductaService);

        periodo = new PeriodoAcademico();
        periodo.setId(PERIODO);
        periodo.setFechaInicio(LocalDate.of(2026, 2, 1));
        periodo.setFechaFin(LocalDate.of(2026, 6, 30));

        Usuario estudiante = new Usuario();
        estudiante.setId(ESTUDIANTE);

        cotidiano = new TipoComponente();
        cotidiano.setClave(ClaveComponente.COTIDIANO);

        Componente rubro = new Componente();
        rubro.setId(20L);
        rubro.setClave(ClaveComponente.COTIDIANO);

        ResultadoComponente resultado = new ResultadoComponente();
        resultado.setEstudiante(estudiante);
        resultado.setComponente(rubro);
        resultado.setCalificacion(80);

        DistribucionPorcentual distribucion = new DistribucionPorcentual();
        distribucion.setCotidiano(40);
        distribucion.setTareas(15);
        distribucion.setProyectos(20);
        distribucion.setExamenes(20);
        distribucion.setAsistencia(5);

        when(periodoRepository.findByDireccionIdAndActivoTrueOrderByFechaInicioDesc(DIRECCION))
                .thenReturn(List.of(periodo));
        when(usuarioRepository.findEstudiantesActivosByNivelId(NIVEL)).thenReturn(List.of(estudiante));
        when(resultadoRepository.findByComponente_Direccion_IdAndComponente_Nivel_IdAndComponente_Materia_IdAndPeriodo_Id(
                DIRECCION, NIVEL, MATERIA, PERIODO)).thenReturn(List.of(resultado));
        when(distribucionService.obtenerDistribucion(DIRECCION, PERIODO, MATERIA)).thenReturn(distribucion);
        when(componenteService.listar(DIRECCION, ClaveComponente.COTIDIANO, NIVEL, MATERIA, null))
                .thenReturn(List.of(rubro));
        when(componenteService.calcularPesosEfectivos(List.of(rubro))).thenReturn(Map.of(20L, 100.0));
    }

    @Test
    void laAsistenciaPesaEnElPromedioCuandoRebajaElComponente() {
        when(rebajaConductaService.asistenciaRebajaComponente(DIRECCION)).thenReturn(true);
        when(asistenciaRepository.findByDireccionIdAndEstudianteIdAndMateriaIdAndFechaBetween(
                DIRECCION, ESTUDIANTE, MATERIA, periodo.getFechaInicio(), periodo.getFechaFin()))
                .thenReturn(List.of(leccion(EstadoAsistencia.PRESENTE), leccion(EstadoAsistencia.AUSENTE)));

        FilaPromedio fila = service.calcularPromedio(DIRECCION, NIVEL, MATERIA, List.of(cotidiano)).get(0);

        assertEquals(50, fila.getAsistencia());
        assertEquals(76.7, fila.getPromedioFinal());
    }

    @Test
    void laAsistenciaNoSeCalculaNiPesaCuandoRebajaLaConducta() {
        when(rebajaConductaService.asistenciaRebajaComponente(DIRECCION)).thenReturn(false);

        FilaPromedio fila = service.calcularPromedio(DIRECCION, NIVEL, MATERIA, List.of(cotidiano)).get(0);

        assertNull(fila.getAsistencia());
        assertEquals(80.0, fila.getPromedioFinal());
        verifyNoInteractions(asistenciaRepository);
    }

    private static AsistenciaEstudiante leccion(EstadoAsistencia estado) {
        AsistenciaEstudiante leccion = new AsistenciaEstudiante();
        leccion.setEstado(estado);
        return leccion;
    }
}
