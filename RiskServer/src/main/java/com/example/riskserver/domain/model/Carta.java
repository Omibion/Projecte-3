package com.example.riskserver.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "carta")
public class Carta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TipusCarta tipus;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TipusCarta getTipus() {
        return tipus;
    }

    public void setTipus(TipusCarta tipus) {
        this.tipus = tipus;
    }

    // Getters y setters
}
