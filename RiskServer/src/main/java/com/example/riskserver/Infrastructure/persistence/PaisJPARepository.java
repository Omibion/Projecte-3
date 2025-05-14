package com.example.riskserver.Infrastructure.persistence;

import com.example.riskserver.domain.model.Pais;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaisJPARepository extends JpaRepository<Pais, Integer> {
    public List<Pais> findAll();
}
