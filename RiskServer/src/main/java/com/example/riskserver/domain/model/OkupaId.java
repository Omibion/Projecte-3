package com.example.riskserver.domain.model;

import java.io.Serializable;
import java.util.Objects;

public class OkupaId implements Serializable {
    private Long jugador;
    private Long pais;

    public OkupaId() {}

    public OkupaId(Long jugador, Long pais) {
        this.jugador = jugador;
        this.pais = pais;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OkupaId)) return false;
        OkupaId that = (OkupaId) o;
        return Objects.equals(jugador, that.jugador) &&
                Objects.equals(pais, that.pais);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jugador, pais);
    }
}
