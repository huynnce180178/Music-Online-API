package com.ra.base_spring_boot.services.login;

import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.dto.req.LoginRequestDTO;
import com.ra.base_spring_boot.dto.resp.LoginResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import jakarta.servlet.http.HttpSession;

public interface LoginService {
    LoginResponseDTO loginWithGoogle(LoginRequestDTO requestDTO);
    ResponseEntity<ResponseWrapper> handleLogin(Authentication authentication, HttpSession session);
}
