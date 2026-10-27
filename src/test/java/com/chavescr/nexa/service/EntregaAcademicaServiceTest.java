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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.chavescr.nexa.dto.PaginaEntregas;
import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.EntregaAcademica;
import com.chavescr.nexa.entity.EntregaAcademica.Plazo;
import com.chavescr.nexa.entity.EntregaAcademica.TipoEntrega;
import com.chavescr.nexa.entity.EntregaAcademicaDocente;
import com.chavescr.nexa.entity.EntregaAcademicaDocente.EstadoPresentacion;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.AdjuntoEntregaAcademicaRepository;
import com.chavescr.nexa.repository.DireccionRepository;
import com.chavescr.nexa.repository.EntregaAcademicaDocenteRepository;
import com.chavescr.nexa.repository.EntregaAcademicaRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;
import com.chavescr.nexa.repository.UsuarioRepository;
import com.chavescr.nexa.service.EntregaAcademicaService.FiltroEntregas;

@ExtendWith(MockitoExtension.class)
class EntregaAcademicaServiceTest {

    private static final Long DIRECCION = 1L;
    private static final Long DIRECTOR = 1L;
    private static final Long MARIA = 2L;
    private static final Long PEDRO = 3L;

    @Mock
    private EntregaAcademicaRepository entregaRepository;
    @Mock
    private EntregaAcademicaDocenteRepository presentacionRepository;
    @Mock
    private AdjuntoEntregaAcademicaRepository adjuntoRepository;
    @Mock
    private PeriodoAcademicoRepository periodoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private DireccionRepository direccionRepository;
    @Mock
    private AlmacenamientoService almacenamientoService;
    @Mock
    private NotificacionService notificacionService;

    private EntregaAcademicaService service;
    private Usuario director;
    private Usuario maria;
    private Usuario pedro;
    private EntregaAcademica planeamiento;
    private final List<EntregaAcademicaDocente> respuestas = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new EntregaAcademicaService(entregaRepository, presentacionRepository, adjuntoRepository,
                periodoRepository, usuarioRepository, direccionRepository, almacenamientoService, notificacionService);

        director = usuario(DIRECTOR, "Alejandro Chaves");
        maria = usuario(MARIA, "María Soto");
        pedro = usuario(PEDRO, "Pedro Mora");

        planeamiento = entrega(10L, "Planeamiento II trimestre", TipoEntrega.PLANEAMIENTO,
                LocalDate.now().minusDays(2), LocalDate.now().plusDays(10));

