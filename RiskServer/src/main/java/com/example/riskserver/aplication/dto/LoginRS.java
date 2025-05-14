package com.example.riskserver.aplication.dto;

import com.example.riskserver.domain.model.User;

public class LoginRS {
    private String response;
    private String status;
    private int code;
    private User user;
    private String token;


    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    // Getters y Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getResponse() {return response;}
    public void setResponse(String response) {this.response = response;}

    public User getUser() {return user;}
    public void setUser(User user) {this.user = user;}
}
