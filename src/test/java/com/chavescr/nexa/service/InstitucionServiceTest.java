package com.chavescr.nexa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.entity.OfertaEducativa;
import com.chavescr.nexa.repository.DireccionRepository;
import com.chavescr.nexa.repository.InstitucionRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class InstitucionServiceTest {

    @Mock
    private InstitucionRepository institucionRepository;

    @Mock
    private DireccionRepository direccionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private NivelAcademicoRepository nivelAcademicoRepository;

    private InstitucionService service;

    @BeforeEach
    void setUp() {
        service = new InstitucionService(institucionRepository, direccionRepository, usuarioRepository,
                nivelAcademicoRepository);
    }

    @Test
    void laDireccionMarcadaQuedaComoEntradaYLasDemasNo() {
        Institucion institucion = institucionGuardada();
        List<Direccion> guardadas = capturarDirecciones();

        service.guardar(null, null, "Escuela Central", null, null, null, true,
                List.of("PREESCOLAR", "PRIMARIA"),
                Map.of("PREESCOLAR", "100", "PRIMARIA", "200"),
                "PRIMARIA");

        assertEquals(OfertaEducativa.PRIMARIA, unicaEntrada(guardadas).getOferta());
        assertEquals(institucion.getId(), unicaEntrada(guardadas).getInstitucion().getId());
    }

    @Test
    void sinMarcaLaPrimeraDireccionElegidaEsLaEntrada() {
        institucionGuardada();
        List<Direccion> guardadas = capturarDirecciones();

        service.guardar(null, null, "Escuela Central", null, null, null, true,
                List.of("SECUNDARIA"),
                Map.of("SECUNDARIA", "300"),
                null);

        assertEquals(OfertaEducativa.SECUNDARIA, unicaEntrada(guardadas).getOferta());
    }

    @Test
    void laEntradaTieneQueEstarEntreLasDireccionesMarcadas() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.guardar(null, null, "Escuela Central", null, null, null, true,
                        List.of("PRIMARIA"),
                        Map.of("PRIMARIA", "200"),
                        "SECUNDARIA"));

        assertEquals("La dirección de entrada tiene que ser una de las direcciones marcadas.", error.getMessage());
    }

    @Test
    void alQuitarLaEntradaLaDireccionQueQuedaPasaASerla() {
        Institucion institucion = new Institucion();
        institucion.setId(1L);
        when(institucionRepository.findById(1L)).thenReturn(Optional.of(institucion));
        when(institucionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Direccion preescolar = direccion(4L, OfertaEducativa.PREESCOLAR, true);
        Direccion primaria = direccion(5L, OfertaEducativa.PRIMARIA, false);
        when(direccionRepository.findByInstitucionId(1L)).thenReturn(List.of(preescolar, primaria));
        when(direccionRepository.findByCodigo(any())).thenReturn(Optional.empty());
        when(direccionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(usuarioRepository.countByDireccionId(anyLong())).thenReturn(0L);
        when(nivelAcademicoRepository.existsByDireccionId(anyLong())).thenReturn(false);

        service.guardar(1L, null, "Escuela Central", null, null, null, true,
                List.of("PRIMARIA"),
                Map.of("PRIMARIA", "200"),
                null);

        assertTrue(primaria.isPrincipal());
        verify(direccionRepository).delete(preescolar);
    }

    @Test
    void alListarMarcaEntradaSiNingunaLoEstaba() {
        Institucion institucion = new Institucion();
        institucion.setNombre("Escuela Central");
        Direccion secundaria = direccion(8L, OfertaEducativa.SECUNDARIA, false);
        Direccion preescolar = direccion(7L, OfertaEducativa.PREESCOLAR, false);
        institucion.setDirecciones(new LinkedHashSet<>(List.of(secundaria, preescolar)));
        when(direccionRepository.existsByInstitucionIsNull()).thenReturn(false);
        when(institucionRepository.findAllConDirecciones()).thenReturn(List.of(institucion));

        service.listar();

        assertTrue(preescolar.isPrincipal());
        assertFalse(secundaria.isPrincipal());
        verify(direccionRepository).save(preescolar);
        verify(direccionRepository, never()).save(secundaria);
    }

    private Institucion institucionGuardada() {
        when(institucionRepository.save(any())).thenAnswer(invocation -> {
            Institucion guardada = invocation.getArgument(0);
            guardada.setId(1L);
            return guardada;
        });
        when(direccionRepository.findByInstitucionId(1L)).thenReturn(List.of());
        when(direccionRepository.findByCodigo(any())).thenReturn(Optional.empty());
        Institucion institucion = new Institucion();
        institucion.setId(1L);
        return institucion;
    }

    private List<Direccion> capturarDirecciones() {
        List<Direccion> guardadas = new ArrayList<>();
        AtomicLong ids = new AtomicLong(10);
        when(direccionRepository.save(any())).thenAnswer(invocation -> {
            Direccion direccion = invocation.getArgument(0);
            if (direccion.getId() == null) {
                direccion.setId(ids.incrementAndGet());
            }
            guardadas.removeIf(existente -> existente.getId().equals(direccion.getId()));
            guardadas.add(direccion);
            return direccion;
        });
        return guardadas;
    }

    private static Direccion unicaEntrada(List<Direccion> direcciones) {
        List<Direccion> entradas = direcciones.stream().filter(Direccion::isPrincipal).toList();
        assertEquals(1, entradas.size());
        return entradas.get(0);
    }

    private static Direccion direccion(Long id, OfertaEducativa oferta, boolean principal) {
        Direccion direccion = new Direccion();
        direccion.setId(id);
        direccion.setOferta(oferta);
        direccion.setPrincipal(principal);
        direccion.setCodigo(oferta.name());
        return direccion;
    }
}
