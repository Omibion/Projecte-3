package com.example.riskserver.domain.repository;

import com.example.riskserver.domain.model.Jugadorp;
import com.example.riskserver.domain.model.Partida;
import com.example.riskserver.domain.model.Sala;

import java.util.List;

public interface SalasRepository {
    public List<Partida> findallbyestado(boolean estado);
    public List<Jugadorp> findallbypartida_id(int partida_id);
}
