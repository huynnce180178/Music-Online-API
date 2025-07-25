package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.dto.req.LoginRequestDTO;
import com.ra.base_spring_boot.dto.resp.LoginResponseDTO;
import com.ra.base_spring_boot.repository.EmailVerificationRepository;
import com.ra.base_spring_boot.services.EmailVerificationService;
import com.ra.base_spring_boot.services.login.LoginService;
import com.ra.base_spring_boot.security.principle.MyUserDetails;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Login With Google and Facebook")
@RestController
@RequiredArgsConstructor
public class LoginController {
    private final LoginService loginService;
    private final EmailVerificationService emailVerificationService;
    @Autowired
    private EmailVerificationRepository emailVerificationRepository;
    @PostMapping("/api/v1/login-google")
    public ResponseEntity<ResponseWrapper> loginWithGoogle(@RequestBody LoginRequestDTO requestDTO) {
        LoginResponseDTO response = loginService.loginWithGoogle(requestDTO);
        return ResponseEntity.ok(
            ResponseWrapper.builder()
                .status(HttpStatus.OK)
                .code(200)
                .data(response)
                .build()
        );
    }

    @GetMapping("/google/oauth2-success")
    public ResponseEntity<ResponseWrapper> handleGoogleLogin(Authentication authentication, HttpSession session) {
        return loginService.handleLogin(authentication, session);
    }

    @GetMapping("/facebook/oauth2-success")
    public ResponseEntity<ResponseWrapper> handleFacebookLogin(Authentication authentication, HttpSession session) {
        return loginService.handleLogin(authentication, session);
    }
    @PostMapping("/api/v1/facebook/set-mail")
    public ResponseEntity<ResponseWrapper> setMail(@RequestParam String facebookId, @RequestParam String newEmail) {
        emailVerificationService.setMail(facebookId, newEmail);
        return ResponseEntity.ok(ResponseWrapper.builder()
                .status(HttpStatus.OK)
                .code(200)
                .data("OTP sent to new email")
                .build());
    }

    @PostMapping("/api/v1/facebook/verify-mail")
    public ResponseEntity<ResponseWrapper> verifyMail(@RequestParam String facebookId, @RequestParam String otp) {
        boolean result = emailVerificationService.verifyMail(facebookId, otp);
        if (result) {
            return ResponseEntity.ok(ResponseWrapper.builder()
                    .status(HttpStatus.OK)
                    .code(200)
                    .data("Email verified and updated successfully")
                    .build());
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseWrapper.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .code(400)
                    .data("Invalid OTP or expired")
                    .build());
        }
    }
    @GetMapping("/api/v1/user/has-gmail")
    public ResponseEntity<ResponseWrapper> hasGmail(@AuthenticationPrincipal MyUserDetails userDetails) {
        if (userDetails == null || userDetails.getUser() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ResponseWrapper.builder()
                            .status(HttpStatus.UNAUTHORIZED)
                            .code(401)
                            .data("Unauthorized: Invalid or missing token")
                            .build()
            );
        }
        String email = userDetails.getUser().getEmail();
        boolean hasGmail = email != null && email.toLowerCase().endsWith("@gmail.com");
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .status(HttpStatus.OK)
                        .code(200)
                        .data(hasGmail)
                        .build()
        );
    }
}
