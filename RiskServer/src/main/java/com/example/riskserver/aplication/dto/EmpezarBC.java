package com.example.riskserver.aplication.dto;

import com.example.riskserver.domain.model.Partida;

public class EmpezarBC {
    String response;
    int code;
    Partida p;

    public String getResponse() {
        return response;
    }

    public void setResponse(String responsee) {
        this.response = responsee;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
