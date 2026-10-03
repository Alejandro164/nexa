package com.chavescr.nexa.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;

import com.chavescr.nexa.dto.DireccionDTO;
import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.service.DireccionService;
import com.chavescr.nexa.service.SesionDireccionService;
import com.chavescr.nexa.service.UsuarioService;

@ExtendWith(MockitoExtension.class)
class MainControllerTest {

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private DireccionService direccionService;

    private MainController controller;

    @BeforeEach
    void setUp() {
        controller = new MainController();
        ReflectionTestUtils.setField(controller, "usuarioService", usuarioService);
        ReflectionTestUtils.setField(controller, "direccionService", direccionService);
        ReflectionTestUtils.setField(controller, "sesionDireccionService",
                new SesionDireccionService(usuarioService, direccionService));
    }

    @Test
    void quienNoEsAdminNiDirectorNoCambiaLaDireccionActiva() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("SESSION_DIRECCION_ID", 1L);
        session.setAttribute("SESSION_DIRECCION_NOMBRE", "Dirección Propia");
        session.setAttribute("SESSION_USUARIO_ID", 7L);

        controller.cambiarDireccion(99L, new MockHttpServletRequest(), new MockHttpServletResponse(),
                session, new ExtendedModelMap());

        assertEquals(1L, session.getAttribute("SESSION_DIRECCION_ID"));
        assertEquals("Dirección Propia", session.getAttribute("SESSION_DIRECCION_NOMBRE"));
        verify(usuarioService, never()).actualizarUltimaDireccion(any(), any());
    }

    @Test
    void permiteCambiarAUnaDireccionALaQueElUsuarioSiPertenece() throws Exception {
        DireccionDTO propia = new DireccionDTO();
        propia.setId(1L);
        propia.setNombre("Dirección Propia");
        DireccionDTO otraPropia = new DireccionDTO();
        otraPropia.setId(2L);
        otraPropia.setNombre("Segunda Dirección");
        when(usuarioService.obtenerDireccionesDelUsuarioActual()).thenReturn(List.of(propia, otraPropia));

        MockHttpSession session = new MockHttpSession();

        controller.cambiarDireccion(2L, new MockHttpServletRequest(), new MockHttpServletResponse(),
                session, new ExtendedModelMap());

        assertEquals(2L, session.getAttribute("SESSION_DIRECCION_ID"));
        assertEquals("Segunda Dirección", session.getAttribute("SESSION_DIRECCION_NOMBRE"));
        verify(usuarioService, never()).actualizarUltimaDireccion(any(), any());
    }

    @Test
    void systemConfigPuedeCambiarACualquierDireccionExistenteSinPertenecerAElla() throws Exception {
        Direccion otra = new Direccion();
        otra.setId(5L);
        otra.setNombre("Otra Dirección");
        when(direccionService.findByIdConInstitucion(5L)).thenReturn(Optional.of(otra));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addUserRole("ROLE_SYSTEM_CONFIG");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("SESSION_USUARIO_ID", 7L);

        controller.cambiarDireccion(5L, request, new MockHttpServletResponse(), session, new ExtendedModelMap());

        verify(usuarioService).actualizarUltimaDireccion(7L, otra);

        assertEquals(5L, session.getAttribute("SESSION_DIRECCION_ID"));
        assertEquals("Otra Dirección", session.getAttribute("SESSION_DIRECCION_NOMBRE"));
    }

    @Test
    void directorPuedeCambiarDeDireccion() throws Exception {
        DireccionDTO actual = new DireccionDTO();
        actual.setId(1L);
        actual.setNombre("Primaria");
        DireccionDTO otra = new DireccionDTO();
        otra.setId(2L);
        otra.setNombre("Secundaria");
        when(usuarioService.obtenerDireccionesDelUsuarioActual()).thenReturn(List.of(actual, otra));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addUserRole("ROLE_DIRECTOR");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("SESSION_DIRECCION_ID", 1L);
        session.setAttribute("SESSION_DIRECCION_NOMBRE", "Primaria");

        controller.cambiarDireccion(2L, request, new MockHttpServletResponse(), session, new ExtendedModelMap());

        assertEquals(2L, session.getAttribute("SESSION_DIRECCION_ID"));
        assertEquals("Secundaria", session.getAttribute("SESSION_DIRECCION_NOMBRE"));
    }

    @Test
    void docenteNoEntraAUnaDireccionQueNoEsSuya() throws Exception {
        DireccionDTO propia = new DireccionDTO();
        propia.setId(1L);
        propia.setNombre("Primaria");
        when(usuarioService.obtenerDireccionesDelUsuarioActual()).thenReturn(List.of(propia));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addUserRole("ROLE_DOCENTE");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("SESSION_DIRECCION_ID", 1L);
        session.setAttribute("SESSION_DIRECCION_NOMBRE", "Primaria");

        controller.cambiarDireccion(2L, request, response, session, new ExtendedModelMap());

        assertEquals(403, response.getStatus());
        assertEquals(1L, session.getAttribute("SESSION_DIRECCION_ID"));
        assertEquals("Primaria", session.getAttribute("SESSION_DIRECCION_NOMBRE"));
        verify(usuarioService, never()).actualizarUltimaDireccion(any(), any());
    }
}
