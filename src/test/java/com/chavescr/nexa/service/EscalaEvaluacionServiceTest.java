package com.chavescr.nexa.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chavescr.nexa.entity.DestinoRebajaAsistencia;
import com.chavescr.nexa.entity.QuienRegistraPuntosBoleta;

@ExtendWith(MockitoExtension.class)
class EscalaEvaluacionServiceTest {

    private static final Long DIRECCION = 2L;

    @Mock
    private EscalaNotasService escalaNotasService;

    @Mock
    private RebajaConductaService rebajaConductaService;

    private EscalaEvaluacionService service;

    @BeforeEach
    void setUp() {
        service = new EscalaEvaluacionService(escalaNotasService, rebajaConductaService);
    }

    @Test
    void guardaLaEscalaYLasRebajas() {
        service.guardar(DIRECCION, 0, 100, 1, 70, "[]", "[]",
                QuienRegistraPuntosBoleta.PROFESOR_GUIA, DestinoRebajaAsistencia.CONDUCTA);

        verify(escalaNotasService).guardar(DIRECCION, 0, 100, 1, 70, "[]");
        verify(rebajaConductaService).guardar(DIRECCION, "[]", QuienRegistraPuntosBoleta.PROFESOR_GUIA,
                DestinoRebajaAsistencia.CONDUCTA);
    }

    @Test
    void siLaEscalaNoEsValidaNoGuardaLasRebajas() {
        when(escalaNotasService.guardar(anyLong(), anyInt(), anyInt(), anyInt(), anyInt(), anyString()))
                .thenThrow(new IllegalArgumentException("Hacen falta al menos dos tramos"));

        assertThrows(IllegalArgumentException.class, () -> service.guardar(DIRECCION, 0, 100, 1, 70, "[]", "[]",
                QuienRegistraPuntosBoleta.PROFESOR_GUIA, DestinoRebajaAsistencia.CONDUCTA));

        verifyNoInteractions(rebajaConductaService);
    }

    @Test
    void siLasRebajasNoSonValidasFallaTodoElGuardado() {
        when(rebajaConductaService.guardar(anyLong(), anyString(), any(), any()))
                .thenThrow(new IllegalArgumentException("Indica quién registra los puntos de las boletas"));

        assertThrows(IllegalArgumentException.class, () -> service.guardar(DIRECCION, 0, 100, 1, 70, "[]", "[]",
                null, DestinoRebajaAsistencia.CONDUCTA));
    }
}
