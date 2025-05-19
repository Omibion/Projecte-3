package com.example.riskserver.aplication.dto;

public class MeAtacanRQ {
    String request;
    String paisAtacante;
    String paisDefensor;
    int numTropasAtaque;
    int numTropasDefensa;

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

    public int getNumTropasAtaque() {
        return numTropasAtaque;
    }

    public void setNumTropasAtaque(int numTropasAtaque) {
        this.numTropasAtaque = numTropasAtaque;
    }

    public int getNumTropasDefensa() {
        return numTropasDefensa;
    }

    public void setNumTropasDefensa(int numTropasDefensa) {
        this.numTropasDefensa = numTropasDefensa;
    }
}
