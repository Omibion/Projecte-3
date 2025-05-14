package com.example.riskserver.domain.model;

import java.util.List;

public class Juego {
    List<JugadorJuego> jugadores;
    int turno;//id del jugador
    Estat fase;

    public List<JugadorJuego> getJugadores() {
        return jugadores;
    }

    public void setJugadores(List<JugadorJuego> jugadores) {
        this.jugadores = jugadores;
    }

    public int getTurno() {
        return turno;
    }

    public void setTurno(int turno) {
        this.turno = turno;
    }

    public Estat getFase() {
        return fase;
    }

    public void setFase(Estat fase) {
        this.fase = fase;
    }
}
