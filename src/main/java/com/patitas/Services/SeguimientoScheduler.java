package com.patitas.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SeguimientoScheduler {

    private final AdopcionService adopciones;

    /** Todos los días a la 01:00 marca como VENCIDO lo que no tiene evidencia. */
    @Scheduled(cron = "0 0 1 * * *")
    public void revisarVencidos() {
        adopciones.marcarVencidos();
    }
}
