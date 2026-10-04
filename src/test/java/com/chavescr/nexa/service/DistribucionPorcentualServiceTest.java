package com.chavescr.nexa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chavescr.nexa.entity.DistribucionPorcentual;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.repository.DistribucionPorcentualRepository;
import com.chavescr.nexa.repository.MateriaRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;

@ExtendWith(MockitoExtension.class)
class DistribucionPorcentualServiceTest {

    private static final Long DIRECCION = 2L;
    private static final Long PERIODO = 1L;
    private static final Long MATERIA = 4L;

    @Mock
    private DistribucionPorcentualRepository distribucionRepository;

    @Mock
    private PeriodoAcademicoRepository periodoRepository;

    @Mock
    private MateriaRepository materiaRepository;

    @Mock
    private RebajaConductaService rebajaConductaService;

    private DistribucionPorcentualService service;

    @BeforeEach
    void setUp() {
        service = new DistribucionPorcentualService(distribucionRepository, periodoRepository, materiaRepository,
                rebajaConductaService);
    }

    @Test
    void laPredeterminadaIncluyeLaAsistenciaCuandoRebajaElComponente() {
        when(rebajaConductaService.asistenciaRebajaComponente(DIRECCION)).thenReturn(true);

        DistribucionPorcentual predeterminada = service.obtenerDistribucion(DIRECCION, PERIODO, MATERIA);

        assertEquals(List.of(40, 15, 20, 20, 5), pesos(predeterminada));
    }

    @Test
    void sinAsistenciaLaPredeterminadaRepartePorProporcion() {
        when(rebajaConductaService.asistenciaRebajaComponente(DIRECCION)).thenReturn(false);

        DistribucionPorcentual predeterminada = service.obtenerDistribucion(DIRECCION, PERIODO, MATERIA);

        assertEquals(List.of(42, 16, 21, 21, 0), pesos(predeterminada));
        assertEquals(100, predeterminada.getTotal());
    }

    @Test
    void guardaLaAsistenciaCuandoRebajaElComponente() {
        when(rebajaConductaService.asistenciaRebajaComponente(DIRECCION)).thenReturn(true);
        prepararGuardado();

        DistribucionPorcentual guardada = service.guardarDistribucion(DIRECCION, PERIODO, MATERIA, 40, 15, 20, 20, 5);

        assertEquals(5, guardada.getAsistencia());
    }

    @Test
    void ignoraLaAsistenciaCuandoRebajaLaConducta() {
        when(rebajaConductaService.asistenciaRebajaComponente(DIRECCION)).thenReturn(false);
        prepararGuardado();

        DistribucionPorcentual guardada = service.guardarDistribucion(DIRECCION, PERIODO, MATERIA, 45, 15, 20, 20, 5);

        assertEquals(0, guardada.getAsistencia());
    }

    @Test
    void sinAsistenciaLosCuatroComponentesDebenSumar100() {
        when(rebajaConductaService.asistenciaRebajaComponente(DIRECCION)).thenReturn(false);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.guardarDistribucion(DIRECCION, PERIODO, MATERIA, 40, 15, 20, 20, 5));

        assertEquals("La suma de los porcentajes debe ser exactamente 100% (actual: 95%)", error.getMessage());
    }

    private static List<Integer> pesos(DistribucionPorcentual distribucion) {
        return List.of(distribucion.getCotidiano(), distribucion.getTareas(), distribucion.getProyectos(),
                distribucion.getExamenes(), distribucion.getAsistencia());
    }

    private void prepararGuardado() {
        when(periodoRepository.findByIdAndDireccionId(PERIODO, DIRECCION))
                .thenReturn(Optional.of(new PeriodoAcademico()));
        when(materiaRepository.findByIdAndDireccionId(MATERIA, DIRECCION)).thenReturn(Optional.of(new Materia()));
        when(distribucionRepository.save(any(DistribucionPorcentual.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }
}
