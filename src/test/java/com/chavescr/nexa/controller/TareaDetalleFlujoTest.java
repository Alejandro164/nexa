package com.chavescr.nexa.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.chavescr.nexa.entity.ActividadTarea;
import com.chavescr.nexa.entity.EtiquetaTarea;
import com.chavescr.nexa.entity.TareaPlanTrabajo;
import com.chavescr.nexa.entity.TareaPlanTrabajo.EstadoTarea;
import com.chavescr.nexa.entity.TareaPlanTrabajo.RecordatorioTarea;
import com.chavescr.nexa.repository.TareaPlanTrabajoRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

import jakarta.persistence.EntityManager;

/**
 * Recorre el detalle de una tarea (estado, fechas, etiquetas, checklist, miembros, adjuntos y
 * comentarios) contra la base de datos real, pasando por el controlador y renderizando la plantilla.
 * Todo corre en una transacción que se revierte al terminar, así que no deja datos.
 */
@SpringBootTest
@Transactional
class TareaDetalleFlujoTest {

    @Autowired
    private TareaController controller;
    @Autowired
    private TareaPlanTrabajoRepository tareaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private SpringTemplateEngine templateEngine;
    @Autowired
    private EntityManager em;
    @Autowired
    private com.chavescr.nexa.service.AlmacenamientoService almacenamientoService;

    private Long direccionId;
    private Long tareaId;
    private Long usuarioId;
    private MockHttpSession session;
    private MockHttpServletRequest request;

    @BeforeEach
    void preparar() {
        TareaPlanTrabajo tarea = tareaRepository.findAll().stream().findFirst().orElse(null);
        assumeFalse(tarea == null, "La base de dev no tiene tareas de plan de trabajo");
        tareaId = tarea.getId();
        direccionId = tarea.getPlanTrabajo().getDireccion().getId();
        usuarioId = usuarioRepository.findActivosByDireccionId(direccionId).get(0).getId();

        session = new MockHttpSession();
        session.setAttribute("SESSION_DIRECCION_ID", direccionId);
        session.setAttribute("SESSION_USUARIO_ID", usuarioId);
        request = new MockHttpServletRequest();
    }

    /** Ejecuta la acción del controlador y devuelve el HTML del cuerpo ya renderizado. */
    private String render(String vista, ExtendedModelMap model) {
        // Como en la app (open-in-view), la sesión sigue abierta mientras se renderiza la vista
        em.flush();
        assertTrue(vista.startsWith("agenda/tareas/detalle :: "), vista);
        Context context = new Context();
        context.setVariables(model);
        String fragmento = vista.substring(vista.indexOf(":: ") + 3);
        return templateEngine.process(new TemplateSpec("agenda/tareas/detalle", Set.of(fragmento),
                (org.thymeleaf.templatemode.TemplateMode) null, null), context);
    }

    private TareaPlanTrabajo recargar() {
        em.flush();
        em.clear();
        return tareaRepository.findById(tareaId).orElseThrow();
    }

