package com.example.riskserver.aplication.dto;

public class AtacarRQ {
    String request;
    String paisAtacante;
    String paisDefensor;
    String token;
    int numTropas;

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getPaisAtacante() {
        return paisAtacante;
    }

    public void setPaisAtacante(String paisAtacante) {
        this.paisAtacante = paisAtacante;
    }

    public String getPaisDefensor() {
        return paisDefensor;
    }

    public void setPaisDefensor(String paisDefensor) {
        this.paisDefensor = paisDefensor;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getNumTropas() {
        return numTropas;
    }

    public void setNumTropas(int numTropas) {
        this.numTropas = numTropas;
    }
}
