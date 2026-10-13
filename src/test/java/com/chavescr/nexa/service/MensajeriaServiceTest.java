package com.chavescr.nexa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.chavescr.nexa.entity.Conversacion;
import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.Mensaje;
import com.chavescr.nexa.entity.MensajeReaccion;
import com.chavescr.nexa.entity.Rol;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.ConversacionRepository;
import com.chavescr.nexa.repository.DireccionRepository;
import com.chavescr.nexa.repository.MensajeReaccionRepository;
import com.chavescr.nexa.repository.MensajeRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class MensajeriaServiceTest {

    private static final Long DIRECCION = 1L;

    @Mock
    private ConversacionRepository conversacionRepository;
    @Mock
    private MensajeRepository mensajeRepository;
    @Mock
    private MensajeReaccionRepository reaccionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private DireccionRepository direccionRepository;
    @Mock
    private NotificacionService notificacionService;
    @Mock
    private AlmacenamientoService almacenamientoService;

    private MensajeriaService service;
    private Direccion direccion;
    private Usuario docente;
    private Usuario madre;
    private Usuario otraMadre;
    private Usuario hija;
    private Usuario estudianteAjeno;

    @BeforeEach
    void setUp() {
        service = new MensajeriaService(conversacionRepository, mensajeRepository, reaccionRepository,
                usuarioRepository, direccionRepository, notificacionService, almacenamientoService);

        direccion = new Direccion();
        direccion.setId(DIRECCION);

        docente = usuario(1L, "María González", "ROLE_DOCENTE");
        madre = usuario(2L, "Rosa Jiménez", "ROLE_PADRE");
        otraMadre = usuario(3L, "Manuel Alpízar", "ROLE_PADRE");
        hija = usuario(4L, "Sofía Alpízar", "ROLE_ESTUDIANTE");
        estudianteAjeno = usuario(5L, "Valeria Solano", "ROLE_ESTUDIANTE");

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

    private void conocer(Usuario... usuarios) {
        for (Usuario u : usuarios) {
            when(usuarioRepository.findById(u.getId())).thenReturn(Optional.of(u));
        }
    }

    @Test
    void unaMadreNoPuedeIniciarConversacionConOtraMadre() {
        conocer(madre, otraMadre);

        assertThrows(IllegalArgumentException.class,
                () -> service.abrirOCrear(madre.getId(), otraMadre.getId(), DIRECCION));
        verify(conversacionRepository, never()).save(any());
    }

    @Test
    void unaMadreNoPuedeEscribirleAUnEstudianteQueNoEsSuHijo() {
        conocer(madre, estudianteAjeno);

        assertThrows(IllegalArgumentException.class,
                () -> service.abrirOCrear(madre.getId(), estudianteAjeno.getId(), DIRECCION));
    }

    @Test
    void unaMadrePuedeEscribirleASuHijoYAlPersonal() {
        conocer(madre, hija, docente);
        when(conversacionRepository.buscarPar(eq(DIRECCION), anyLong(), anyLong())).thenReturn(Optional.empty());
        when(direccionRepository.findById(DIRECCION)).thenReturn(Optional.of(direccion));
        when(conversacionRepository.save(any(Conversacion.class))).thenAnswer(inv -> {
            Conversacion c = inv.getArgument(0);
            c.setId(9L);
            return c;
        });

        assertEquals(9L, service.abrirOCrear(madre.getId(), hija.getId(), DIRECCION));
        assertEquals(9L, service.abrirOCrear(madre.getId(), docente.getId(), DIRECCION));
    }

    @Test
    void elParSeGuardaOrdenadoPorId() {
        conocer(madre, docente);
        when(conversacionRepository.buscarPar(DIRECCION, 1L, 2L)).thenReturn(Optional.empty());
        when(direccionRepository.findById(DIRECCION)).thenReturn(Optional.of(direccion));
        when(conversacionRepository.save(any(Conversacion.class))).thenAnswer(inv -> inv.getArgument(0));

        service.abrirOCrear(madre.getId(), docente.getId(), DIRECCION);

        verify(conversacionRepository).buscarPar(DIRECCION, 1L, 2L);
    }

    @Test
    void elPersonalNoPuedeEscribirleAUnUsuarioInactivo() {
        madre.setActivo(false);
        conocer(docente, madre);

        assertThrows(IllegalArgumentException.class,
                () -> service.abrirOCrear(docente.getId(), madre.getId(), DIRECCION));
    }

    @Test
    void alEnviarSeNotificaAlDestinatarioConElTextoDelMensaje() throws IOException {
        Conversacion conversacion = conversacion(5L, docente, madre);
        when(conversacionRepository.findByIdConUsuarios(5L)).thenReturn(Optional.of(conversacion));
        conocer(docente);
        when(mensajeRepository.save(any(Mensaje.class))).thenAnswer(inv -> {
            Mensaje m = inv.getArgument(0);
            m.setId(30L);
            return m;
        });
        when(mensajeRepository.contarPendientes(5L, madre.getId(), 0L)).thenReturn(1L);

        service.enviar(5L, docente.getId(), "  Hola Rosa  ", null, null);

        verify(notificacionService).reemplazarPorEnlace(eq(madre), contains("María González: Hola Rosa"),
                eq("/comunicacion?c=5"));
        assertEquals(30L, conversacion.getUltimoLeido(docente.getId()));
        assertEquals(0L, conversacion.getUltimoLeido(madre.getId()));
    }

    @Test
    void conVariosPendientesLaNotificacionDiceCuantosSon() throws IOException {
        Conversacion conversacion = conversacion(5L, docente, madre);
        when(conversacionRepository.findByIdConUsuarios(5L)).thenReturn(Optional.of(conversacion));
        conocer(docente);
        when(mensajeRepository.save(any(Mensaje.class))).thenAnswer(inv -> {
            Mensaje m = inv.getArgument(0);
            m.setId(31L);
            return m;
        });
        when(mensajeRepository.contarPendientes(5L, madre.getId(), 0L)).thenReturn(3L);

        service.enviar(5L, docente.getId(), "Otro más", null, null);

        verify(notificacionService).reemplazarPorEnlace(eq(madre), contains("te envió 3 mensajes nuevos"),
                eq("/comunicacion?c=5"));
    }

    @Test
    void noSePuedeEnviarUnMensajeVacio() {
        Conversacion conversacion = conversacion(5L, docente, madre);
        when(conversacionRepository.findByIdConUsuarios(5L)).thenReturn(Optional.of(conversacion));
        conocer(docente);

        assertThrows(IllegalArgumentException.class,
                () -> service.enviar(5L, docente.getId(), "   ", null, List.of()));
        verify(notificacionService, never()).reemplazarPorEnlace(any(), any(), any());
    }

    @Test
    void unTerceroNoPuedeAbrirNiEscribirEnLaConversacion() {
        Conversacion conversacion = conversacion(5L, docente, madre);
        when(conversacionRepository.findByIdConUsuarios(5L)).thenReturn(Optional.of(conversacion));

        assertThrows(IllegalArgumentException.class, () -> service.abrir(5L, otraMadre.getId(), DIRECCION));
        assertThrows(IllegalArgumentException.class,
                () -> service.enviar(5L, otraMadre.getId(), "hola", null, null));
    }

    @Test
    void laConversacionSeAbreDesdeCualquierDireccionActivaYSeMarcaSiEsDeOtra() {
        direccion.setNombre("Liceo Alpha");
        Conversacion conversacion = conversacion(5L, docente, madre);
        when(conversacionRepository.findByIdConUsuarios(5L)).thenReturn(Optional.of(conversacion));

        assertEquals("Liceo Alpha", service.abrir(5L, madre.getId(), 99L).direccion());
        assertEquals(null, service.abrir(5L, madre.getId(), DIRECCION).direccion());
    }

    @Test
    void laListaIncluyeLasConversacionesDeTodasLasDireccionesDelUsuario() {
        direccion.setNombre("Liceo Alpha");
        Conversacion conversacion = conversacion(5L, docente, madre);
        when(conversacionRepository.listarDeUsuario(madre.getId())).thenReturn(List.of(conversacion));

        var lista = service.listar(madre.getId(), 99L);

        assertEquals(1, lista.size());
        assertEquals("Liceo Alpha", lista.get(0).direccion());
    }

    @Test
    void reaccionarAlternaLaReaccionDelUsuario() {
        Conversacion conversacion = conversacion(5L, docente, madre);
        Mensaje mensaje = new Mensaje();
        mensaje.setId(30L);
        mensaje.setConversacion(conversacion);
        when(mensajeRepository.findByIdConConversacion(30L)).thenReturn(Optional.of(mensaje));
        when(usuarioRepository.getReferenceById(madre.getId())).thenReturn(madre);
        when(reaccionRepository.buscar(30L, madre.getId(), "👍")).thenReturn(Optional.empty());

        assertEquals(5L, service.reaccionar(30L, "👍", madre.getId()));
        verify(reaccionRepository).save(any(MensajeReaccion.class));

        MensajeReaccion existente = new MensajeReaccion();
        when(reaccionRepository.buscar(30L, madre.getId(), "👍")).thenReturn(Optional.of(existente));
        service.reaccionar(30L, "👍", madre.getId());
        verify(reaccionRepository).delete(existente);
    }

    @Test
    void reaccionarRechazaTextoQueNoEsUnEmoji() {
        assertThrows(IllegalArgumentException.class, () -> service.reaccionar(30L, "hola", madre.getId()));
        assertThrows(IllegalArgumentException.class, () -> service.reaccionar(30L, " ", madre.getId()));
        verify(reaccionRepository, never()).save(any());
    }

    @Test
    void losContactosDeUnaMadreIncluyenAlPersonalYASusHijosPeroNoAOtrasFamilias() {
        conocer(madre);
        when(usuarioRepository.findActivosByDireccionIdAndRolIn(eq(DIRECCION), any())).thenReturn(List.of(docente));

        List<Long> ids = service.contactos(madre.getId(), DIRECCION, null).stream()
                .map(c -> c.id()).toList();

        assertEquals(List.of(hija.getId(), docente.getId()), ids);
        assertTrue(!ids.contains(otraMadre.getId()) && !ids.contains(estudianteAjeno.getId()));
    }

    private Conversacion conversacion(Long id, Usuario a, Usuario b) {
        Conversacion c = new Conversacion();
        c.setId(id);
        c.setDireccion(direccion);
        ReflectionTestUtils.setField(c, "fechaCreacion", LocalDateTime.now());
        boolean ordenado = a.getId() < b.getId();
        c.setUsuarioA(ordenado ? a : b);
        c.setUsuarioB(ordenado ? b : a);
        return c;
    }
}
