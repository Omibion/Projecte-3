package com.example.riskserver.domain.model;

import jakarta.persistence.*;

@Entity
@IdClass(OkupaId.class)
public class Okupa {

    @Id
    @ManyToOne
    @JoinColumn(name = "jugador_id")
    private Jugadorp jugador;

    @Id
    @ManyToOne
    @JoinColumn(name = "pais_id")
    private Pais pais;

    public Okupa() {}

    public Okupa(Jugadorp jugador, Pais pais) {
        this.jugador = jugador;
        this.pais = pais;
    }

    // Getters y setters
    public Jugadorp getJugador() {
        return jugador;
    }

    public void setJugador(Jugadorp jugador) {
        this.jugador = jugador;
    }

    public Pais getPais() {
        return pais;
    }

    public void setPais(Pais pais) {
        this.pais = pais;
    }
}
