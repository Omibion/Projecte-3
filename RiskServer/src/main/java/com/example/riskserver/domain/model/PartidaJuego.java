package com.example.riskserver.domain.model;

import java.util.List;

public class PartidaJuego {
    public List<JugadorJuego> jugadores;
    long turno; //id de jugador
    public Estat fase;

    public List<JugadorJuego> getJugadores() {
        return jugadores;
    }

    public void setJugadores(List<JugadorJuego> jugadores) {
        this.jugadores = jugadores;
    }

    public long getTurno() {
        return turno;
    }

    public void setTurno(long turno) {
        this.turno = turno;
    }

    public Estat getFase() {
        return fase;
    }

    public void setFase(Estat fase) {
        this.fase = fase;
    }
}
