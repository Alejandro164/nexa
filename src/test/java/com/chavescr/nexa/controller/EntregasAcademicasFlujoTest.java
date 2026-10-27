package com.chavescr.nexa.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.chavescr.nexa.entity.EntregaAcademica;
import com.chavescr.nexa.entity.EntregaAcademica.TipoEntrega;
import com.chavescr.nexa.entity.EntregaAcademicaDocente;
import com.chavescr.nexa.entity.EntregaAcademicaDocente.EstadoPresentacion;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.EntregaAcademicaDocenteRepository;
import com.chavescr.nexa.repository.UsuarioRepository;
import com.chavescr.nexa.service.EntregaAcademicaService;

import jakarta.persistence.EntityManager;

/**
 * Recorre las Entregas académicas de extremo a extremo contra la base de datos real: la dirección crea una
 * entrega, el docente la prepara y la presenta, la dirección la devuelve y luego la aprueba. Pasa por el
 * controlador y renderiza las plantillas. Todo corre en una transacción que se revierte al terminar.
 */
@SpringBootTest
@Transactional
class EntregasAcademicasFlujoTest {

    private static final Long DIRECCION = 2L;
    private static final Long ADMIN = 1L;

    @Autowired
    private EntregasAcademicasController controller;
    @Autowired
    private EntregaAcademicaService service;
    @Autowired
    private EntregaAcademicaDocenteRepository presentacionRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private SpringTemplateEngine templateEngine;
    @Autowired
    private EntityManager em;
    @Autowired
    private com.chavescr.nexa.service.AlmacenamientoService almacenamientoService;

    private Usuario docente;
    private MockHttpSession sesionDireccion;
    private MockHttpSession sesionDocente;
    private MockHttpServletRequest comoDireccion;
    private MockHttpServletRequest comoDocente;

    @BeforeEach
    void preparar() {
        assumeFalse(usuarioRepository.findById(ADMIN).isEmpty(), "La base de dev no tiene al administrador");
        List<Usuario> docentes = usuarioRepository.findActivosByDireccionIdAndRol(DIRECCION, "ROLE_DOCENTE");
        assumeFalse(docentes.isEmpty(), "La dirección 2 no tiene docentes");
        docente = docentes.get(0);

        sesionDireccion = sesion(ADMIN);
        sesionDocente = sesion(docente.getId());
        comoDireccion = new MockHttpServletRequest();
        comoDireccion.addUserRole("ROLE_ADMIN");
        comoDocente = new MockHttpServletRequest();
        comoDocente.addUserRole("ROLE_DOCENTE");
    }

    private MockHttpSession sesion(Long usuarioId) {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("SESSION_DIRECCION_ID", DIRECCION);
        s.setAttribute("SESSION_USUARIO_ID", usuarioId);
        return s;
    }

    /** Renderiza la vista que devolvió el controlador, con el modelo que armó. */
    private String render(String vista, ExtendedModelMap model) {
        em.flush();
        Context context = new Context();
        context.setVariables(model);
        int separador = vista.indexOf(" :: ");
        return templateEngine.process(new TemplateSpec(vista.substring(0, separador),
                Set.of(vista.substring(separador + 4)), (org.thymeleaf.templatemode.TemplateMode) null, null), context);
    }

    private EntregaAcademicaDocente respuesta(Long entregaId) {
        em.flush();
        em.clear();
        return presentacionRepository.findByEntregaIdAndDocenteId(entregaId, docente.getId()).orElseThrow();
    }