        lenient().when(usuarioRepository.findById(DIRECTOR)).thenReturn(Optional.of(director));
        lenient().when(usuarioRepository.findById(MARIA)).thenReturn(Optional.of(maria));
        lenient().when(usuarioRepository.findById(PEDRO)).thenReturn(Optional.of(pedro));
        lenient().when(usuarioRepository.findActivosByDireccionIdAndRol(DIRECCION, "ROLE_DOCENTE"))
                .thenReturn(List.of(maria, pedro));
        lenient().when(entregaRepository.findByIdAndDireccionId(10L, DIRECCION)).thenReturn(Optional.of(planeamiento));
        lenient().when(entregaRepository
                .findByDireccionIdAndActivaTrueAndFechaInicioLessThanEqualOrderByFechaFinAsc(eq(DIRECCION), any()))
                .thenReturn(List.of(planeamiento));
        lenient().when(presentacionRepository.findByEntrega_Direccion_Id(DIRECCION)).thenAnswer(i -> respuestas);
        lenient().when(presentacionRepository.findByEntrega_Direccion_IdAndDocenteId(eq(DIRECCION), any()))
                .thenAnswer(i -> respuestas.stream()
                        .filter(r -> r.getDocente().getId().equals(i.getArgument(1))).toList());
        lenient().when(presentacionRepository.findByEntregaIdAndDocenteId(eq(10L), any()))
                .thenAnswer(i -> respuestas.stream().filter(r -> r.getDocente().getId().equals(i.getArgument(1)))
                        .findFirst());
        lenient().when(presentacionRepository.save(any(EntregaAcademicaDocente.class))).thenAnswer(i -> {
            EntregaAcademicaDocente r = i.getArgument(0);
            if (!respuestas.contains(r)) {
                ReflectionTestUtils.setField(r, "id", 500L + respuestas.size());
                respuestas.add(r);
            }
            return r;
        });
    }

    private Usuario usuario(Long id, String nombre) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre(nombre);
        return u;
    }

    private EntregaAcademica entrega(Long id, String nombre, TipoEntrega tipo, LocalDate inicio, LocalDate fin) {
        EntregaAcademica e = new EntregaAcademica();
        e.setId(id);
        e.setNombre(nombre);
        e.setTipo(tipo);
        e.setFechaInicio(inicio);
        e.setFechaFin(fin);
        e.setCreadoPor(director);
        Direccion d = new Direccion();
        d.setId(DIRECCION);
        e.setDireccion(d);
        return e;
    }

    @Test
    void guardarEntregaValidaElPlazoYAvisaALosDocentesSiYaEmpezo() {
        assertThrows(IllegalArgumentException.class, () -> service.guardarEntrega(DIRECCION, DIRECTOR, null, "  ",
                null, TipoEntrega.INFORME, null, LocalDate.now(), LocalDate.now().plusDays(3), true));
        assertThrows(IllegalArgumentException.class, () -> service.guardarEntrega(DIRECCION, DIRECTOR, null, "Informe",
                null, TipoEntrega.INFORME, null, null, LocalDate.now(), true));
        assertThrows(IllegalArgumentException.class, () -> service.guardarEntrega(DIRECCION, DIRECTOR, null, "Informe",
                null, TipoEntrega.INFORME, null, LocalDate.now(), LocalDate.now().minusDays(1), true));
        verify(entregaRepository, never()).save(any(EntregaAcademica.class));

        when(direccionRepository.findById(DIRECCION)).thenReturn(Optional.of(planeamiento.getDireccion()));
        when(entregaRepository.save(any(EntregaAcademica.class))).thenAnswer(i -> i.getArgument(0));

        EntregaAcademica guardada = service.guardarEntrega(DIRECCION, DIRECTOR, null, "  Informe de notas ", "desc",
                TipoEntrega.INFORME, null, LocalDate.now(), LocalDate.now().plusDays(7), true);

        assertEquals("Informe de notas", guardada.getNombre());
        assertEquals(director, guardada.getCreadoPor());
        verify(notificacionService).crearTodas(anyList(), contains("Informe de notas"), eq("/entregas-academicas"));

        // Una entrega que empieza en el futuro todavía no avisa a nadie
        EntregaAcademica futura = service.guardarEntrega(DIRECCION, DIRECTOR, null, "Futura", null,
                TipoEntrega.OTRA, null, LocalDate.now().plusDays(5), LocalDate.now().plusDays(9), true);
        assertEquals(Plazo.PROXIMA, futura.getPlazo());
    }

    @Test
    void elPlazoSeCalculaRespectoAHoy() {
        assertEquals(Plazo.EN_PLAZO, planeamiento.getPlazo());
        planeamiento.setFechaFin(LocalDate.now().minusDays(1));
        assertEquals(Plazo.VENCIDA, planeamiento.getPlazo());
        planeamiento.setFechaInicio(LocalDate.now().plusDays(1));
        planeamiento.setFechaFin(LocalDate.now().plusDays(5));
        assertEquals(Plazo.PROXIMA, planeamiento.getPlazo());
    }

    @Test
    void elDocenteVeSoloLoSuyoYLaDireccionVeATodos() {
        PaginaEntregas propias = service.listar(DIRECCION, MARIA, false, true, null, 1);
        assertEquals(1, propias.getTotal());
        assertEquals(maria, propias.getFilas().get(0).getDocente());
        assertEquals("Sin iniciar", propias.getFilas().get(0).getEstadoEtiqueta());
        assertEquals("Iniciar", propias.getFilas().get(0).getAccionDocente());

        PaginaEntregas todas = service.listar(DIRECCION, DIRECTOR, true, true, null, 1);
        assertEquals(2, todas.getTotal());
        assertEquals(2, service.contarPendientes(DIRECCION, DIRECTOR, true));
        assertEquals(1, service.contarPendientes(DIRECCION, PEDRO, false));
    }

    @Test
    void losFiltrosYLaPaginacionFuncionan() {
        List<EntregaAcademica> muchas = new ArrayList<>();
        for (long i = 1; i <= 23; i++) {
            muchas.add(entrega(100 + i, "Entrega " + i, i % 2 == 0 ? TipoEntrega.INFORME : TipoEntrega.PLANEAMIENTO,
                    LocalDate.now().minusDays(1), LocalDate.now().plusDays(i)));
        }
        when(entregaRepository.findByDireccionIdAndActivaTrueAndFechaInicioLessThanEqualOrderByFechaFinAsc(eq(DIRECCION), any()))
                .thenReturn(muchas);

        PaginaEntregas primera = service.listar(DIRECCION, MARIA, false, true, null, 1);
        assertEquals(23, primera.getTotal());
        assertEquals(3, primera.getTotalPaginas());
        assertEquals(EntregaAcademicaService.TAMANO_PAGINA, primera.getFilas().size());
        assertTrue(primera.isHaySiguiente());
        assertFalse(primera.isHayAnterior());

        PaginaEntregas ultima = service.listar(DIRECCION, MARIA, false, true, null, 99);
        assertEquals(3, ultima.getPagina());
        assertEquals(3, ultima.getFilas().size());

        PaginaEntregas informes = service.listar(DIRECCION, MARIA, false, true,
                new FiltroEntregas(null, TipoEntrega.INFORME, null), 1);
        assertEquals(11, informes.getTotal());

        PaginaEntregas una = service.listar(DIRECCION, MARIA, false, true,
                new FiltroEntregas(105L, null, null), 1);
        assertEquals(1, una.getTotal());
    }

    @Test
    void elFlujoCompletoBorradorPresentarDevolverYAprobar() {
        // Sin nada que presentar no se deja presentar
        assertThrows(IllegalArgumentException.class, () -> service.presentar(DIRECCION, MARIA, 10L, "  "));

        // El borrador se crea al guardar y sigue en Pendientes
        service.guardarBorrador(DIRECCION, MARIA, 10L, "  Avance 50%  ");
        EntregaAcademicaDocente respuesta = respuestas.get(0);
        assertEquals(EstadoPresentacion.BORRADOR, respuesta.getEstado());
        assertEquals("Avance 50%", respuesta.getObservacion());
        assertEquals(1, service.listar(DIRECCION, MARIA, false, true, null, 1).getTotal());
        assertEquals("Continuar", service.listar(DIRECCION, MARIA, false, true, null, 1).getFilas().get(0).getAccionDocente());

        // Presentar: pasa a Realizadas y avisa a quien creó la entrega
        service.presentar(DIRECCION, MARIA, 10L, "Listo para revisión");
        assertEquals(EstadoPresentacion.PRESENTADA, respuesta.getEstado());
        assertNotNull(respuesta.getFechaPresentacion());
        verify(notificacionService).crear(eq(director), contains("María Soto presentó"), eq("/entregas-academicas"));
        assertEquals(0, service.listar(DIRECCION, MARIA, false, true, null, 1).getTotal());
        assertEquals(1, service.listar(DIRECCION, MARIA, false, false, null, 1).getTotal());
        // Ya presentada no se puede modificar
        assertThrows(IllegalArgumentException.class, () -> service.guardarBorrador(DIRECCION, MARIA, 10L, "cambio"));

        // La dirección la devuelve: exige observación y vuelve a Pendientes
        when(presentacionRepository.findByIdAndEntrega_Direccion_Id(respuesta.getId(), DIRECCION))
                .thenReturn(Optional.of(respuesta));
        assertThrows(IllegalArgumentException.class,
                () -> service.revisar(DIRECCION, DIRECTOR, respuesta.getId(), false, "  "));
        service.revisar(DIRECCION, DIRECTOR, respuesta.getId(), false, "Falta la rúbrica");
        assertEquals(EstadoPresentacion.DEVUELTA, respuesta.getEstado());
        assertEquals("Falta la rúbrica", respuesta.getObservacionRevision());
        verify(notificacionService).crear(eq(maria), contains("devuelta"), eq("/entregas-academicas"));
        assertEquals("Corregir", service.listar(DIRECCION, MARIA, false, true, null, 1).getFilas().get(0).getAccionDocente());

        // Corrige, presenta de nuevo y la dirección la aprueba
        service.presentar(DIRECCION, MARIA, 10L, null);
        assertNull(respuesta.getObservacionRevision());
        service.revisar(DIRECCION, DIRECTOR, respuesta.getId(), true, null);
        assertEquals(EstadoPresentacion.APROBADA, respuesta.getEstado());
        assertEquals(director, respuesta.getRevisadoPor());
        verify(notificacionService).crear(eq(maria), contains("aprobada"), eq("/entregas-academicas"));
        // Ya aprobada no se vuelve a revisar
        assertThrows(IllegalArgumentException.class,
                () -> service.revisar(DIRECCION, DIRECTOR, respuesta.getId(), true, null));
    }

    @Test
    void unaEntregaFuturaOInactivaNoSePuedeEmpezar() {
        planeamiento.setFechaInicio(LocalDate.now().plusDays(3));
        planeamiento.setFechaFin(LocalDate.now().plusDays(9));
        assertThrows(IllegalArgumentException.class, () -> service.guardarBorrador(DIRECCION, MARIA, 10L, "adelantado"));
        assertThrows(IllegalArgumentException.class, () -> service.detalle(DIRECCION, MARIA, false, 10L, null));
        // La dirección sí puede abrirla para ver el estado
        assertEquals(maria, service.detalle(DIRECCION, DIRECTOR, true, 10L, MARIA).getDocente());

        planeamiento.setFechaInicio(LocalDate.now().minusDays(1));
        planeamiento.setActiva(false);
        assertThrows(IllegalArgumentException.class, () -> service.guardarBorrador(DIRECCION, MARIA, 10L, "inactiva"));
    }

    @Test
    void losEnlacesAdjuntosSoloAceptanHttpYNoSeAdjuntaEnUnaEntregaPresentada() throws Exception {
        service.agregarAdjuntos(DIRECCION, MARIA, 10L, List.of(), "www.mep.go.cr/circular");
        EntregaAcademicaDocente respuesta = respuestas.get(0);
        assertEquals(1, respuesta.getAdjuntos().size());
        assertEquals("https://www.mep.go.cr/circular", respuesta.getAdjuntos().get(0).getUrl());

        assertThrows(IllegalArgumentException.class,
                () -> service.agregarAdjuntos(DIRECCION, MARIA, 10L, List.of(), "https://mal enlace"));
        assertThrows(IllegalArgumentException.class,
                () -> service.agregarAdjuntos(DIRECCION, MARIA, 10L, List.of(), " "));

        // Con un enlace adjunto ya se puede presentar sin observación
        service.presentar(DIRECCION, MARIA, 10L, null);
        assertThrows(IllegalArgumentException.class,
                () -> service.agregarAdjuntos(DIRECCION, MARIA, 10L, List.of(), "https://otro.cr"));
    }

    @Test
    void unaEntregaConRespuestasNoSeEliminaSoloSeDesactiva() {
        when(presentacionRepository.existsByEntregaId(10L)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.eliminarEntrega(DIRECCION, 10L));
        verify(entregaRepository, never()).delete(any(EntregaAcademica.class));

        when(presentacionRepository.existsByEntregaId(10L)).thenReturn(false);
        service.eliminarEntrega(DIRECCION, 10L);
        verify(entregaRepository).delete(planeamiento);
    }

    @Test
    void elSeguimientoListaATodosLosDocentesConSuEstado() {
        service.guardarBorrador(DIRECCION, MARIA, 10L, "avance");
        when(presentacionRepository.findByEntregaId(10L)).thenReturn(respuestas);

        var filas = service.seguimiento(DIRECCION, 10L);

        assertEquals(2, filas.size());
        assertEquals("Borrador", filas.get(0).getEstadoEtiqueta());
        assertEquals("Sin iniciar", filas.get(1).getEstadoEtiqueta());
    }
}
