package com.example.riskserver.aplication.dto;

public class LoginRQ {
    private String request;
    private String username;
    private String password;

    // Getters y Setters
    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
