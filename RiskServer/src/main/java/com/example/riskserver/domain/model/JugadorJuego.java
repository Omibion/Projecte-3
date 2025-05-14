package com.example.riskserver.domain.model;

import java.util.HashMap;

public class JugadorJuego {
    long id;
    String nombre;
    int totalTropas;
    int tropasTurno;
    HashMap<Pais, Integer> paisesControlados;
    String color;
    String token;

    public JugadorJuego() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getTotalTropas() {
        return totalTropas;
    }

    public void setTotalTropas(int totalTropas) {
        this.totalTropas = totalTropas;
    }

    public int getTropasTurno() {
        return tropasTurno;
    }

    public void setTropasTurno(int tropasTurno) {
        this.tropasTurno = tropasTurno;
    }

    public HashMap<Pais, Integer> getPaisesControlados() {
        return paisesControlados;
    }

    public void setPaisesControlados(HashMap<Pais, Integer> paisesControlados) {
        this.paisesControlados = paisesControlados;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}