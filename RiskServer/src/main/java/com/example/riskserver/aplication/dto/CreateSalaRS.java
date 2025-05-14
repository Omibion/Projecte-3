package com.example.riskserver.aplication.dto;

import com.example.riskserver.domain.model.Jugadorp;
import com.example.riskserver.domain.model.Sala;

public class CreateSalaRS {
    String response;
    Sala sala;
    private int code;
    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public Sala getSala() {
        return sala;
    }

    public void setSala(Sala sala) {
        this.sala = sala;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
