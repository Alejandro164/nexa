package com.chavescr.nexa.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.chavescr.nexa.service.TareaDetalleService;

/** Cada hora avisa (campanita) de las tareas cuyo recordatorio ya llegó. */
@Configuration
@EnableScheduling
public class RecordatorioTareaJob {

    private static final Logger log = LoggerFactory.getLogger(RecordatorioTareaJob.class);

    private final TareaDetalleService tareaDetalleService;

    public RecordatorioTareaJob(TareaDetalleService tareaDetalleService) {
        this.tareaDetalleService = tareaDetalleService;
    }

    @Scheduled(cron = "0 5 * * * *")
    public void enviarRecordatorios() {
        try {
            tareaDetalleService.enviarRecordatoriosPendientes();
        } catch (RuntimeException e) {
            // Un fallo puntual no debe tumbar el programador: se reintenta en la siguiente hora.
            log.error("No se pudieron enviar los recordatorios de tareas", e);
        }
    }
}
