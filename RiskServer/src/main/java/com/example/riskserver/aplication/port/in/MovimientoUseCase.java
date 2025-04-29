package com.example.riskserver.aplication.port.in;


import com.example.riskserver.domain.model.Movimiento;

public interface MovimientoUseCase {
    void aplicarMovimiento(Movimiento movimiento);
}
