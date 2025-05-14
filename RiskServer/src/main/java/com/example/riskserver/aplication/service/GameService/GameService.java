package com.example.riskserver.aplication.service.GameService;

import com.example.riskserver.domain.model.JugadorJuego;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GameService {
    public List orden_turno(List<JugadorJuego> jugadores){
        Collections.shuffle(jugadores);
        return jugadores;
    }

    public int d6(){
        Random rand = new Random();
        return rand.nextInt(6) + 1;
    }
}
