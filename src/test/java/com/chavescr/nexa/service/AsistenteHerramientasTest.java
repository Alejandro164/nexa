package com.chavescr.nexa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chavescr.nexa.dto.ContextoAsistente;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AsistenteHerramientasTest {

    private static final Long DIRECCION = 1L;
    private static final Long PADRE = 50L;
    private static final Long DOCENTE = 10L;

    @Mock
    private DashboardService dashboardService;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private DocenteGuiaService docenteGuiaService;
    @Mock
    private ConfiguracionAcademicaService configuracionAcademicaService;

    @InjectMocks
    private AsistenteHerramientas herramientas;

    private NivelAcademico ochoDos;
    private NivelAcademico nueveUno;
    private Usuario hijo;
    private Usuario ajeno;

    @BeforeEach
    void preparar() {
        ochoDos = nivel(80L, 8, "2");
        nueveUno = nivel(91L, 9, "1");
        hijo = estudiante(1L, "José Ramírez", ochoDos);
        ajeno = estudiante(2L, "Ana Mora", nueveUno);
        lenient().when(configuracionAcademicaService.listarNivelesActivos(DIRECCION))
                .thenReturn(List.of(ochoDos, nueveUno));
    }

    @Test
    void soloLaDireccionRecibeLaHerramientaDeRendimiento() {
        assertTrue(herramientas.nombres(ctx("ROLE_DIRECTOR")).contains(AsistenteHerramientas.RENDIMIENTO));
        assertTrue(herramientas.nombres(ctx("ROLE_SYSTEM_CONFIG")).contains(AsistenteHerramientas.RENDIMIENTO));
        assertFalse(herramientas.nombres(ctx("ROLE_DOCENTE")).contains(AsistenteHerramientas.RENDIMIENTO));
        assertFalse(herramientas.nombres(ctx("ROLE_PADRE")).contains(AsistenteHerramientas.RENDIMIENTO));
    }

    @Test
    void unDocenteNoPuedePedirElRendimientoAunqueElModeloLoIntente() {
        assertThrows(AsistenteHerramientas.ErrorHerramienta.class,
                () -> herramientas.ejecutar(AsistenteHerramientas.RENDIMIENTO, Map.of(), ctx("ROLE_DOCENTE")));
        verify(dashboardService, never()).rendimiento(any());
    }

    @Test
    void unPadreSoloVeASusHijos() {
        when(usuarioRepository.findEstudiantesByPadreId(PADRE)).thenReturn(List.of(hijo));
        when(usuarioRepository.findEstudianteActivoConNivel(hijo.getId(), DIRECCION)).thenReturn(Optional.of(hijo));
        when(dashboardService.resumenEstudiantes(eq(DIRECCION), anyList())).thenReturn(List.of());

        herramientas.ejecutar(AsistenteHerramientas.RESUMEN_ESTUDIANTE, Map.of("estudiante_id", 1), ctx("ROLE_PADRE"));
        AsistenteHerramientas.ErrorHerramienta error = assertThrows(AsistenteHerramientas.ErrorHerramienta.class,
                () -> herramientas.ejecutar(AsistenteHerramientas.RESUMEN_ESTUDIANTE,
                        Map.of("estudiante_id", ajeno.getId()), ctx("ROLE_PADRE")));
        assertTrue(error.getMessage().contains("no tiene acceso"));
        verify(dashboardService).resumenEstudiantes(DIRECCION, List.of(hijo));
    }

    @Test
    void elDocenteVeLosEstudiantesDeSuSeccionGuia() {
        when(docenteGuiaService.listarSecciones(DIRECCION, DOCENTE)).thenReturn(List.of(ochoDos));
        when(usuarioRepository.findEstudiantesActivosConNivel(DIRECCION, null, ochoDos.getId()))
                .thenReturn(List.of(hijo));

        String resultado = herramientas.ejecutar(AsistenteHerramientas.BUSCAR_ESTUDIANTES, Map.of(),
                ctx("ROLE_DOCENTE"));

        assertTrue(resultado.contains("José Ramírez"));
        assertFalse(resultado.contains("Ana Mora"));
        assertThrows(AsistenteHerramientas.ErrorHerramienta.class,
                () -> herramientas.ejecutar(AsistenteHerramientas.RESUMEN_ESTUDIANTE,
                        Map.of("estudiante_id", ajeno.getId()), ctx("ROLE_DOCENTE")));
    }

    @Test
    void laBusquedaIgnoraTildesYFiltraPorSeccion() {
        when(usuarioRepository.findEstudiantesActivosConNivel(DIRECCION, null, null)).thenReturn(List.of(hijo, ajeno));

        String porNombre = herramientas.ejecutar(AsistenteHerramientas.BUSCAR_ESTUDIANTES,
                Map.of("nombre", "jose"), ctx("ROLE_DIRECTOR"));
        String porSeccion = herramientas.ejecutar(AsistenteHerramientas.BUSCAR_ESTUDIANTES,
                Map.of("seccion", "9 - 1"), ctx("ROLE_DIRECTOR"));

        assertTrue(porNombre.contains("\"total\":1") && porNombre.contains("José Ramírez"));
        assertTrue(porSeccion.contains("\"total\":1") && porSeccion.contains("Ana Mora"));
        assertThrows(AsistenteHerramientas.ErrorHerramienta.class,
                () -> herramientas.ejecutar(AsistenteHerramientas.BUSCAR_ESTUDIANTES, Map.of("seccion", "12-9"),
                        ctx("ROLE_DIRECTOR")));
    }

    @Test
    void elDocenteConsultaSoloSusPropiasLecciones() {
        when(dashboardService.leccionesDelDia(eq(DIRECCION), eq("LUNES"), eq(DOCENTE), isNull())).thenReturn(List.of());

        herramientas.ejecutar(AsistenteHerramientas.LECCIONES, Map.of("dia", "lunes", "seccion", "9-1"),
                ctx("ROLE_DOCENTE"));

        verify(dashboardService).leccionesDelDia(DIRECCION, "LUNES", DOCENTE, null);
    }

    @Test
    void laAgendaRechazaRangosDeMasDeDosMeses() {
        AsistenteHerramientas.ErrorHerramienta error = assertThrows(AsistenteHerramientas.ErrorHerramienta.class,
                () -> herramientas.ejecutar(AsistenteHerramientas.AGENDA,
                        Map.of("desde", "2026-01-01", "hasta", "2026-06-01"), ctx("ROLE_PADRE")));
        assertTrue(error.getMessage().contains("62"));
    }

    @Test
    void unaHerramientaDesconocidaEsUnError() {
        assertThrows(AsistenteHerramientas.ErrorHerramienta.class,
                () -> herramientas.ejecutar("borrar_todo", Map.of(), ctx("ROLE_ADMIN")));
    }

    @Test
    void lasDefinicionesSonIgualesEntrePedidos() {
        assertEquals(herramientas.definiciones(ctx("ROLE_DIRECTOR")).toString(),
                herramientas.definiciones(ctx("ROLE_DIRECTOR")).toString());
    }

    private ContextoAsistente ctx(String rol) {
        Long usuarioId = "ROLE_PADRE".equals(rol) ? PADRE : DOCENTE;
        return new ContextoAsistente(DIRECCION, "Liceo de prueba", usuarioId, "Usuario", Set.of(rol));
    }

    private static NivelAcademico nivel(Long id, int grado, String seccion) {
        NivelAcademico nivel = new NivelAcademico();
        nivel.setId(id);
        nivel.setGrado(grado);
        nivel.setSeccion(seccion);
        nivel.setActivo(true);
        return nivel;
    }

    private static Usuario estudiante(Long id, String nombre, NivelAcademico nivel) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre(nombre);
        u.setNivelAcademico(nivel);
        return u;
    }
}
