package com.example.riskserver.aplication.dto;

import com.example.riskserver.domain.model.Jugadorp;
import com.example.riskserver.domain.model.Sala;

import java.util.List;

public class UpdateUserRQ {
    String request;
    Sala sala;
    List<Jugadorp> jugadores;
    String token;

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public Sala getSala() {
        return sala;
    }

    public void setSala(Sala sala) {
        this.sala = sala;
    }

    public List<Jugadorp> getJugadores() {
        return jugadores;
    }

    public void setJugadores(List<Jugadorp> jugadores) {
        this.jugadores = jugadores;
    }
}
