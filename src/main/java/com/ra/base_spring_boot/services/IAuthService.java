package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.req.FormLogin;
import com.ra.base_spring_boot.dto.req.FormRegister;
import com.ra.base_spring_boot.dto.resp.JwtResponse;
import org.springframework.web.multipart.MultipartFile;

public interface IAuthService
{
    void register(FormRegister formRegister, MultipartFile profilePic);
    JwtResponse login(FormLogin formLogin);
    void logout(String authHeader);
    void Activate(String token);
    void forgotPassword(String email);
    void resetPassword(String token, String password);
    void getTokenRefresh(String userName);

}
