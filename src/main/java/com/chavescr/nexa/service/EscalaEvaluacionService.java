package com.chavescr.nexa.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.CicloEducativo;
import com.chavescr.nexa.entity.QuienRegistraPuntosBoleta;

/**
 * Guarda juntas la escala de notas de un ciclo y las rebajas de conducta de una dirección, como las edita la pestaña
 * "Escala notas" con un solo botón. Es una sola transacción: si alguna de las dos no es válida, no se guarda
 * ninguna.
 */
@Service
@Transactional
public class EscalaEvaluacionService {

    private final EscalaNotasService escalaNotasService;
    private final RebajaConductaService rebajaConductaService;

    public EscalaEvaluacionService(EscalaNotasService escalaNotasService,
            RebajaConductaService rebajaConductaService) {
        this.escalaNotasService = escalaNotasService;
        this.rebajaConductaService = rebajaConductaService;
    }

    @Transactional(rollbackFor = Exception.class)
    public void guardar(Long direccionId, CicloEducativo ciclo, Integer notaMinima, Integer notaMaxima,
            Integer decimales, Integer notaAprobacion, String tramosJson, String rebajasJson,
            QuienRegistraPuntosBoleta quienRegistraPuntosBoleta, Integer tardiasPorAusencia) {
        escalaNotasService.guardar(direccionId, ciclo, notaMinima, notaMaxima, decimales, notaAprobacion, tramosJson);
        rebajaConductaService.guardar(direccionId, rebajasJson, quienRegistraPuntosBoleta, tardiasPorAusencia);
    }
}
