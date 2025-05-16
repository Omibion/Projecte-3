package com.example.riskserver.aplication.dto;

import com.example.riskserver.domain.model.PartidaJuego;

public class PartidaRS {
    String Response;
    PartidaJuego partida;
    int code;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public PartidaJuego getPartida() {
        return partida;
    }

    public void setPartida(PartidaJuego partida) {
        this.partida = partida;
    }

    public String getResponse() {
        return Response;
    }

    public void setResponse(String response) {
        Response = response;
    }
}
