package com.example.riskserver.aplication.dto;

import com.example.riskserver.domain.model.Sala;

import java.util.List;

public class GetSalasRS {
    String response;
    List<Sala> salas;
    private int code;
    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public List<Sala> getSalas() {
        return salas;
    }

    public void setSalas(List<Sala> salas) {
        this.salas = salas;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
