package com.chavescr.nexa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;

import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.entity.OfertaEducativa;
import com.chavescr.nexa.service.SesionDireccionService.MenuCambioDireccion;
import com.chavescr.nexa.service.SesionDireccionService.OpcionDireccion;

@ExtendWith(MockitoExtension.class)
class SesionDireccionServiceTest {

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private DireccionService direccionService;

    private SesionDireccionService service;

    @BeforeEach
    void setUp() {
        service = new SesionDireccionService(usuarioService, direccionService);
    }

    @Test
    void elMenuSoloListaLasDireccionesDeLaInstitucionActiva() {
        Institucion central = institucion(10L, "Escuela Central");
        Institucion norte = institucion(20L, "Escuela Norte");
        Direccion secundaria = direccion(2L, central, OfertaEducativa.SECUNDARIA);
        Direccion primaria = direccion(1L, central, OfertaEducativa.PRIMARIA);
        Direccion otra = direccion(3L, norte, OfertaEducativa.PRIMARIA);
        when(usuarioService.listarDireccionesActivasDelUsuarioActual())
                .thenReturn(List.of(secundaria, primaria, otra));

        MenuCambioDireccion menu = service.menu(false, 1L);

        assertTrue(menu.isVisible());
        assertEquals(List.of("Primaria", "Secundaria"),
                menu.opciones().stream().map(OpcionDireccion::texto).toList());
        assertTrue(menu.opciones().get(0).actual());
        assertFalse(menu.opciones().stream().anyMatch(op -> op.id().equals(3L)));
    }

    @Test
    void conUnaSolaDireccionElMenuNoSeMuestra() {
        Institucion central = institucion(10L, "Escuela Central");
        Direccion primaria = direccion(1L, central, OfertaEducativa.PRIMARIA);
        Direccion otra = direccion(3L, institucion(20L, "Escuela Norte"), OfertaEducativa.PRIMARIA);
        when(usuarioService.listarDireccionesActivasDelUsuarioActual()).thenReturn(List.of(primaria, otra));

        MenuCambioDireccion menu = service.menu(false, 1L);

        assertFalse(menu.isVisible());
        assertTrue(menu.opciones().isEmpty());
    }

    @Test
    void sinDireccionActivaElMenuNoSeMuestra() {
        MenuCambioDireccion menu = service.menu(true, null);

        assertFalse(menu.isVisible());
        verify(direccionService, never()).listarActivasPorInstitucion(any());
        verify(usuarioService, never()).listarDireccionesActivasDelUsuarioActual();
    }

    @Test
    void elAdminVeLasDemasDireccionesDeLaMismaInstitucion() {
        Institucion liceo = institucion(1L, "Liceo");
        Direccion primaria = direccion(4L, liceo, OfertaEducativa.PRIMARIA);
        Direccion secundaria = direccion(5L, liceo, OfertaEducativa.SECUNDARIA);
        when(direccionService.findByIdConInstitucion(4L)).thenReturn(Optional.of(primaria));
        when(direccionService.listarActivasPorInstitucion(1L)).thenReturn(List.of(secundaria, primaria));

        MenuCambioDireccion menu = service.menu(true, 4L);

        assertTrue(menu.isVisible());
        assertEquals(List.of("Primaria", "Secundaria"),
                menu.opciones().stream().map(OpcionDireccion::texto).toList());
        verify(usuarioService, never()).listarDireccionesActivasDelUsuarioActual();
    }

    @Test
    void unaDireccionNulaNoBorraLaDireccionQueYaEstaEnSesion() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("SESSION_DIRECCION_ID", 1L);
        session.setAttribute("SESSION_DIRECCION_NOMBRE", "Primaria");

        assertFalse(service.cambiar(session, 7L, null, false));

        assertEquals(1L, session.getAttribute("SESSION_DIRECCION_ID"));
        assertEquals("Primaria", session.getAttribute("SESSION_DIRECCION_NOMBRE"));
        verify(usuarioService, never()).actualizarUltimaDireccion(any(), any());
    }

    @Test
    void salirQuitaLaDireccionYOlvidaLaRecordada() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("SESSION_DIRECCION_ID", 1L);
        session.setAttribute("SESSION_DIRECCION_NOMBRE", "Primaria");

        service.salir(session, 7L);

        assertNull(session.getAttribute("SESSION_DIRECCION_ID"));
        assertNull(session.getAttribute("SESSION_DIRECCION_NOMBRE"));
        verify(usuarioService).actualizarUltimaDireccion(7L, null);
    }

    private static Institucion institucion(Long id, String nombre) {
        Institucion institucion = new Institucion();
        institucion.setId(id);
        institucion.setNombre(nombre);
        return institucion;
    }

    private static Direccion direccion(Long id, Institucion institucion, OfertaEducativa oferta) {
        Direccion direccion = new Direccion();
        direccion.setId(id);
        direccion.setNombre(oferta.getEtiqueta());
        direccion.setInstitucion(institucion);
        direccion.setOferta(oferta);
        direccion.setActiva(true);
        return direccion;
    }
}
