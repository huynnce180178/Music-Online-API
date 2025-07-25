package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.dto.req.FormLogin;
import com.ra.base_spring_boot.dto.req.FormRegister;
import com.ra.base_spring_boot.model.EmailVerification;
import com.ra.base_spring_boot.services.EmailVerificationService;
import com.ra.base_spring_boot.services.IAuthService;
import com.ra.base_spring_boot.services.impl.EmailVerificationServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {
    private final IAuthService authService;
    private final EmailVerificationServiceImpl emailVerificationService;

    /**
     * @param formLogin FormLogin
     * @apiNote handle login with { username , password }
     */
    @Operation(summary = "User login", description = "Handle user login with username and password")
    @PostMapping("/login")
    public ResponseEntity<?> handleLogin(@Valid @RequestBody FormLogin formLogin) {
        return ResponseEntity.ok().body(
                ResponseWrapper.builder()
                        .status(HttpStatus.OK)
                        .code(200)
                        .data(authService.login(formLogin))
                        .build()
        );
    }

    /**
     * @param formRegister FormRegister
     * @apiNote handle register with { fullName , username , password }
     */
    @Operation(summary = "User registration", description = "Handle user registration with fullName, username, password and profile picture")
    @PostMapping("/register")
    public ResponseEntity<?> handleRegister(@Valid @ModelAttribute ("formRegister") FormRegister formRegister) {
        System.out.println(formRegister);
        authService.register(formRegister,formRegister.getProfilePic());
        return ResponseEntity.created(URI.create("api/v1/auth/register")).body(
                ResponseWrapper.builder()
                        .status(HttpStatus.CREATED)
                        .code(201)
                        .data("Registration successful. Please check your email to activate your account.")
                        .build()
        );
    }
    @Operation(summary = "User logout", description = "Handle user logout with authorization header")
    @DeleteMapping("/logout")
    public ResponseEntity<?> handleLogout(@RequestHeader("Authorization") String authHeader) {
        authService.logout(authHeader);
        return ResponseEntity.ok(ResponseWrapper.builder()
                .status(HttpStatus.OK)
                .code(200)
                .data("Logout successfully")
                .build());

    }
    @GetMapping("/getTokenRefresh")
    public ResponseEntity<?> getTokenRefresh(@RequestParam String email) {
        authService.getTokenRefresh(email);
        return ResponseEntity.ok(ResponseWrapper.builder()
                .status(HttpStatus.OK)
                .code(200)
                .data("Refresh token successfully")
                .build());
    }
    @Operation(summary = "Account activation", description = "Activate user account with activation token")
    @GetMapping("/activate/{token}")
    public ResponseEntity<?> handleActivate(@PathVariable String token) {
        authService.Activate(token);
        return ResponseEntity.ok(ResponseWrapper.builder()
                .status(HttpStatus.OK)
                .code(200)
                .data("Account activated successfully!")
                .build());
    }
    @Operation(summary = "Forgot password", description = "Send password reset email to the provided email address")
    @PostMapping("/forgot-password")
    public ResponseEntity<ResponseWrapper> forgotPassword(@RequestParam String email) {
        authService.forgotPassword(email);
        return ResponseEntity.ok(ResponseWrapper.builder()
                .status(HttpStatus.OK)
                .code(200)
                .data("Password reset email has been sent.")
                .build());
    }
    @Operation(summary = "Reset password", description = "Reset user password using a reset token and new password")
    @PatchMapping("/reset-password/{token}")
    public ResponseEntity<String> resetPassword(@PathVariable String token, @RequestParam String newPassword) {
        authService.resetPassword(token, newPassword);
        return ResponseEntity.ok("Password has been reset successfully.");
    }
}
