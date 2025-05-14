package com.example.riskserver.domain.model;

import jakarta.persistence.*;

import java.util.List;

@Entity
public class Pais {
    @Id
    private long id;
    @Column
    private String nom;
    @ManyToOne
            @JoinColumn(name = "continent_id", referencedColumnName = "id")
    Continent continent;

    @Transient
    List<Pais> fronteras;
    public Pais() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Continent getContinent() {
        return continent;
    }

    public void setContinent(Continent continent) {
        this.continent = continent;
    }
}
