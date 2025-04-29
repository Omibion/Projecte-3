package com.example.riskserver.domain.model;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.apache.commons.codec.digest.DigestUtils;

public class User {
    private String username;
    private String passwordHash;

    public boolean verifyPassword(String plainPassword) {
        return DigestUtils.md5Hex(plainPassword).equals(this.passwordHash);
    }
}