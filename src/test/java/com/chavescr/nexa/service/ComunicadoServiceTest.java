package com.chavescr.nexa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chavescr.nexa.dto.ComunicadosVista;
import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.NubeNodo;
import com.chavescr.nexa.entity.Oficio;
import com.chavescr.nexa.entity.OficioDestinatario;
import com.chavescr.nexa.entity.OficioLectura;
import com.chavescr.nexa.entity.Rol;
import com.chavescr.nexa.entity.TipoDestinatario;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.OficioLecturaRepository;
import com.chavescr.nexa.repository.OficioRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class ComunicadoServiceTest {

    private static final Long DIRECCION = 1L;

    @Mock
    private OficioRepository oficioRepository;
    @Mock
    private OficioLecturaRepository lecturaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private NivelAcademicoRepository nivelRepository;
    @Mock
    private NotificacionService notificacionService;
    @Mock
    private NubeNodoService nubeNodoService;

    private ComunicadoService service;
    private Direccion direccion;
    private Usuario directora;
    private Usuario docente;
    private Usuario madre;
    private Usuario otraMadre;
    private Usuario hija;
    private NivelAcademico seccionHija;

    @BeforeEach
    void setUp() {
        service = new ComunicadoService(oficioRepository, lecturaRepository, usuarioRepository, nivelRepository,
                notificacionService, nubeNodoService);

        direccion = new Direccion();
        direccion.setId(DIRECCION);

        directora = usuario(1L, "María González", "ROLE_DIRECTOR");
        docente = usuario(2L, "Carlos López", "ROLE_DOCENTE");
        madre = usuario(3L, "Rosa Jiménez", "ROLE_PADRE");
        otraMadre = usuario(4L, "Manuel Alpízar", "ROLE_PADRE");
        hija = usuario(5L, "Sofía Alpízar", "ROLE_ESTUDIANTE");

        seccionHija = new NivelAcademico();
        seccionHija.setId(10L);
        seccionHija.setGrado(8);
        seccionHija.setSeccion("2");
        seccionHija.setDireccion(direccion);
        hija.setNivelAcademico(seccionHija);
        madre.getEstudiantes().add(hija);
        hija.getPadres().add(madre);
    }

    private Usuario usuario(Long id, String nombre, String rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre(nombre);
        u.setActivo(true);
        u.setRoles(new HashSet<>(Set.of(new Rol(rol))));
        u.setDirecciones(new HashSet<>(Set.of(direccion)));
        u.setEstudiantes(new HashSet<>());
        u.setPadres(new HashSet<>());
        return u;
    }

    private Oficio oficio(Long id, String asunto, OficioDestinatario... destinatarios) {
        Oficio o = new Oficio();
        o.setId(id);
        o.setDireccion(direccion);
        o.setNumero("2026-00" + id);
        o.setAsunto(asunto);
        o.setEstado("EMITIDO");
        o.setFecha(LocalDate.of(2026, 10, 12));
        o.setRedactadoPor(directora);
        NubeNodo nodo = new NubeNodo();
        nodo.setNombre("oficio.pdf");
        nodo.setExtension("pdf");
        nodo.setUrlArchivo("1/Oficios/oficio.pdf");
        o.setNubeNodo(nodo);
        for (OficioDestinatario d : destinatarios) {
            d.setOficio(o);
            o.getDestinatarios().add(d);
        }
        return o;
    }

    private OficioDestinatario grupo(TipoDestinatario tipo) {
        return new OficioDestinatario(null, tipo, null, null);
    }

    private OficioDestinatario persona(Usuario u) {
        return new OficioDestinatario(null, TipoDestinatario.PERSONA, u, null);
    }

    private OficioDestinatario familiasDe(NivelAcademico n) {
        return new OficioDestinatario(null, TipoDestinatario.FAMILIAS_SECCION, null, n);
    }

    private void conocer(Usuario... usuarios) {
        for (Usuario u : usuarios) {
            when(usuarioRepository.findById(u.getId())).thenReturn(Optional.of(u));
        }
    }

    // ── Destinatarios del formulario ──────────────────────────────────────────

    @Test
    void resuelveGruposSeccionesYPersonasYQuitaRepetidos() {
        conocer(madre);
        when(nivelRepository.findById(10L)).thenReturn(Optional.of(seccionHija));

        List<OficioDestinatario> resueltos = service.resolver(new Oficio(), DIRECCION,
                List.of("COMUNIDAD", "SECCION-10", "PERSONA-3", "COMUNIDAD"));

        assertEquals(List.of("COMUNIDAD", "SECCION-10", "PERSONA-3"),
                resueltos.stream().map(OficioDestinatario::clave).toList());
    }

    @Test
    void rechazaDestinatariosInvalidos() {
        Oficio o = new Oficio();
        assertThrows(IllegalArgumentException.class, () -> service.resolver(o, DIRECCION, List.of("TODOS")));
        assertThrows(IllegalArgumentException.class, () -> service.resolver(o, DIRECCION, List.of("PERSONA")));
        assertThrows(IllegalArgumentException.class, () -> service.resolver(o, DIRECCION, List.of("PERSONA-abc")));
        assertThrows(IllegalArgumentException.class, () -> service.resolver(o, DIRECCION, List.of("SECCION")));
    }

    @Test
    void rechazaPersonasInactivasOAjenasALaDireccion() {
        madre.setActivo(false);
        Usuario deOtraDireccion = usuario(6L, "Jorge Salas", "ROLE_PADRE");
        Direccion otra = new Direccion();
        otra.setId(99L);
        deOtraDireccion.setDirecciones(new HashSet<>(Set.of(otra)));
        conocer(madre, deOtraDireccion);

        assertThrows(IllegalArgumentException.class, () -> service.resolver(new Oficio(), DIRECCION, List.of("PERSONA-3")));
        assertThrows(IllegalArgumentException.class, () -> service.resolver(new Oficio(), DIRECCION, List.of("PERSONA-6")));
    }

    @Test
    void rechazaSeccionesDeOtraDireccion() {
        Direccion otra = new Direccion();
        otra.setId(99L);
        seccionHija.setDireccion(otra);
        when(nivelRepository.findById(10L)).thenReturn(Optional.of(seccionHija));

        assertThrows(IllegalArgumentException.class, () -> service.resolver(new Oficio(), DIRECCION, List.of("SECCION-10")));
    }

    // ── Avisos al emitir ──────────────────────────────────────────────────────

    @Test
    void alPublicarAvisaSoloALosDestinatariosYNoAQuienRedacta() {
        Oficio o = oficio(7L, "Reunión de docentes", grupo(TipoDestinatario.DOCENTES), persona(madre));
        when(usuarioRepository.findActivosByDireccionIdAndRol(DIRECCION, "ROLE_DOCENTE")).thenReturn(List.of(docente, directora));

        int avisados = service.publicar(o);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Usuario>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificacionService).crearTodas(captor.capture(), contains("OF-2026-007"), eq("/comunicacion?tab=comunicados&o=7"));
        assertEquals(2, avisados);
        assertTrue(captor.getValue().contains(docente));
        assertTrue(captor.getValue().contains(madre));
        assertFalse(captor.getValue().contains(directora), "quien redacta no necesita aviso");
    }

    @Test
    void alPublicarUnaSeccionAvisaALasFamiliasDeSusEstudiantes() {
        Oficio o = oficio(8L, "Reunión de padres", familiasDe(seccionHija));
        when(usuarioRepository.findEstudiantesActivosByNivelId(10L)).thenReturn(List.of(hija));

        service.publicar(o);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Usuario>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificacionService).crearTodas(captor.capture(), anyString(), anyString());
        assertEquals(List.of(madre), captor.getValue());
    }

    @Test
    void sinDestinatariosInternosNoAvisaANadie() {
        assertEquals(0, service.publicar(oficio(9L, "Solo correo externo")));
        verify(notificacionService, never()).crearTodas(anyList(), anyString(), anyString());
    }

    // ── Quién ve qué ──────────────────────────────────────────────────────────

    @Test
    void cadaPersonaVeSoloLosOficiosQueLeLlegan() {
        Oficio paraDocentes = oficio(1L, "Docentes", grupo(TipoDestinatario.DOCENTES));
        Oficio paraSeccion = oficio(2L, "Sección", familiasDe(seccionHija));
        Oficio paraTodos = oficio(3L, "Todos", grupo(TipoDestinatario.COMUNIDAD));
        Oficio paraRosa = oficio(4L, "Rosa", persona(madre));
        when(oficioRepository.emitidosConDestinatarios(DIRECCION)).thenReturn(List.of(paraTodos, paraSeccion, paraRosa, paraDocentes));
        conocer(madre, otraMadre, docente);

        List<String> deMadre = numeros(service.panel("PERSONA-3", DIRECCION, 3L, false, null, null, null));
        List<String> deOtraMadre = numeros(service.panel("PERSONA-4", DIRECCION, 4L, false, null, null, null));
        List<String> deDocente = numeros(service.panel("PERSONA-2", DIRECCION, 2L, false, null, null, null));

        assertEquals(List.of("OF-2026-003", "OF-2026-002", "OF-2026-004"), deMadre);
        assertEquals(List.of("OF-2026-003"), deOtraMadre);
        assertEquals(List.of("OF-2026-003", "OF-2026-001"), deDocente);
    }

    @Test
    void quienNoSuperviseSoloPuedePedirSuPropioPanel() {
        conocer(madre, otraMadre);

        assertThrows(IllegalArgumentException.class, () -> service.panel("PERSONA-4", DIRECCION, 3L, false, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> service.panel("COMUNIDAD", DIRECCION, 3L, false, null, null, null));
    }

    @Test
    void laSupervisionVeElProgresoDeLecturaDelGrupo() {
        Oficio paraTodos = oficio(3L, "Todos", grupo(TipoDestinatario.COMUNIDAD));
        when(oficioRepository.emitidosConDestinatarios(DIRECCION)).thenReturn(List.of(paraTodos));
        when(usuarioRepository.findActivosByDireccionId(DIRECCION)).thenReturn(List.of(directora, docente, madre, otraMadre));
        List<Object[]> lecturas = List.<Object[]>of(new Object[] { 3L, 3L, java.time.LocalDateTime.now() });
        when(lecturaRepository.lecturasDe(List.of(3L))).thenReturn(lecturas);
        conocer(directora);

        ComunicadosVista.Panel panel = service.panel("COMUNIDAD", DIRECCION, 1L, true, null, null, null);

        ComunicadosVista.Lectura lectura = panel.oficios().get(0).lectura();
        assertEquals(1, lectura.leidos());
        assertEquals(4, lectura.total());
        assertEquals(3, lectura.pendientes());
        assertEquals(1L, panel.sinLeer());
    }

    @Test
    void filtraPorTipoYPorTexto() {
        Oficio circular = oficio(1L, "Calendario de exámenes", grupo(TipoDestinatario.COMUNIDAD));
        circular.setNumeroCircular("CIR-2026-010");
        Oficio simple = oficio(2L, "Autorización de salida", grupo(TipoDestinatario.COMUNIDAD));
        when(oficioRepository.emitidosConDestinatarios(DIRECCION)).thenReturn(List.of(circular, simple));
        conocer(madre);

        assertEquals(List.of("OF-2026-001"), numeros(service.panel("PERSONA-3", DIRECCION, 3L, false, null, "CIRCULAR", null)));
        assertEquals(List.of("OF-2026-002"), numeros(service.panel("PERSONA-3", DIRECCION, 3L, false, null, "OFICIO", null)));
        assertEquals(List.of("OF-2026-002"), numeros(service.panel("PERSONA-3", DIRECCION, 3L, false, "SALIDA", null, null)));
        assertEquals(List.of(), numeros(service.panel("PERSONA-3", DIRECCION, 3L, false, null, null, 2025)));
    }

    // ── Documento y lectura ───────────────────────────────────────────────────

    @Test
    void abrirElDocumentoRegistraLaLecturaUnaSolaVez() {
        Oficio o = oficio(1L, "Docentes", grupo(TipoDestinatario.DOCENTES));
        when(oficioRepository.emitidoConDestinatarios(1L, DIRECCION)).thenReturn(Optional.of(o));
        when(nubeNodoService.getRutaRecursos()).thenReturn("/datos");
        conocer(docente);
        when(lecturaRepository.existsByOficioIdAndUsuarioId(1L, 2L)).thenReturn(false, true);

        ComunicadosVista.Documento primero = service.documento(1L, DIRECCION, 2L, false, false);
        service.documento(1L, DIRECCION, 2L, false, true);

        assertTrue(primero.enLinea(), "el PDF se muestra en la página");
        assertEquals("application/pdf", primero.tipoContenido());
        verify(lecturaRepository).saveAndFlush(any(OficioLectura.class));
    }

    @Test
    void descargarUnPdfLoEntregaComoAdjunto() {
        Oficio o = oficio(1L, "Todos", grupo(TipoDestinatario.COMUNIDAD));
        when(oficioRepository.emitidoConDestinatarios(1L, DIRECCION)).thenReturn(Optional.of(o));
        when(nubeNodoService.getRutaRecursos()).thenReturn("/datos");
        conocer(madre);

        assertFalse(service.documento(1L, DIRECCION, 3L, false, true).enLinea());
    }

    @Test
    void quienNoEsDestinatarioNoPuedeAbrirElDocumento() {
        Oficio o = oficio(1L, "Docentes", grupo(TipoDestinatario.DOCENTES));
        when(oficioRepository.emitidoConDestinatarios(1L, DIRECCION)).thenReturn(Optional.of(o));
        conocer(madre);

        assertThrows(IllegalArgumentException.class, () -> service.documento(1L, DIRECCION, 3L, false, false));
        verify(lecturaRepository, never()).saveAndFlush(any());
    }

    @Test
    void laSupervisionPuedeAbrirCualquierOficioPeroSoloDejaLecturaSiLeLlega() {
        Oficio o = oficio(1L, "Docentes", grupo(TipoDestinatario.DOCENTES));
        when(oficioRepository.emitidoConDestinatarios(1L, DIRECCION)).thenReturn(Optional.of(o));
        when(nubeNodoService.getRutaRecursos()).thenReturn("/datos");
        conocer(directora);

        assertTrue(service.documento(1L, DIRECCION, 1L, true, false).enLinea());
        verify(lecturaRepository, never()).saveAndFlush(any());
    }

    @Test
    void unDocumentoFueraDeLaCarpetaDeRecursosSeRechaza() {
        Oficio o = oficio(1L, "Todos", grupo(TipoDestinatario.COMUNIDAD));
        o.getNubeNodo().setUrlArchivo("../../etc/passwd");
        when(oficioRepository.emitidoConDestinatarios(1L, DIRECCION)).thenReturn(Optional.of(o));
        when(nubeNodoService.getRutaRecursos()).thenReturn("/datos");
        conocer(madre);

        assertThrows(IllegalArgumentException.class, () -> service.documento(1L, DIRECCION, 3L, false, false));
    }

    private static List<String> numeros(ComunicadosVista.Panel panel) {
        return panel.oficios().stream().map(ComunicadosVista.Oficio::numero).toList();
    }
}
