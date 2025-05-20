package com.example.riskserver.aplication.dto;

public class SaltarTurnoRQ {
    String request;
    int code;
    String token;

    public String getRequest() {
        return request;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
