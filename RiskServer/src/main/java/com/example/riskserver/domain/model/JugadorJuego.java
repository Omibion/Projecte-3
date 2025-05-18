package com.example.riskserver.domain.model;

import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

public class JugadorJuego {
    long id;
    String nombre;
    int totalTropas;
    int tropasTurno;
    HashMap<String, Integer> paisesControlados;
    String color;
    String token;
    private Set<Integer> continentesControlados;

    public Set<Integer> getContinentesControlados() {
        return continentesControlados;
    }

    public void setContinentesControlados(Set<Integer> continentesControlados) {
        this.continentesControlados = continentesControlados;
    }

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

    public HashMap<String, Integer> getPaisesControlados() {
        return paisesControlados;
    }

    public void setPaisesControlados(HashMap<String, Integer> paisesControlados) {
        this.paisesControlados = paisesControlados;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        JugadorJuego that = (JugadorJuego) o;
        return id == that.id && totalTropas == that.totalTropas && tropasTurno == that.tropasTurno && Objects.equals(nombre, that.nombre) && Objects.equals(paisesControlados, that.paisesControlados) && Objects.equals(color, that.color) && Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nombre, totalTropas, tropasTurno, paisesControlados, color, token);
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