package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.dto.req.LoginRequestDTO;
import com.ra.base_spring_boot.dto.resp.LoginResponseDTO;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.repository.EmailVerificationRepository;
import com.ra.base_spring_boot.repository.LoginRepository;
import com.ra.base_spring_boot.security.jwt.JwtProvider;
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

import java.util.Map;

@Tag(name = "Login With Google and Facebook")
@RestController
@RequiredArgsConstructor
public class LoginController {
    private final LoginService loginService;
    private final EmailVerificationService emailVerificationService;
    @Autowired
    private EmailVerificationRepository emailVerificationRepository;
    @Autowired
    private LoginRepository loginRepository;
    @Autowired
    private JwtProvider jwtProvider;

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
    @PostMapping("/api/v1/oauth2/verify-token")
    public ResponseEntity<ResponseWrapper> verifyOAuth2Token(@RequestBody Map<String, String> request) {
        try {
            String accessToken = request.get("accessToken");
            String provider = request.get("provider");
            String email = request.get("email");

            // Validate input
            if (accessToken == null || accessToken.trim().isEmpty()) {
                return ResponseEntity.ok(ResponseWrapper.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .code(400)
                        .data("Access token is required")
                        .build());
            }

            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.ok(ResponseWrapper.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .code(400)
                        .data("Email is required")
                        .build());
            }

            String emailFromToken;
            try {
                emailFromToken = jwtProvider.extractUsername(accessToken);
            } catch (Exception e) {
                return ResponseEntity.ok(ResponseWrapper.builder()
                        .status(HttpStatus.UNAUTHORIZED)
                        .code(401)
                        .data("Invalid or expired token")
                        .build());
            }

            try {
                if (jwtProvider.isTokenExpired(accessToken)) {
                    return ResponseEntity.ok(ResponseWrapper.builder()
                            .status(HttpStatus.UNAUTHORIZED)
                            .code(401)
                            .data("Token has expired")
                            .build());
                }
            } catch (Exception e) {
                return ResponseEntity.ok(ResponseWrapper.builder()
                        .status(HttpStatus.UNAUTHORIZED)
                        .code(401)
                        .data("Invalid token format")
                        .build());
            }

            // Verify email match
            if (emailFromToken == null || !email.equals(emailFromToken)) {
                return ResponseEntity.ok(ResponseWrapper.builder()
                        .status(HttpStatus.UNAUTHORIZED)
                        .code(401)
                        .data("Token email mismatch")
                        .build());
            }

            // Tìm user trong database
            User user = loginRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return ResponseEntity.ok(ResponseWrapper.builder()
                        .status(HttpStatus.NOT_FOUND)
                        .code(404)
                        .data("User not found")
                        .build());
            }

            // Kiểm tra user status
            if (user.getStatus() == null || !user.getStatus().name().equals("ACTIVE")) {
                return ResponseEntity.ok(ResponseWrapper.builder()
                        .status(HttpStatus.FORBIDDEN)
                        .code(403)
                        .data("User account is not active")
                        .build());
            }

            // Chuẩn bị response data
            Map<String, Object> responseData = new java.util.HashMap<>();
            responseData.put("accessToken", accessToken);
            responseData.put("type", "Bearer");
            responseData.put("userId", user.getId());
            responseData.put("userName", user.getFirstName() != null ? user.getFirstName() : "");
            responseData.put("email", user.getEmail());
            responseData.put("facebookId", user.getFacebookId());
            responseData.put("profileImage", user.getProfileImage());

            // Lấy roles
            java.util.List<String> roleNames = new java.util.ArrayList<>();
            if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                roleNames = user.getRoles().stream()
                        .map(role -> role.getRoleName().name())
                        .collect(java.util.stream.Collectors.toList());
            }
            responseData.put("roles", roleNames);

            // Kiểm tra profile completion
            boolean needCompleteProfile = (user.getFirstName() == null || user.getFirstName().trim().isEmpty()) ||
                    (user.getLastName() == null || user.getLastName().trim().isEmpty());
            responseData.put("needCompleteProfile", needCompleteProfile);
            responseData.put("provider", provider != null ? provider : "unknown");

            return ResponseEntity.ok(ResponseWrapper.builder()
                    .status(HttpStatus.OK)
                    .code(200)
                    .data(responseData)
                    .message("OAuth2 token verified successfully")
                    .build());

        } catch (Exception e) {
            // Log error for debugging
            System.err.println("Error verifying OAuth2 token: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.ok(ResponseWrapper.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .code(500)
                    .data("Error verifying OAuth2 token")
                    .message(e.getMessage())
                    .build());
        }
    }
}
