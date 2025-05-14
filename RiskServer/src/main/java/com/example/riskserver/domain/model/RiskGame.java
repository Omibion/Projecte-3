package com.example.riskserver.domain.model;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RiskGame {
    public List orden_turno(List<JugadorJuego> jugadores){
        Collections.shuffle(jugadores);
        return jugadores;
    }

    public int d6(){
        Random rand = new Random();
        return rand.nextInt(6) + 1;
    }
}
