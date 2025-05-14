package com.example.riskserver.domain.model;

import jakarta.persistence.*;

import java.util.List;

@Entity
public class Ma {
    @Id
    int id;
    @OneToOne
    @JoinColumn(name = "jugador_id", referencedColumnName = "id")
    Jugadorp jugador;
    @OneToMany
    @JoinColumn(name = "carta_id", referencedColumnName = "id")
    List<Carta> cartas;

    public Ma() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Jugadorp getJugador() {
        return jugador;
    }

    public void setJugador(Jugadorp jugador) {
        this.jugador = jugador;
    }

    public List<Carta> getCartas() {
        return cartas;
    }

    public void setCartas(List<Carta> cartas) {
        this.cartas = cartas;
    }
}