    @Test
    void lasEntregasAcademicasFuncionanDeExtremoAExtremo() {
        LocalDate hoy = LocalDate.now();

        // ── Dirección: un error de validación vuelve al formulario con el mensaje ──
        ExtendedModelMap model = new ExtendedModelMap();
        MockHttpServletResponse respuestaHttp = new MockHttpServletResponse();
        String html = render(controller.guardar(null, "Mal plazo", null, TipoEntrega.INFORME, null, hoy,
                hoy.minusDays(1), "on", model, sesionDireccion, comoDireccion, respuestaHttp), model);
        assertTrue(html.contains("La fecha de fin no puede ser anterior"));
        assertEquals("#entregas-modal-container", respuestaHttp.getHeader("HX-Retarget"));

        // ── Dirección: crea la entrega ──
        model = new ExtendedModelMap();
        html = render(controller.guardar(null, "Planeamiento del II trimestre", "Incluye objetivos y rúbricas.",
                TipoEntrega.PLANEAMIENTO, null, hoy.minusDays(1), hoy.plusDays(14), "on", model, sesionDireccion,
                comoDireccion, new MockHttpServletResponse()), model);
        assertTrue(html.contains("Planeamiento del II trimestre"));
        assertTrue(html.contains("En plazo"));
        EntregaAcademica entrega = service.listarEntregas(DIRECCION).stream()
                .filter(e -> e.getNombre().equals("Planeamiento del II trimestre")).findFirst().orElseThrow();
        Long entregaId = entrega.getId();

        // ── Docente: la ve como pendiente y sin iniciar ──
        model = new ExtendedModelMap();
        html = render(controller.pendientes(null, null, null, 1, model, sesionDocente, comoDocente), model);
        assertTrue(html.contains("Planeamiento del II trimestre"));
        assertTrue(html.contains("Sin iniciar"));
        assertTrue(html.contains(">Iniciar<"));
        assertFalse(html.contains("Docente</th>"), "El docente no ve la columna de docente");

        // ── Docente: abre el detalle, guarda borrador, adjunta un enlace y un archivo ──
        model = new ExtendedModelMap();
        html = render(controller.detalle(entregaId, null, model, sesionDocente, comoDocente), model);
        assertTrue(html.contains("Incluye objetivos y rúbricas."));
        assertTrue(html.contains("Guardar borrador"));

        model = new ExtendedModelMap();
        render(controller.guardarBorrador(entregaId, "Avance 50%", model, sesionDocente, comoDocente), model);
        assertEquals(EstadoPresentacion.BORRADOR, respuesta(entregaId).getEstado());

        MockMultipartFile archivo = new MockMultipartFile("archivos", "planeamiento.pdf", "application/pdf",
                "contenido de prueba".getBytes());
        model = new ExtendedModelMap();
        html = render(controller.agregarAdjuntos(entregaId, List.of(archivo), "www.mep.go.cr/circular",
                "Avance 80%", model, sesionDocente, comoDocente), model);
        assertFalse(html.contains("ed-error"), html);
        EntregaAcademicaDocente borrador = respuesta(entregaId);
        assertEquals("Avance 80%", borrador.getObservacion());
        assertEquals(2, borrador.getAdjuntos().size());
        var guardado = borrador.getAdjuntos().stream().filter(a -> !a.isEnlace()).findFirst().orElseThrow();
        assertTrue(java.nio.file.Files.isReadable(almacenamientoService.resolver(guardado.getRuta())));

        // En Pendientes pasa a "Borrador / Continuar"
        model = new ExtendedModelMap();
        html = render(controller.pendientes(null, null, null, 1, model, sesionDocente, comoDocente), model);
        assertTrue(html.contains("Borrador"));
        assertTrue(html.contains(">Continuar<"));

        // ── Docente: presenta ──
        model = new ExtendedModelMap();
        html = render(controller.presentar(entregaId, "Listo para revisión", model, sesionDocente, comoDocente), model);
        assertTrue(html.contains("pendiente de revisión"));
        assertEquals(EstadoPresentacion.PRESENTADA, respuesta(entregaId).getEstado());

        // Ya no es pendiente: aparece en Realizadas
        model = new ExtendedModelMap();
        html = render(controller.pendientes(null, null, null, 1, model, sesionDocente, comoDocente), model);
        // (el nombre sigue en la lista desplegable de filtros; lo que cambia es que la tabla queda vacía)
        assertTrue(html.contains("No tienes entregas pendientes."));
        model = new ExtendedModelMap();
        html = render(controller.realizadas(null, null, null, 1, model, sesionDocente, comoDocente), model);
        assertTrue(html.contains("Planeamiento del II trimestre"));
        assertTrue(html.contains("Presentada"));

        // Un presentado ya no se puede modificar: el error se ve en el modal
        model = new ExtendedModelMap();
        html = render(controller.guardarBorrador(entregaId, "cambio", model, sesionDocente, comoDocente), model);
        assertTrue(html.contains("ya fue presentada"));

        // ── Dirección: ve al docente, abre la revisión y devuelve la entrega ──
        model = new ExtendedModelMap();
        html = render(controller.realizadas(null, null, null, 1, model, sesionDireccion, comoDireccion), model);
        assertTrue(html.contains(docente.getNombre()));
        assertTrue(html.contains(">Revisar<"));

        Long presentacionId = respuesta(entregaId).getId();
        model = new ExtendedModelMap();
        html = render(controller.detalle(entregaId, docente.getId(), model, sesionDireccion, comoDireccion), model);
        assertTrue(html.contains("Aprobar"));
        assertTrue(html.contains("Devolver con observaciones"));

        model = new ExtendedModelMap();
        html = render(controller.revisar(entregaId, presentacionId, false, " ", docente.getId(), model,
                sesionDireccion, comoDireccion), model);
        assertTrue(html.contains("Indica qué debe corregir"));

        model = new ExtendedModelMap();
        render(controller.revisar(entregaId, presentacionId, false, "Falta la rúbrica", docente.getId(), model,
                sesionDireccion, comoDireccion), model);
        assertEquals(EstadoPresentacion.DEVUELTA, respuesta(entregaId).getEstado());

        // ── Docente: vuelve a Pendientes como devuelta, corrige y presenta otra vez ──
        model = new ExtendedModelMap();
        html = render(controller.pendientes(null, null, null, 1, model, sesionDocente, comoDocente), model);
        assertTrue(html.contains("Devuelta"));
        assertTrue(html.contains(">Corregir<"));
        model = new ExtendedModelMap();
        html = render(controller.detalle(entregaId, null, model, sesionDocente, comoDocente), model);
        assertTrue(html.contains("Falta la rúbrica"));

        model = new ExtendedModelMap();
        render(controller.presentar(entregaId, "Rúbrica agregada", model, sesionDocente, comoDocente), model);
        assertEquals(EstadoPresentacion.PRESENTADA, respuesta(entregaId).getEstado());

        // ── Dirección: aprueba ──
        model = new ExtendedModelMap();
        render(controller.revisar(entregaId, presentacionId, true, null, docente.getId(), model,
                sesionDireccion, comoDireccion), model);
        assertEquals(EstadoPresentacion.APROBADA, respuesta(entregaId).getEstado());
        model = new ExtendedModelMap();
        html = render(controller.realizadas(null, null, null, 1, model, sesionDocente, comoDocente), model);
        assertTrue(html.contains("Aprobada"));

        // ── Seguimiento y borrado ──
        model = new ExtendedModelMap();
        html = render(controller.seguimiento(entregaId, model, sesionDireccion, comoDireccion), model);
        assertTrue(html.contains(docente.getNombre()));
        assertTrue(html.contains("Aprobada"));

        model = new ExtendedModelMap();
        html = render(controller.eliminar(entregaId, model, sesionDireccion, comoDireccion), model);
        assertTrue(html.contains("ya tiene respuestas de docentes"));
        assertTrue(html.contains("Planeamiento del II trimestre"));
    }

    @Test
    void elMantenimientoYLaRevisionSonSoloParaLaDireccion() {
        ExtendedModelMap model = new ExtendedModelMap();
        assertThrows(AccessDeniedException.class,
                () -> controller.mantenimiento(model, sesionDocente, comoDocente));
        assertThrows(AccessDeniedException.class,
                () -> controller.formCrear(model, sesionDocente, comoDocente));
        assertThrows(AccessDeniedException.class,
                () -> controller.revisar(1L, 1L, true, null, null, model, sesionDocente, comoDocente));

        // Y quien supervisa no presenta entregas: la acción se rechaza
        EntregaAcademica abierta = service.guardarEntrega(DIRECCION, ADMIN, null, "Entrega de prueba", null,
                TipoEntrega.OTRA, null, LocalDate.now().minusDays(1), LocalDate.now().plusDays(5), true);
        ExtendedModelMap supervisor = new ExtendedModelMap();
        assertThrows(AccessDeniedException.class,
                () -> controller.guardarBorrador(abierta.getId(), "x", supervisor, sesionDireccion, comoDireccion));
    }
}
