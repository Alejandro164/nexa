package com.chavescr.nexa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import com.chavescr.nexa.entity.ActividadTarea;
import com.chavescr.nexa.entity.ActividadTarea.TipoActividad;
import com.chavescr.nexa.entity.ChecklistItemTarea;
import com.chavescr.nexa.entity.EtiquetaTarea;
import com.chavescr.nexa.entity.MiembroPlanTrabajo;
import com.chavescr.nexa.entity.PlanTrabajo;
import com.chavescr.nexa.entity.TareaPlanTrabajo;
import com.chavescr.nexa.entity.TareaPlanTrabajo.EstadoTarea;
import com.chavescr.nexa.entity.TareaPlanTrabajo.RecordatorioTarea;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.AdjuntoTareaRepository;
import com.chavescr.nexa.repository.DireccionRepository;
import com.chavescr.nexa.repository.EtiquetaTareaRepository;
import com.chavescr.nexa.repository.TareaPlanTrabajoRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class TareaDetalleServiceTest {

    private static final Long DIRECCION = 1L;
    private static final Long TAREA = 10L;

    @Mock
    private TareaPlanTrabajoRepository tareaRepository;
    @Mock
    private EtiquetaTareaRepository etiquetaRepository;
    @Mock
    private AdjuntoTareaRepository adjuntoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private DireccionRepository direccionRepository;
    @Mock
    private AlmacenamientoService almacenamientoService;
    @Mock
    private NotificacionService notificacionService;

    private TareaDetalleService service;
    private Usuario director;
    private Usuario docente;
    private TareaPlanTrabajo tarea;

    @BeforeEach
    void setUp() {
        service = new TareaDetalleService(tareaRepository, etiquetaRepository, adjuntoRepository,
                usuarioRepository, direccionRepository, almacenamientoService, notificacionService);

        director = usuario(1L, "Alejandro Chaves");
        docente = usuario(2L, "María Soto");

        PlanTrabajo plan = new PlanTrabajo();
        plan.setId(5L);
        plan.setNombre("Feria de Ciencias");
        MiembroPlanTrabajo miembro = new MiembroPlanTrabajo();
        miembro.setId(7L);
        miembro.setUsuario(docente);

        tarea = new TareaPlanTrabajo();
        ReflectionTestUtils.setField(tarea, "id", TAREA);
        tarea.setTitulo("Redactar el planeamiento");
        tarea.setFechaLimite(LocalDate.now().plusDays(5));
        tarea.setPlanTrabajo(plan);
        tarea.setMiembro(miembro);

        lenient().when(tareaRepository.findByIdAndPlanTrabajo_Direccion_Id(TAREA, DIRECCION)).thenReturn(Optional.of(tarea));
        lenient().when(usuarioRepository.findById(1L)).thenReturn(Optional.of(director));
    }

    private Usuario usuario(Long id, String nombre) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre(nombre);
        return u;
    }

    @Test
    void cambiarEstadoRegistraActividadYMarcaLaFechaDeCierre() {
        service.cambiarEstado(DIRECCION, 1L, TAREA, EstadoTarea.EN_REVISION);
        assertEquals(EstadoTarea.EN_REVISION, tarea.getEstado());
        assertNull(tarea.getFechaCompletada());
        assertEquals("cambió el estado de «Pendiente» a «En revisión»", tarea.getActividad().get(0).getTexto());

        service.cambiarEstado(DIRECCION, 1L, TAREA, EstadoTarea.FINALIZADO);
        assertNotNull(tarea.getFechaCompletada());

        // Reabrir la tarea borra la fecha de cierre
        service.cambiarEstado(DIRECCION, 1L, TAREA, EstadoTarea.EN_CORRECCION);
        assertNull(tarea.getFechaCompletada());
        assertEquals(3, tarea.getActividad().size());

        // Mismo estado: no hay cambio ni actividad nueva
        service.cambiarEstado(DIRECCION, 1L, TAREA, EstadoTarea.EN_CORRECCION);
        assertEquals(3, tarea.getActividad().size());
    }

    @Test
    void laTareaVencidaSeCalculaPorLaFechaYNoEsUnEstado() {
        tarea.setFechaLimite(LocalDate.now().minusDays(1));
        assertTrue(tarea.isVencida());

        tarea.setEstado(EstadoTarea.FINALIZADO);
        assertFalse(tarea.isVencida());
        assertEquals(5, EstadoTarea.values().length);
    }

    @Test
    void actualizarFechasValidaElRangoYReiniciaElRecordatorio() {
        LocalDate limite = LocalDate.now().plusDays(10);
        assertThrows(IllegalArgumentException.class,
                () -> service.actualizarFechas(DIRECCION, 1L, TAREA, limite.plusDays(1), limite, RecordatorioTarea.NINGUNO));
        assertThrows(IllegalArgumentException.class,
                () -> service.actualizarFechas(DIRECCION, 1L, TAREA, null, null, RecordatorioTarea.NINGUNO));

        tarea.setRecordatorioEnviado(true);
        service.actualizarFechas(DIRECCION, 1L, TAREA, LocalDate.now(), limite, RecordatorioTarea.UN_DIA_ANTES);

        assertEquals(limite, tarea.getFechaLimite());
        assertEquals(RecordatorioTarea.UN_DIA_ANTES, tarea.getRecordatorio());
        assertFalse(tarea.isRecordatorioEnviado());
        verify(tareaRepository).save(tarea);
    }

    @Test
    void laChecklistCalculaElProgresoYRegistraCadaCambio() {
        service.agregarItem(DIRECCION, 1L, TAREA, "  Revisar objetivos  ");
        service.agregarItem(DIRECCION, 1L, TAREA, "Enviar a dirección");
        assertEquals(2, tarea.getChecklistTotal());
        assertEquals(0, tarea.getChecklistPorcentaje());
        assertThrows(IllegalArgumentException.class, () -> service.agregarItem(DIRECCION, 1L, TAREA, "   "));

        ChecklistItemTarea primero = tarea.getChecklist().get(0);
        ReflectionTestUtils.setField(primero, "id", 100L);
        service.alternarItem(DIRECCION, 1L, TAREA, 100L);

        assertEquals("Revisar objetivos", primero.getTexto());
        assertEquals(1, tarea.getChecklistCompletados());
        assertEquals(50, tarea.getChecklistPorcentaje());
        assertEquals("completó «Revisar objetivos»", tarea.getActividad().get(0).getTexto());

        service.eliminarItem(DIRECCION, 1L, TAREA, 100L);
        assertEquals(1, tarea.getChecklistTotal());
        assertThrows(IllegalArgumentException.class, () -> service.alternarItem(DIRECCION, 1L, TAREA, 999L));
    }

    @Test
    void lasEtiquetasSeAlternanYNoSePermitenNombresRepetidos() {
        EtiquetaTarea urgente = new EtiquetaTarea();
        urgente.setId(3L);
        urgente.setNombre("Urgente");
        urgente.setColor("#eb5a46");
        when(etiquetaRepository.findByIdAndDireccionId(3L, DIRECCION)).thenReturn(Optional.of(urgente));

        service.alternarEtiqueta(DIRECCION, 1L, TAREA, 3L);
        assertEquals(1, tarea.getEtiquetas().size());
        service.alternarEtiqueta(DIRECCION, 1L, TAREA, 3L);
        assertTrue(tarea.getEtiquetas().isEmpty());

        when(etiquetaRepository.existsByDireccionIdAndNombreIgnoreCase(DIRECCION, "urgente")).thenReturn(true);
        assertThrows(IllegalArgumentException.class,
                () -> service.crearEtiqueta(DIRECCION, 1L, TAREA, "urgente", "#eb5a46"));
        assertThrows(IllegalArgumentException.class,
                () -> service.crearEtiqueta(DIRECCION, 1L, TAREA, "Nueva", "rojo"));
        verify(etiquetaRepository, never()).save(any(EtiquetaTarea.class));
    }

    @Test
    void agregarUnColaboradorLoNotificaYNoSePuedeRepetirNiAgregarAlResponsable() {
        Usuario colega = usuario(3L, "Pedro Mora");
        when(usuarioRepository.findActivosByDireccionId(DIRECCION)).thenReturn(List.of(director, docente, colega));

        service.agregarColaborador(DIRECCION, 1L, TAREA, 3L);

        assertEquals(1, tarea.getColaboradores().size());
        verify(notificacionService).crear(eq(colega), contains("Redactar el planeamiento"), eq("/agenda"));
        assertThrows(IllegalArgumentException.class, () -> service.agregarColaborador(DIRECCION, 1L, TAREA, 3L));
        // El responsable (docente) ya participa
        assertThrows(IllegalArgumentException.class, () -> service.agregarColaborador(DIRECCION, 1L, TAREA, 2L));
        // Alguien de otra institución no está en la lista de activos
        assertThrows(IllegalArgumentException.class, () -> service.agregarColaborador(DIRECCION, 1L, TAREA, 99L));

        service.quitarColaborador(DIRECCION, 1L, TAREA, 3L);
        assertTrue(tarea.getColaboradores().isEmpty());
    }

    @Test
    void losEnlacesAdjuntosSoloAceptanHttp() throws Exception {
        service.agregarAdjuntos(DIRECCION, 1L, TAREA, List.of(), "www.mep.go.cr/circular");
        assertEquals(1, tarea.getAdjuntos().size());
        assertEquals("https://www.mep.go.cr/circular", tarea.getAdjuntos().get(0).getUrl());
        assertTrue(tarea.getAdjuntos().get(0).isEnlace());

        assertThrows(IllegalArgumentException.class,
                () -> service.agregarAdjuntos(DIRECCION, 1L, TAREA, List.of(), "https://mal enlace"));
        assertThrows(IllegalArgumentException.class,
                () -> service.agregarAdjuntos(DIRECCION, 1L, TAREA, List.of(), "  "));
    }

    @Test
    void soloElAutorOQuienSupervisaPuedeBorrarUnComentario() {
        service.comentar(DIRECCION, 1L, TAREA, "  Falta anexar la rúbrica  ");
        ActividadTarea comentario = tarea.getActividad().get(0);
        ReflectionTestUtils.setField(comentario, "id", 50L);
        assertEquals(TipoActividad.COMENTARIO, comentario.getTipo());
        assertEquals("Falta anexar la rúbrica", comentario.getTexto());

        // Otra persona sin permisos de supervisión
        assertThrows(AccessDeniedException.class, () -> service.eliminarComentario(DIRECCION, 2L, false, TAREA, 50L));
        assertEquals(1, tarea.getActividad().size());

        // Quien supervisa sí puede
        service.eliminarComentario(DIRECCION, 2L, true, TAREA, 50L);
        assertTrue(tarea.getActividad().isEmpty());
    }

    @Test
    void losRecordatoriosSoloSeEnvianCuandoLlegaLaFechaYUnaSolaVez() {
        tarea.setRecordatorio(RecordatorioTarea.UN_DIA_ANTES);
        tarea.setFechaLimite(LocalDate.now().plusDays(1));
        Usuario colega = usuario(3L, "Pedro Mora");
        tarea.getColaboradores().add(colega);
        when(tareaRepository.findConRecordatorioPendiente(RecordatorioTarea.NINGUNO, EstadoTarea.FINALIZADO))
                .thenReturn(List.of(tarea));

        assertEquals(1, service.enviarRecordatoriosPendientes());

        assertTrue(tarea.isRecordatorioEnviado());
        verify(notificacionService).crearTodas(anyList(), contains("vence mañana"), eq("/agenda"));

        // Todavía no llega el aviso: 1 semana antes de un vencimiento a 20 días
        TareaPlanTrabajo lejana = new TareaPlanTrabajo();
        lejana.setTitulo("Lejana");
        lejana.setFechaLimite(LocalDate.now().plusDays(20));
        lejana.setRecordatorio(RecordatorioTarea.UNA_SEMANA_ANTES);
        when(tareaRepository.findConRecordatorioPendiente(RecordatorioTarea.NINGUNO, EstadoTarea.FINALIZADO))
                .thenReturn(List.of(lejana));
        assertEquals(0, service.enviarRecordatoriosPendientes());
        assertFalse(lejana.isRecordatorioEnviado());
    }
}
