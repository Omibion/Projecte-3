package com.example.riskserver.aplication.service.SalaService.builder;

import com.example.riskserver.domain.model.Jugadorp;
import com.example.riskserver.domain.model.Partida;
import com.example.riskserver.domain.model.Sala;

import java.util.List;

public class SalaBuilder {
    public static Sala build(Partida p, List<Jugadorp> jugadors){
        Sala sala = new Sala();
        sala.setJugadores(jugadors);
        sala.setId(p.getId());
        sala.setMaxPlayers(p.getMax_players());
        sala.setNombre(p.getNom());
        return sala;
    }
}
