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

import com.chavescr.nexa.dto.InstitucionDTO;
import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.service.InstitucionService;
import com.chavescr.nexa.service.UsuarioService;

@ExtendWith(MockitoExtension.class)
class MainControllerTest {

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private InstitucionService institucionService;

    private MainController controller;

    @BeforeEach
    void setUp() {
        controller = new MainController();
        ReflectionTestUtils.setField(controller, "usuarioService", usuarioService);
        ReflectionTestUtils.setField(controller, "institucionService", institucionService);
    }

    @Test
    void noPermiteCambiarAUnaInstitucionALaQueElUsuarioNoPertenece() throws Exception {
        InstitucionDTO propia = new InstitucionDTO();
        propia.setId(1L);
        propia.setNombre("Institución Propia");
        when(usuarioService.obtenerInstitucionesDelUsuarioActual()).thenReturn(List.of(propia));

        MockHttpSession session = new MockHttpSession();

        controller.cambiarInstitucion(99L, new MockHttpServletRequest(), new MockHttpServletResponse(),
                session);

        assertNull(session.getAttribute("SESSION_INSTITUCION_ID"));
        assertNull(session.getAttribute("SESSION_INSTITUCION_NOMBRE"));
    }

    @Test
    void permiteCambiarAUnaInstitucionALaQueElUsuarioSiPertenece() throws Exception {
        InstitucionDTO propia = new InstitucionDTO();
        propia.setId(1L);
        propia.setNombre("Institución Propia");
        InstitucionDTO otraPropia = new InstitucionDTO();
        otraPropia.setId(2L);
        otraPropia.setNombre("Segunda Institución");
        when(usuarioService.obtenerInstitucionesDelUsuarioActual()).thenReturn(List.of(propia, otraPropia));

        MockHttpSession session = new MockHttpSession();

        controller.cambiarInstitucion(2L, new MockHttpServletRequest(), new MockHttpServletResponse(),
                session);

        assertEquals(2L, session.getAttribute("SESSION_INSTITUCION_ID"));
        assertEquals("Segunda Institución", session.getAttribute("SESSION_INSTITUCION_NOMBRE"));
    }

    @Test
    void adminPuedeCambiarACualquierInstitucionExistenteSinPertenecerAElla() throws Exception {
        Institucion otra = new Institucion();
        otra.setId(5L);
        otra.setNombre("Otra Institución");
        when(institucionService.findById(5L)).thenReturn(Optional.of(otra));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addUserRole("ROLE_ADMIN");
        MockHttpSession session = new MockHttpSession();

        controller.cambiarInstitucion(5L, request, new MockHttpServletResponse(), session);

        assertEquals(5L, session.getAttribute("SESSION_INSTITUCION_ID"));
        assertEquals("Otra Institución", session.getAttribute("SESSION_INSTITUCION_NOMBRE"));
    }
}
