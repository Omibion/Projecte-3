package com.example.riskserver.aplication.dto;

import com.example.riskserver.domain.model.User;

public class CreateSalaRQ {
    String name;
    User user;
    String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
