package com.chavescr.nexa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chavescr.nexa.dto.NombresRubros;
import com.chavescr.nexa.dto.PestanaRubro;
import com.chavescr.nexa.entity.ClaveComponente;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.TipoComponente;

@ExtendWith(MockitoExtension.class)
class GestionAcademicaServiceTest {

    private static final Long DIRECCION = 2L;
    private static final Long MATERIA = 4L;

    @Mock
    private AlcanceDocenteService alcanceDocenteService;

    @Mock
    private TipoComponenteService tipoComponenteService;

    @Mock
    private ComponenteEvaluacionService componenteEvaluacionService;

    private GestionAcademicaService service;

    @BeforeEach
    void setUp() {
        service = new GestionAcademicaService(alcanceDocenteService, tipoComponenteService,
                componenteEvaluacionService);
    }

    @Test
    void soloHayPestanasDeLosRubrosQueCalificaElComponente() {
        when(tipoComponenteService.listarActivos(DIRECCION)).thenReturn(List.of(
                tipo(ClaveComponente.COTIDIANO, "Cotidiano", "📝"),
                tipo(ClaveComponente.TAREA, "Tareas", "📚"),
                tipo(ClaveComponente.PROYECTO, "Proyecto", "🎯"),
                tipo(ClaveComponente.EXAMEN, "Pruebas", "📋"),
                tipo(null, "Laboratorio", null)));
        // III Ciclo — Básicas: sin proyecto.
        when(componenteEvaluacionService.pesosDeMateria(DIRECCION, MATERIA)).thenReturn(new int[] { 45, 10, 0, 40, 5 });
        when(componenteEvaluacionService.nombresDeMateria(DIRECCION, MATERIA))
                .thenReturn(new NombresRubros(null, null, null, "Exámenes", null));

        List<PestanaRubro> pestanas = service.pestanas(DIRECCION, MATERIA);

        assertEquals(List.of("COTIDIANO", "TAREA", "EXAMEN"), pestanas.stream().map(PestanaRubro::getId).toList());
        PestanaRubro pruebas = pestanas.get(2);
        assertEquals("Exámenes", pruebas.getNombre());
        assertEquals("📋 Exámenes", pruebas.getEtiqueta());
        assertEquals(40, pruebas.getPorcentaje());
        assertEquals("/gestion-academica/examenes", pruebas.getUrl());
        assertEquals("examenes-panel", pruebas.getPanelId());
        assertEquals("Cotidiano", pestanas.get(0).getNombre());
    }

    @Test
    void laPestanaUsaElEmojiDelComponenteOElDeLaInstitucion() {
        when(tipoComponenteService.listarActivos(DIRECCION)).thenReturn(List.of(
                tipo(ClaveComponente.COTIDIANO, "Cotidiano", "📝"),
                tipo(ClaveComponente.EXAMEN, "Pruebas", "📋")));
        when(componenteEvaluacionService.pesosDeMateria(DIRECCION, MATERIA)).thenReturn(new int[] { 60, 0, 0, 35, 5 });
        when(componenteEvaluacionService.nombresDeMateria(DIRECCION, MATERIA)).thenReturn(
                new NombresRubros(null, null, null, "Exámenes", null, null, null, null, "🧪", null));

        List<PestanaRubro> pestanas = service.pestanas(DIRECCION, MATERIA);

        assertEquals("📝 Cotidiano", pestanas.get(0).getEtiqueta());
        assertEquals("🧪 Exámenes", pestanas.get(1).getEtiqueta());
    }

    @Test
    void sinMateriaNoHayPestanas() {
        assertTrue(service.pestanas(DIRECCION, null).isEmpty());
        verifyNoInteractions(componenteEvaluacionService, tipoComponenteService);
    }

    @Test
    void eligeLaMateriaPedidaLaRecordadaOLaPrimera() {
        List<Materia> materias = List.of(materia(4L), materia(7L), materia(9L));

        assertEquals(7L, service.elegirMateria(materias, 7L, 9L));
        assertEquals(9L, service.elegirMateria(materias, 99L, 9L));
        assertEquals(4L, service.elegirMateria(materias, null, 55L));
        assertNull(service.elegirMateria(List.of(), 7L, 9L));
    }

    private static TipoComponente tipo(ClaveComponente clave, String nombre, String emoji) {
        TipoComponente tipo = new TipoComponente();
        tipo.setClave(clave);
        tipo.setNombre(nombre);
        tipo.setEmoji(emoji);
        return tipo;
    }

    private static Materia materia(Long id) {
        Materia materia = new Materia();
        materia.setId(id);
        return materia;
    }
}
