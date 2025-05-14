package com.example.riskserver.aplication.port.in;

import com.example.riskserver.aplication.dto.LoginRS;

public interface LoginUseCase {
    LoginRS login(String username, String password);
}