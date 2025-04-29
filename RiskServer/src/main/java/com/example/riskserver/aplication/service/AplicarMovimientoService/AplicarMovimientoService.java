package com.example.riskserver.aplication.service.AplicarMovimientoService;

import com.example.riskserver.aplication.port.in.MovimientoUseCase;
import com.example.riskserver.domain.model.Movimiento;
import org.springframework.stereotype.Service;

    @Service
    public class AplicarMovimientoService implements MovimientoUseCase {

        @Override
        public void aplicarMovimiento(Movimiento movimiento) {
            // lógica del juego: validar, aplicar, actualizar partida, etc.
            System.out.println("Aplicando movimiento: " + movimiento);
        }
    }


