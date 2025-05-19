package com.example.riskserver.aplication.dto;

public class TeAtacanRS {
    String response;
    String paisAtacante;
    String paisDefensor;
    String NumTropasAtaque;

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

    public String getNumTropasAtaque() {
        return NumTropasAtaque;
    }

    public void setNumTropasAtaque(String numTropasAtaque) {
        NumTropasAtaque = numTropasAtaque;
    }
}
