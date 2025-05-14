package com.example.riskserver.aplication.service.LoginService;

import com.example.riskserver.Infrastructure.persistence.UserJpaRepository;
import com.example.riskserver.aplication.dto.LoginRS;
import com.example.riskserver.aplication.port.in.LoginUseCase;
import com.example.riskserver.domain.model.User;
import org.springframework.stereotype.Service;

@Service
public class LoginService implements LoginUseCase {

    private final UserJpaRepository userRepository;

    // Inyección de dependencias mediante constructor
    public LoginService(UserJpaRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public LoginRS login(String username, String password) {
        // Ahora puedes usar el repositorio
        User user = userRepository.findByNom(username); // Asegúrate que este método exista en tu repositorio
        if (user == null) {
            LoginRS loginRS = new LoginRS();
            loginRS.setResponse("loginRS");
            loginRS.setCode(422);
            loginRS.setStatus("Mal");
            return loginRS;
        }
        LoginRS loginRS = new LoginRS();
        loginRS.setResponse("loginRS");
        loginRS.setCode(200);
        loginRS.setStatus("Bien");
        loginRS.setUser(user);
        return loginRS;
    }
}