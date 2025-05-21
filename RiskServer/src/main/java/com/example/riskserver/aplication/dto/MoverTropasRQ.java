package com.example.riskserver.aplication.dto;

public class MoverTropasRQ {
    String request;
    String token;
    String paisOrigen;
    String paisDestino;
    int numTropas;

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPaisOrigen() {
        return paisOrigen;
    }

    public void setPaisOrigen(String paisOrigen) {
        this.paisOrigen = paisOrigen;
    }

    public String getPaisDestino() {
        return paisDestino;
    }

    public void setPaisDestino(String paisDestino) {
        this.paisDestino = paisDestino;
    }

    public int getNumTropas() {
        return numTropas;
    }

    public void setNumTropas(int numTropas) {
        this.numTropas = numTropas;
    }
}
