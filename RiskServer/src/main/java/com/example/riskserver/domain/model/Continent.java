package com.example.riskserver.domain.model;

import jakarta.persistence.*;

import java.util.List;

@Entity
public class Continent {
    @Id
    private int id;
    @Column
    private String nom;
    @Column
    private int reforç;
    @OneToMany(mappedBy = "continent",fetch = FetchType.EAGER)//Es eager per que no hi han gaires paisos per continent

    private List<Pais> paisos;

    public Continent() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public int getReforç() {
        return reforç;
    }

    public void setReforç(int reforç) {
        this.reforç = reforç;
    }

    public List<Pais> getPaisos() {
        return paisos;
    }

    public void setPaisos(List<Pais> paisos) {
        this.paisos = paisos;
    }
}
