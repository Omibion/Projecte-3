package com.example.riskserver.aplication.dto;

public class HasConquistadoRQ {
    String response;
    String conquistado;
    String atacante;
    int code;

    public String getAtacante() {
        return atacante;
    }

    public void setAtacante(String atacante) {
        this.atacante = atacante;
    }

    public String getRequest() {
        return response;
    }

    public void setRequest(String request) {
        this.response = request;
    }

    public String getConquistado() {
        return conquistado;
    }

    public void setConquistado(String conquistado) {
        this.conquistado = conquistado;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
