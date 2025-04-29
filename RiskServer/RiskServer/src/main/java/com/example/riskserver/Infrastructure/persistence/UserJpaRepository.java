package com.example.riskserver.Infrastructure.persistence;

import com.example.riskserver.domain.model.User;
import com.example.riskserver.domain.repository.UserRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserJpaRepository extends JpaRepository<User, Long>, UserRepository {
    User findByUsername(String username);
}
