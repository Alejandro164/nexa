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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;

import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.entity.OfertaEducativa;
import com.chavescr.nexa.service.SesionDireccionService.Estado;
import com.chavescr.nexa.service.SesionDireccionService.MenuCambioDireccion;
import com.chavescr.nexa.service.SesionDireccionService.Navegacion;
import com.chavescr.nexa.service.SesionDireccionService.OpcionDireccion;
import com.chavescr.nexa.service.SesionDireccionService.OpcionInstitucion;

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
        verify(direccionService, never()).listarActivasConInstitucion();
        verify(usuarioService, never()).listarDireccionesActivasDelUsuarioActual();
    }

    @Test
    void elAdminVeLasDemasDireccionesDeLaMismaInstitucion() {
        Institucion liceo = institucion(1L, "Liceo");
        Direccion primaria = direccion(4L, liceo, OfertaEducativa.PRIMARIA);
        Direccion secundaria = direccion(5L, liceo, OfertaEducativa.SECUNDARIA);
        when(direccionService.listarActivasConInstitucion()).thenReturn(List.of(secundaria, primaria));

        MenuCambioDireccion menu = service.menu(true, 4L);

        assertTrue(menu.isVisible());
        assertEquals(List.of("Primaria", "Secundaria"),
                menu.opciones().stream().map(OpcionDireccion::texto).toList());
        verify(usuarioService, never()).listarDireccionesActivasDelUsuarioActual();
        verify(direccionService).listarActivasConInstitucion();
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

    @Test
    void conUnaInstitucionElLoginAbreLaDireccionDeEntrada() {
        Institucion central = institucion(10L, "Escuela Central");
        Direccion primaria = direccion(1L, central, OfertaEducativa.PRIMARIA);
        primaria.setPrincipal(true);
        Direccion secundaria = direccion(2L, central, OfertaEducativa.SECUNDARIA);
        when(usuarioService.listarDireccionesActivasDelUsuarioActual()).thenReturn(List.of(secundaria, primaria));

        MockHttpSession session = new MockHttpSession();
        var resultado = service.resolver(session, false);

        assertEquals(Estado.RESUELTA, resultado.estado());
        assertEquals(1L, session.getAttribute("SESSION_DIRECCION_ID"));
        assertEquals("Escuela Central · Primaria", session.getAttribute("SESSION_DIRECCION_NOMBRE"));
    }

    @Test
    void conVariasInstitucionesElLoginPideElegirInstitucion() {
        when(usuarioService.listarDireccionesActivasDelUsuarioActual()).thenReturn(List.of(
                direccion(1L, institucion(10L, "Escuela Central"), OfertaEducativa.PRIMARIA),
                direccion(3L, institucion(20L, "Escuela Norte"), OfertaEducativa.PRIMARIA)));

        MockHttpSession session = new MockHttpSession();
        var resultado = service.resolver(session, false);

        assertEquals(Estado.REQUIERE_SELECCION, resultado.estado());
        assertNull(session.getAttribute("SESSION_DIRECCION_ID"));
    }

    @Test
    void laDireccionRecordadaSeAbreAunqueNoSeaLaDeEntrada() {
        Institucion central = institucion(10L, "Escuela Central");
        Direccion primaria = direccion(1L, central, OfertaEducativa.PRIMARIA);
        primaria.setPrincipal(true);
        Direccion secundaria = direccion(2L, central, OfertaEducativa.SECUNDARIA);
        when(usuarioService.listarDireccionesActivasDelUsuarioActual()).thenReturn(List.of(primaria, secundaria));
        when(usuarioService.obtenerUltimaDireccionIdDelUsuarioActual()).thenReturn(2L);

        MockHttpSession session = new MockHttpSession();
        service.resolver(session, false);

        assertEquals(2L, session.getAttribute("SESSION_DIRECCION_ID"));
    }

    @Test
    void elSelectorDeInstitucionesApuntaALaEntradaYMarcaLaActual() {
        Institucion central = institucion(10L, "Escuela Central");
        Direccion primaria = direccion(1L, central, OfertaEducativa.PRIMARIA);
        primaria.setPrincipal(true);
        Direccion secundaria = direccion(2L, central, OfertaEducativa.SECUNDARIA);
        Institucion norte = institucion(20L, "Escuela Norte");
        Direccion norteSecundaria = direccion(3L, norte, OfertaEducativa.SECUNDARIA);
        norteSecundaria.setPrincipal(true);
        when(usuarioService.listarDireccionesActivasDelUsuarioActual())
                .thenReturn(List.of(secundaria, primaria, norteSecundaria));

        Navegacion nav = service.navegacion(false, 2L);

        assertTrue(nav.puedeCambiarInstitucion());
        assertEquals(List.of("Escuela Central", "Escuela Norte"),
                nav.instituciones().stream().map(OpcionInstitucion::nombre).toList());
        OpcionInstitucion actual = nav.instituciones().get(0);
        assertTrue(actual.actual());
        assertEquals(1L, actual.direccionEntradaId());
        assertEquals("Primaria", actual.entrada());
        assertEquals(3L, nav.instituciones().get(1).direccionEntradaId());
        verify(usuarioService).listarDireccionesActivasDelUsuarioActual();
    }

    @Test
    void siNoTieneLaDireccionDeEntradaAbreLaQueSiTiene() {
        Institucion central = institucion(10L, "Escuela Central");
        Direccion secundaria = direccion(2L, central, OfertaEducativa.SECUNDARIA);
        when(usuarioService.listarDireccionesActivasDelUsuarioActual()).thenReturn(List.of(secundaria));

        Navegacion nav = service.navegacion(false, 2L);

        assertFalse(nav.puedeCambiarInstitucion());
        assertEquals(2L, nav.instituciones().get(0).direccionEntradaId());
        assertEquals("Secundaria", nav.instituciones().get(0).entrada());
    }

    @Test
    void navegacionLeeLasDireccionesUnaSolaVez() {
        Institucion central = institucion(10L, "Escuela Central");
        Direccion primaria = direccion(1L, central, OfertaEducativa.PRIMARIA);
        primaria.setPrincipal(true);
        Direccion secundaria = direccion(2L, central, OfertaEducativa.SECUNDARIA);
        Direccion otra = direccion(3L, institucion(20L, "Escuela Norte"), OfertaEducativa.PRIMARIA);
        when(usuarioService.listarDireccionesActivasDelUsuarioActual())
                .thenReturn(List.of(secundaria, primaria, otra));

        Navegacion nav = service.navegacion(false, 1L);

        verify(usuarioService).listarDireccionesActivasDelUsuarioActual();
        verify(direccionService, never()).listarActivasPorInstitucion(any());
        verify(direccionService, never()).listarActivasConInstitucion();
        assertTrue(nav.menu().isVisible());
        assertEquals(2, nav.instituciones().size());
        assertEquals(1L, nav.instituciones().get(0).direccionEntradaId());
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
