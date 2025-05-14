package com.example.riskserver.aplication.dto;

public class GetSalasRQ {
    public String request;
    Integer userId;
    String token;


    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getRequest() {
        return request;
    }

    public Integer getUserId() {
        return userId;
    }
}
