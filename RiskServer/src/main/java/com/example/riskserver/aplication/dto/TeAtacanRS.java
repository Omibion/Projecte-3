package com.example.riskserver.aplication.dto;

public class TeAtacanRS {
    String response;
    String paisAtacante;
    String paisDefensor;
    int numTropasAtaque;
    int code;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
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
}
