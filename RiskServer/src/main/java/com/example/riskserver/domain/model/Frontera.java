package com.example.riskserver.domain.model;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "frontera")
public class Frontera {
    @EmbeddedId
    private FronteraId id;

    @ManyToOne
    @MapsId("pais1Id")
    @JoinColumn(name = "pais1_id")
    private Pais pais1;

    @ManyToOne
    @MapsId("pais2Id")
    @JoinColumn(name = "pais2_id")
    private Pais pais2;

    public Frontera() {
    }

    public FronteraId getId() {
        return id;
    }

    public void setId(FronteraId id) {
        this.id = id;
    }

    public Pais getPais1() {
        return pais1;
    }

    public void setPais1(Pais pais1) {
        this.pais1 = pais1;
    }

    public Pais getPais2() {
        return pais2;
    }

    public void setPais2(Pais pais2) {
        this.pais2 = pais2;
    }
}
