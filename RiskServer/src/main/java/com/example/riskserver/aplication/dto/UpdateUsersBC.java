package com.example.riskserver.aplication.dto;

import com.example.riskserver.domain.model.Jugadorp;

import java.util.List;

public class UpdateUsersBC {
    String response;
    int code;
    List<Jugadorp> jugadores;

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public List<Jugadorp> getJugadores() {
        return jugadores;
    }

    public void setJugadores(List<Jugadorp> jugadors) {
        this.jugadores = jugadors;
    }
}
