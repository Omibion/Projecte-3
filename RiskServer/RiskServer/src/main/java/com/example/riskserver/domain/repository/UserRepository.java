package com.example.riskserver.domain.repository;

import com.example.riskserver.domain.model.User;

public interface UserRepository {
    User findByUsername(String username);
}