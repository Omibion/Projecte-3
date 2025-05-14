package com.example.riskserver.domain.model;

import jakarta.persistence.*;

import java.util.Date;
import java.util.List;
@Entity
public class Partida {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column
    private Date date;
    @Column
    private String nom;
    @Column
    private String token;
    @Column
    private int max_players;
    @Column
    private int admin_id;
    @Column
    private int torn_playes_id;
    @Enumerated(EnumType.STRING)
    Estat estat_torn;
    @Column
    private boolean estado;

    public Partida() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getMax_players() {
        return max_players;
    }

    public void setMax_players(int max_players) {
        this.max_players = max_players;
    }

    public int getAdmin_id() {
        return admin_id;
    }

    public void setAdmin_id(int admin_id) {
        this.admin_id = admin_id;
    }

    public int getTorn_playes_id() {
        return torn_playes_id;
    }

    public void setTorn_playes_id(int torn_playes_id) {
        this.torn_playes_id = torn_playes_id;
    }

    public Estat getEstat_torn() {
        return estat_torn;
    }

    public void setEstat_torn(Estat estat_torn) {
        this.estat_torn = estat_torn;
    }

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }
}