    @Test
    void elDetalleDeUnaTareaFuncionaDeExtremoAExtremo() {
        // Abrir el detalle: se crea el catálogo base de etiquetas de la dirección
        ExtendedModelMap model = new ExtendedModelMap();
        String html = render(controller.detalle(tareaId, model, session, request), model);
        assertTrue(html.contains("td-grid"));
        assertTrue(html.contains("Backlog / Entrada") || html.contains("Trabajo activo"));
        @SuppressWarnings("unchecked")
        List<EtiquetaTarea> catalogo = (List<EtiquetaTarea>) model.get("etiquetasDisponibles");
        assertTrue(catalogo.size() >= 5);

        // Estado
        model = new ExtendedModelMap();
        html = render(controller.estado(tareaId, EstadoTarea.EN_CORRECCION, model, session, request), model);
        assertTrue(html.contains("actual en-correccion"));
        assertEquals(EstadoTarea.EN_CORRECCION, recargar().getEstado());

        model = new ExtendedModelMap();
        render(controller.estado(tareaId, EstadoTarea.FINALIZADO, model, session, request), model);
        assertTrue(recargar().getFechaCompletada() != null);

        // Fechas y recordatorio
        LocalDate limite = LocalDate.now().plusDays(30);
        model = new ExtendedModelMap();
        html = render(controller.fechas(tareaId, LocalDate.now(), limite, RecordatorioTarea.DOS_DIAS_ANTES,
                model, session, request), model);
        assertTrue(html.contains("2 días antes"));
        TareaPlanTrabajo tarea = recargar();
        assertEquals(limite, tarea.getFechaLimite());
        assertEquals(RecordatorioTarea.DOS_DIAS_ANTES, tarea.getRecordatorio());

        // Fechas inválidas: el error se muestra en el mismo modal y no se guarda nada
        model = new ExtendedModelMap();
        html = render(controller.fechas(tareaId, limite.plusDays(1), limite, RecordatorioTarea.NINGUNO,
                model, session, request), model);
        assertTrue(html.contains("td-error"));
        assertEquals(RecordatorioTarea.DOS_DIAS_ANTES, recargar().getRecordatorio());

        // Etiquetas: asignar una del catálogo y crear una nueva
        Long etiquetaId = catalogo.get(0).getId();
        model = new ExtendedModelMap();
        render(controller.alternarEtiqueta(tareaId, etiquetaId, model, session, request), model);
        assertEquals(1, recargar().getEtiquetas().size());
        model = new ExtendedModelMap();
        html = render(controller.crearEtiqueta(tareaId, "Etiqueta de prueba", "#0079bf", model, session, request), model);
        assertTrue(html.contains("Etiqueta de prueba"));
        assertEquals(2, recargar().getEtiquetas().size());

        // Eliminar una etiqueta del catálogo la quita también de la tarea
        model = new ExtendedModelMap();
        render(controller.eliminarEtiqueta(tareaId, etiquetaId, model, session, request), model);
        assertEquals(1, recargar().getEtiquetas().size());

        // Checklist
        model = new ExtendedModelMap();
        render(controller.agregarItem(tareaId, "Revisar objetivos", model, session, request), model);
        model = new ExtendedModelMap();
        html = render(controller.agregarItem(tareaId, "Enviar a dirección", model, session, request), model);
        assertTrue(html.contains("Revisar objetivos") && html.contains("Enviar a dirección"));
        Long itemId = recargar().getChecklist().get(0).getId();
        model = new ExtendedModelMap();
        html = render(controller.alternarItem(tareaId, itemId, model, session, request), model);
        assertTrue(html.contains("1/2"));
        assertEquals(50, recargar().getChecklistPorcentaje());

        // Miembros: un colaborador distinto al responsable
        Long responsableId = recargar().getMiembro() != null ? recargar().getMiembro().getUsuario().getId() : null;
        Long colaboradorId = usuarioRepository.findActivosByDireccionId(direccionId).stream()
                .map(u -> u.getId()).filter(id -> !id.equals(responsableId)).findFirst().orElseThrow();
        model = new ExtendedModelMap();
        render(controller.agregarColaborador(tareaId, colaboradorId, model, session, request), model);
        assertEquals(1, recargar().getColaboradores().size());
        model = new ExtendedModelMap();
        render(controller.quitarColaborador(tareaId, colaboradorId, model, session, request), model);
        assertTrue(recargar().getColaboradores().isEmpty());

        // Adjuntos: enlace externo
        model = new ExtendedModelMap();
        html = render(controller.agregarAdjuntos(tareaId, null, "www.mep.go.cr/circular", model, session, request), model);
        assertTrue(html.contains("https://www.mep.go.cr/circular"));
        assertEquals(1, recargar().getAdjuntos().size());

        // Adjuntos: archivo en disco (al revertirse la transacción de la prueba el archivo se borra)
        org.springframework.mock.web.MockMultipartFile archivo = new org.springframework.mock.web.MockMultipartFile(
                "archivos", "planeamiento.pdf", "application/pdf", "contenido de prueba".getBytes());
        model = new ExtendedModelMap();
        html = render(controller.agregarAdjuntos(tareaId, List.of(archivo), null, model, session, request), model);
        assertFalse(html.contains("td-error"), html);
        assertTrue(html.contains("planeamiento.pdf"));
        List<com.chavescr.nexa.entity.AdjuntoTarea> adjuntos = recargar().getAdjuntos();
        assertEquals(2, adjuntos.size());
        com.chavescr.nexa.entity.AdjuntoTarea guardado = adjuntos.stream().filter(a -> !a.isEnlace()).findFirst().orElseThrow();
        assertEquals("PDF", guardado.getExtension());
        assertTrue(java.nio.file.Files.isReadable(almacenamientoService.resolver(guardado.getRuta())));

        // Comentarios
        model = new ExtendedModelMap();
        html = render(controller.comentar(tareaId, "Falta anexar la rúbrica", model, session, request), model);
        assertTrue(html.contains("Falta anexar la rúbrica"));
        ActividadTarea comentario = recargar().getActividad().stream().filter(ActividadTarea::isComentario).findFirst()
                .orElseThrow();
        model = new ExtendedModelMap();
        render(controller.eliminarComentario(tareaId, comentario.getId(), model, session, request), model);
        assertFalse(recargar().getActividad().stream().anyMatch(ActividadTarea::isComentario));

        // Cada cambio quedó en la actividad
        assertTrue(recargar().getActividad().size() >= 8);
    }

    @Test
    void unaTareaDeOtraInstitucionNoSeAbre() {
        session.setAttribute("SESSION_DIRECCION_ID", direccionId + 1000);
        ExtendedModelMap model = new ExtendedModelMap();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> controller.detalle(tareaId, model, session, request));
        assertNull(model.get("error"));
    }
}
