package com.example.riskserver.Infrastructure.persistence;

import com.example.riskserver.domain.model.Okupa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OkupaJPARepository extends JpaRepository<Okupa, Integer> {
    public Okupa findById(int id);
}
