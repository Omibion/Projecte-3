package com.example.riskserver.domain.model;

import java.util.List;

public class Sala {
    private Integer id;
    private String nombre;
    private List<Jugadorp> jugadores;
    private int maxPlayers;

    public Sala() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public List<Jugadorp> getJugadores() {
        return jugadores;
    }

    public void setJugadores(List<Jugadorp> jugadores) {
        this.jugadores = jugadores;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }
}
