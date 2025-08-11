package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.req.FormLogin;
import com.ra.base_spring_boot.dto.req.FormRegister;
import com.ra.base_spring_boot.dto.resp.JwtResponse;
import com.ra.base_spring_boot.exception.HttpBadRequest;
import com.ra.base_spring_boot.model.EmailVerification;
import com.ra.base_spring_boot.model.Role;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.constants.RoleName;
import com.ra.base_spring_boot.model.constants.UserStatus;
import com.ra.base_spring_boot.repository.IUserRepository;
import com.ra.base_spring_boot.security.jwt.JwtProvider;
import com.ra.base_spring_boot.security.principle.MyUserDetails;
import com.ra.base_spring_boot.services.EmailVerificationService;
import com.ra.base_spring_boot.services.IAuthService;
import com.ra.base_spring_boot.services.IRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final IRoleService roleService;
    private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final EmailVerificationService emailVerificationService;
    private final CloudinaryService cloudinaryService;

    @Override
    public void register(FormRegister formRegister, MultipartFile profilePic) {
        String profilePicUrl;

        if(userRepository.existsByEmail(formRegister.getEmail())) {
            throw new HttpBadRequest("Email already exists");
        }
        if (profilePic != null && !profilePic.isEmpty()) {
            profilePicUrl = cloudinaryService.uploadFile(profilePic, "images");
        } else {
            profilePicUrl = "default.jpg";
        }
        LocalDateTime now = LocalDateTime.now();
        Set<Role> roles = new HashSet<>();
        roles.add(roleService.findByRoleName(RoleName.ROLE_USER));
        User user = User.builder()
                .firstName(formRegister.getFirstName())
                .lastName(formRegister.getLastName())
                .email(formRegister.getEmail())
                .bio(formRegister.getBio())
                .status(UserStatus.VERIFY)
                .roles(roles)
                .profileImage(profilePicUrl)
                .password(passwordEncoder.encode(formRegister.getPassword()))
                .createdAt(now)
                .updatedAt(now)
                .build();
        userRepository.save(user);
        String token = emailVerificationService.generateToken(user);
        emailVerificationService.sendVerificationEmail(user, token);
    }

    @Override
    public JwtResponse login(FormLogin formLogin) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            formLogin.getUsername(), formLogin.getPassword()
                    )
            );
        } catch (AuthenticationException e) {
            throw new HttpBadRequest("email or password is incorrect");
        }

        MyUserDetails userDetails = (MyUserDetails) authentication.getPrincipal();
        if (userDetails.getUser().getStatus() == UserStatus.BLOCKED) {
            throw new HttpBadRequest("Your account is blocked");
        } else if (userDetails.getUser().getStatus() == UserStatus.VERIFY) {
            throw new HttpBadRequest("Account not verified. Check email or resend verification.");
        }
        return JwtResponse.builder()
                .accessToken(jwtProvider.generateToken(userDetails.getUsername()))
                .userId(userDetails.getUser().getId())
                .userName(userDetails.getUsername())
                .fullName(userDetails.getUser().getFirstName() + " " + userDetails.getUser().getLastName())
                .profileImage(userDetails.getUser().getProfileImage())
                .roles(userDetails.getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet()))
                .build();
    }

    @Override
    public void logout(String authHeader) {
        String token = authHeader.substring(7);
        jwtProvider.addToBlackList(token);
    }

    @Override
    public void Activate(String token) {
        EmailVerification emailVerification = emailVerificationService.findByVerificationToken(token);
        if (emailVerification == null || emailVerification.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new HttpBadRequest("Invalid or expired token.");
        }
        User user = emailVerification.getUser();
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new HttpBadRequest("User is already activated.");
        }
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }

    @Override
    public void forgotPassword(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            String token = emailVerificationService.generateToken(user.get());
            emailVerificationService.sendResetPasswordEmail(user.get(), token);
        } else {
            throw new IllegalArgumentException("Email not found.");
        }
    }

    @Override
    public void resetPassword(String token, String password) {
        EmailVerification emailVerification = emailVerificationService.findByVerificationToken(token);
        if (emailVerification == null || emailVerification.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new HttpBadRequest("Invalid or expired token.");
        }

        User user = emailVerification.getUser();
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
    }
    @Override
    public void getTokenRefresh(String userName) {
    User user = userRepository.getByEmail(userName);
    if (user == null) {
        throw new HttpBadRequest("Email not found. Please check or register.");
    }
        String token = emailVerificationService.generateToken(user);
        emailVerificationService.sendVerificationEmail(user, token);

    }
}
