package com.example.riskserver.aplication.dto;

import com.example.riskserver.domain.model.Sala;

public class JoinSalaRS {
    String response;
    int code;
    Sala sala;

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

    public Sala getSala() {
        return sala;
    }

    public void setSala(Sala sala) {
        this.sala = sala;
    }
}
