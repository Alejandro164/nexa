package com.chavescr.nexa.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chavescr.nexa.entity.ComponenteEvaluacion;
import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.repository.ComponenteEvaluacionRepository;
import com.chavescr.nexa.repository.DireccionRepository;
import com.chavescr.nexa.repository.MateriaRepository;

@ExtendWith(MockitoExtension.class)
class ComponenteEvaluacionServiceTest {

    private static final Long DIRECCION = 2L;
    private static final Long MATERIA = 4L;

    @Mock
    private ComponenteEvaluacionRepository repository;

    @Mock
    private DireccionRepository direccionRepository;

    @Mock
    private MateriaRepository materiaRepository;

    private ComponenteEvaluacionService service;

    @BeforeEach
    void setUp() {
        service = new ComponenteEvaluacionService(repository, direccionRepository, materiaRepository);
    }

    @Test
    void unaDireccionSinComponentesRecibeLosPredeterminados() {
        when(repository.existsByDireccionId(DIRECCION)).thenReturn(false);
        when(direccionRepository.findById(DIRECCION)).thenReturn(Optional.of(new Direccion()));

        service.listar(DIRECCION);

        ArgumentCaptor<ComponenteEvaluacion> guardados = ArgumentCaptor.forClass(ComponenteEvaluacion.class);
        verify(repository, times(7)).saveAndFlush(guardados.capture());
        ComponenteEvaluacion general = guardados.getAllValues().get(0);
        assertEquals(ComponenteEvaluacionService.NOMBRE_GENERAL, general.getNombre());
        assertArrayEquals(new int[] { 40, 15, 20, 20, 5 }, general.pesos());
        guardados.getAllValues().forEach(componente -> assertEquals(100, componente.getTotal()));
    }

    @Test
    void noSiembraSiLaDireccionYaTieneComponentes() {
        when(repository.existsByDireccionId(DIRECCION)).thenReturn(true);

        service.listar(DIRECCION);

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void losPorcentajesDebenSumar100() {
        ComponenteEvaluacion datos = componente("Talleres", 40, 15, 20, 20, 0);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.guardar(DIRECCION, datos));

        assertEquals("La suma de los porcentajes debe ser exactamente 100% (actual: 95%)", error.getMessage());
    }

    @Test
    void guardaUnComponenteNuevoEnLaDireccion() {
        Direccion direccion = new Direccion();
        when(direccionRepository.findById(DIRECCION)).thenReturn(Optional.of(direccion));
        when(repository.findByDireccionIdOrderByOrdenAscNombreAsc(DIRECCION)).thenReturn(List.of());
        when(repository.save(any(ComponenteEvaluacion.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        ComponenteEvaluacion guardado = service.guardar(DIRECCION, componente("  III   Ciclo — Básicas ", 45, 10, 0, 40, 5));

        assertEquals("III Ciclo — Básicas", guardado.getNombre());
        assertEquals(direccion, guardado.getDireccion());
        assertArrayEquals(new int[] { 45, 10, 0, 40, 5 }, guardado.pesos());
    }

    @Test
    void noSeRepiteElNombreEnLaDireccion() {
        when(repository.existsByDireccionIdAndNombreIgnoreCase(DIRECCION, "General")).thenReturn(true);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.guardar(DIRECCION, componente("General", 40, 15, 20, 20, 5)));

        assertEquals("Ya existe un componente con ese nombre", error.getMessage());
    }

    @Test
    void noSeEliminaUnComponenteConMaterias() {
        when(repository.findByIdAndDireccionId(9L, DIRECCION)).thenReturn(Optional.of(new ComponenteEvaluacion()));
        when(materiaRepository.existsByComponenteEvaluacionId(9L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.eliminar(DIRECCION, 9L));

        verify(repository, never()).delete(any());
    }

    @Test
    void laMateriaTomaLosPesosDeSuComponente() {
        Materia materia = new Materia();
        materia.setComponenteEvaluacion(componente("Religión", 70, 25, 0, 0, 5));
        when(materiaRepository.findByIdAndDireccionId(MATERIA, DIRECCION)).thenReturn(Optional.of(materia));

        assertArrayEquals(new int[] { 70, 25, 0, 0, 5 }, service.pesosDeMateria(DIRECCION, MATERIA));
    }

    @Test
    void unaMateriaSinComponenteUsaElRepartoPorDefecto() {
        when(materiaRepository.findByIdAndDireccionId(MATERIA, DIRECCION)).thenReturn(Optional.of(new Materia()));

        assertArrayEquals(new int[] { 40, 15, 20, 20, 5 }, service.pesosDeMateria(DIRECCION, MATERIA));
    }

    private static ComponenteEvaluacion componente(String nombre, int cotidiano, int tareas, int proyecto,
            int pruebas, int asistencia) {
        ComponenteEvaluacion componente = new ComponenteEvaluacion();
        componente.setNombre(nombre);
        componente.setCotidiano(cotidiano);
        componente.setTareas(tareas);
        componente.setProyecto(proyecto);
        componente.setPruebas(pruebas);
        componente.setAsistencia(asistencia);
        return componente;
    }
}
