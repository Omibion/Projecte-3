package com.example.riskserver.Infrastructure.persistence;

import com.example.riskserver.domain.model.Frontera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FronteraJPARepository   extends JpaRepository<Frontera, Integer> {
    List<Frontera> findAll();
}
