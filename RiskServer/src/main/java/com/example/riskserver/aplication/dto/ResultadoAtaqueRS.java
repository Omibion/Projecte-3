package com.example.riskserver.aplication.dto;

import java.util.List;

public class ResultadoAtaqueRS {
    String response;
    String paisAtacante;
    String paisDefensor;
    int numTropasAtaque;
    int numTropasDefensa;
    int tropasPerdidasAtacante;
    int tropasPerdidasDefensor;
    List<Integer> dadosAtaque;
    List<Integer> dadosDefensa;
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

    public int getNumTropasDefensa() {
        return numTropasDefensa;
    }

    public void setNumTropasDefensa(int numTropasDefensa) {
        this.numTropasDefensa = numTropasDefensa;
    }

    public int getTropasPerdidasAtacante() {
        return tropasPerdidasAtacante;
    }

    public void setTropasPerdidasAtacante(int tropasPerdidasAtacante) {
        this.tropasPerdidasAtacante = tropasPerdidasAtacante;
    }

    public int getTropasPerdidasDefensor() {
        return tropasPerdidasDefensor;
    }

    public void setTropasPerdidasDefensor(int tropasPerdidasDefensor) {
        this.tropasPerdidasDefensor = tropasPerdidasDefensor;
    }

    public List<Integer> getDadosAtaque() {
        return dadosAtaque;
    }

    public void setDadosAtaque(List<Integer> dadosAtaque) {
        this.dadosAtaque = dadosAtaque;
    }

    public List<Integer> getDadosDefensa() {
        return dadosDefensa;
    }

    public void setDadosDefensa(List<Integer> dadosDefensa) {
        this.dadosDefensa = dadosDefensa;
    }
}
