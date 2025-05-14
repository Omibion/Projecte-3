package com.example.riskserver.aplication.dto;

import com.example.riskserver.domain.model.Jugadorp;
import com.example.riskserver.domain.model.Sala;

import java.util.List;

public class UpdateSalaRQ {
    String request;
    Sala sala;
    String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Sala getSala() {
        return sala;
    }

    public void setSala(Sala sala) {
        this.sala = sala;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }
}
