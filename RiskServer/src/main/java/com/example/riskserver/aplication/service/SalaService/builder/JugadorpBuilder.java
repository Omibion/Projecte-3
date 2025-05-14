package com.example.riskserver.aplication.service.SalaService.builder;

import com.example.riskserver.domain.model.Jugadorp;
import com.example.riskserver.domain.model.Partida;
import com.example.riskserver.domain.model.User;

public class JugadorpBuilder {
    public static Jugadorp build(User u, Partida p,String token) {
        Jugadorp j = new Jugadorp();
        j.setUser_id(u.getId());
        j.setPartida(p);
        j.setEstado(false);
        j.setNombre(u.getNom());
        j.setToken(token);
        return j;
    }
}
