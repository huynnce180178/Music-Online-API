package com.ra.base_spring_boot.services.login;

import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.dto.req.LoginRequestDTO;
import com.ra.base_spring_boot.dto.resp.LoginResponseDTO;
import com.ra.base_spring_boot.dto.resp.UserResponseDTO;
import com.ra.base_spring_boot.model.Role;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.constants.UserStatus;
import com.ra.base_spring_boot.repository.IRoleRepository;
import com.ra.base_spring_boot.repository.LoginRepository;
import com.ra.base_spring_boot.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    private final LoginRepository loginRepository;
    private final IRoleRepository roleRepository;
    private final JwtProvider jwtProvider;

    @Override
    public LoginResponseDTO loginWithGoogle(LoginRequestDTO requestDTO) {
        User user = loginRepository.findByEmail(requestDTO.getEmail()).orElse(null);
        if (user != null) {
            return new LoginResponseDTO("dummy-token", "Login successful");
        } else {
            return new LoginResponseDTO(null, "User not found");
        }
    }

    @Override
    public ResponseEntity<ResponseWrapper> handleLogin(Authentication authentication, HttpSession session) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User)) {
            return ResponseEntity.ok(ResponseWrapper.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .code(400)
                    .data("OAuth2 authentication failed")
                    .build());
        }
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = oauth2User.getAttribute("email");
        String facebookId = oauth2User.getAttribute("id");
        String name = oauth2User.getAttribute("name");
        User user = (email != null) ? loginRepository.findByEmail(email).orElse(null) : null;
        if (user == null && facebookId != null) {
            user = loginRepository.findByFacebookId(facebookId).orElse(null);
        }
        if (user == null) {
            Role defaultRole = roleRepository.findById(3L)
                    .orElseThrow(() -> new RuntimeException("Default role with id=3 not found"));
            user = User.builder()
                    .email(email != null ? email : (facebookId + "@facebook.com"))
                    .facebookId(facebookId)
                    .firstName(name != null ? name : "")
                    .lastName("")
                    .status(UserStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .password("")
                    .roles(Set.of(defaultRole))
                    .build();
            loginRepository.save(user);
        }
        String accessToken = jwtProvider.generateToken(user.getEmail());
        String profileImage = user.getProfileImage();
        final User finalUser = user;
        final String finalProfileImage = (profileImage != null && !profileImage.isEmpty()) ? profileImage : null;
        return ResponseEntity.ok(ResponseWrapper.builder()
                .status(HttpStatus.OK)
                .code(200)
                .data(new java.util.HashMap<String, Object>() {{
                    put("accessToken", accessToken);
                    put("type", "Bearer");
                    put("userId", finalUser.getId());
                    put("userName", finalUser.getFirstName());
                    put("facebookId", finalUser.getFacebookId());
                    put("name", name);
                    put("email", email);
                    put("profileImage", finalProfileImage);
                    put("roles", finalUser.getRoles() != null ? finalUser.getRoles().stream()
                            .map(r -> r.getRoleName().name())
                            .collect(Collectors.toList()) : new java.util.ArrayList<>());
                    put("needCompleteProfile", finalUser.getFirstName() == null || finalUser.getFirstName().isEmpty());
                }})
                .build());
    }


    private UserResponseDTO mapToUserResponseDTO(User user) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return UserResponseDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .profileImage(user.getProfileImage())
                .bio(user.getBio())
                .status(user.getStatus())
                .createdAtStr(user.getCreatedAt() != null ? user.getCreatedAt().format(formatter) : null)
                .updatedAtStr(user.getUpdatedAt() != null ? user.getUpdatedAt().format(formatter) : null)
                .roleNames(user.getRoles() != null
                        ? user.getRoles().stream().map(r -> r.getRoleName().name()).collect(Collectors.toSet())
                        : null)
                .build();
    }
}
