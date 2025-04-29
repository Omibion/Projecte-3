package com.example.riskserver.aplication.controller;

import com.example.riskserver.aplication.dto.LoginRQ;
import com.example.riskserver.aplication.dto.LoginRS;
import com.example.riskserver.application.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/login")
public class LoginController {

    @Autowired
    private LoginService loginService;

    @PostMapping
    public ResponseEntity<LoginRS> login(@RequestBody LoginRQ loginRequest) {
        LoginRS loginResponse = loginService.login(loginRequest.getUsername(), loginRequest.getPassword());

        if (loginResponse.getCode() == 200) {
            return ResponseEntity.ok(loginResponse);  // 200 OK si el login es exitoso
        } else {
            return ResponseEntity.status(loginResponse.getCode()).body(loginResponse);  // 401 si el login falla
        }
    }
}
