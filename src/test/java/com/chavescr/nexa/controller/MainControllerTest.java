package com.chavescr.nexa.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import com.chavescr.nexa.dto.DireccionDTO;
import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.service.DireccionService;
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
    }

    @Test
    void noPermiteCambiarAUnaDireccionALaQueElUsuarioNoPertenece() throws Exception {
        DireccionDTO propia = new DireccionDTO();
        propia.setId(1L);
        propia.setNombre("Dirección Propia");
        when(usuarioService.obtenerDireccionesDelUsuarioActual()).thenReturn(List.of(propia));

        MockHttpSession session = new MockHttpSession();

        controller.cambiarDireccion(99L, new MockHttpServletRequest(), new MockHttpServletResponse(),
                session);

        assertNull(session.getAttribute("SESSION_DIRECCION_ID"));
        assertNull(session.getAttribute("SESSION_DIRECCION_NOMBRE"));
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
                session);

        assertEquals(2L, session.getAttribute("SESSION_DIRECCION_ID"));
        assertEquals("Segunda Dirección", session.getAttribute("SESSION_DIRECCION_NOMBRE"));
    }

    @Test
    void adminPuedeCambiarACualquierDireccionExistenteSinPertenecerAElla() throws Exception {
        Direccion otra = new Direccion();
        otra.setId(5L);
        otra.setNombre("Otra Dirección");
        when(direccionService.findById(5L)).thenReturn(Optional.of(otra));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addUserRole("ROLE_ADMIN");
        MockHttpSession session = new MockHttpSession();

        controller.cambiarDireccion(5L, request, new MockHttpServletResponse(), session);

        assertEquals(5L, session.getAttribute("SESSION_DIRECCION_ID"));
        assertEquals("Otra Dirección", session.getAttribute("SESSION_DIRECCION_NOMBRE"));
    }
}
