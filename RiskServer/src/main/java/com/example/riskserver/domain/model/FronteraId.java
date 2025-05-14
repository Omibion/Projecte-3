package com.example.riskserver.domain.model;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class FronteraId implements Serializable {

    private Long pais1Id;
    private Long pais2Id;

    public FronteraId() {
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        FronteraId that = (FronteraId) o;
        return Objects.equals(pais1Id, that.pais1Id) && Objects.equals(pais2Id, that.pais2Id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pais1Id, pais2Id);
    }
}
