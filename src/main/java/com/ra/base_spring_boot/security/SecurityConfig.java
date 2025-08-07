package com.ra.base_spring_boot.security;

import com.ra.base_spring_boot.Configuration.DomainConfig;
import com.ra.base_spring_boot.model.constants.RoleName;
import com.ra.base_spring_boot.repository.IRoleRepository;
import com.ra.base_spring_boot.repository.LoginRepository;
import com.ra.base_spring_boot.security.exception.AccessDenied;
import com.ra.base_spring_boot.security.exception.JwtEntryPoint;
import com.ra.base_spring_boot.security.jwt.JwtProvider;
import com.ra.base_spring_boot.security.jwt.JwtTokenFilter;
import com.ra.base_spring_boot.security.principle.MyUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig
{
    private final MyUserDetailsService userDetailsService;
    private final JwtEntryPoint jwtEntryPoint;
    private final AccessDenied accessDenied;
    private final JwtTokenFilter jwtTokenFilter;
    private final DomainConfig domainConfig;
    private final LoginRepository userRepository;
    private final IRoleRepository roleRepository;
    private final JwtProvider jwtProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception
    {
        return http
                .cors(cf -> cf.configurationSource(request ->
                {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(List.of(domainConfig.getFrontend()));
                    config.setAllowedMethods(List.of("*"));
                    config.setAllowCredentials(true);
                    config.setAllowedHeaders(List.of("*"));
                    config.setExposedHeaders(List.of("*"));
                    return config;
                }))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                        url -> url
                                .requestMatchers("/api/v1/admin/**").hasAuthority(RoleName.ROLE_ADMIN.toString())
                                .requestMatchers("/api/v1/artist/**").hasAnyAuthority(RoleName.ROLE_ARTIST.toString(), RoleName.ROLE_ADMIN.toString())
                                .requestMatchers("/api/v1/user/downloads/file/**").permitAll()
                                .requestMatchers("/api/v1/user/payment/paypal/**").permitAll()
                                .requestMatchers("/api/v1/user/payment/vnpay/**").permitAll()
                                .requestMatchers("/api/v1/user/**").hasAuthority(RoleName.ROLE_USER.toString())
                                .requestMatchers("/api/v1/guest/**").permitAll()
                                .requestMatchers("/test/login/login.html", "/oauth2/**", "/login/**").permitAll()
                                .requestMatchers("/oauth2/**", "/login/**").permitAll()
                                .anyRequest().permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization ->
                                authorization.authorizationRequestRepository(customAuthorizationRequestRepository()))
                        .successHandler((request, response, authentication) -> {
                            // Tạo JWT token và user data ngay tại đây
                            try {
                                org.springframework.security.oauth2.core.user.OAuth2User oauth2User =
                                        (org.springframework.security.oauth2.core.user.OAuth2User) authentication.getPrincipal();

                                String email = oauth2User.getAttribute("email");
                                String facebookId = oauth2User.getAttribute("id");
                                String name = oauth2User.getAttribute("name");
                                String provider = request.getRequestURI().contains("google") ? "google" : "facebook";

                                // Debug log để kiểm tra attributes
                                System.out.println("OAuth2 Provider: " + provider);
                                System.out.println("Email: " + email);
                                System.out.println("Facebook ID: " + facebookId);
                                System.out.println("Name: " + name);
                                System.out.println("All attributes: " + oauth2User.getAttributes());

                                // Với Facebook, nếu không có email, tạo email giả
                                if (provider.equals("facebook") && email == null && facebookId != null) {
                                    email = facebookId + "@facebook.temp";
                                }

                                // Tìm hoặc tạo user
                                com.ra.base_spring_boot.model.User user = null;
                                if (email != null && !email.endsWith("@facebook.temp")) {
                                    user = userRepository.findByEmail(email).orElse(null);
                                }
                                if (user == null && facebookId != null) {
                                    user = userRepository.findByFacebookId(facebookId).orElse(null);
                                }

                                if (user == null) {
                                    // Tạo user mới
                                    com.ra.base_spring_boot.model.Role defaultRole = roleRepository.findById(3L)
                                            .orElseThrow(() -> new RuntimeException("Default role not found"));

                                    user = com.ra.base_spring_boot.model.User.builder()
                                            .email(email != null ? email : (facebookId + "@facebook.temp"))
                                            .facebookId(facebookId)
                                            .firstName(name != null ? name : "")
                                            .lastName("")
                                            .password("")
                                            .status(com.ra.base_spring_boot.model.constants.UserStatus.ACTIVE)
                                            .createdAt(java.time.LocalDateTime.now())
                                            .updatedAt(java.time.LocalDateTime.now())
                                            .roles(java.util.Set.of(defaultRole))
                                            .build();
                                    user = userRepository.save(user);
                                }

                                // Tạo JWT token - sử dụng email hoặc facebookId
                                String tokenSubject = user.getEmail();
                                if (tokenSubject == null || tokenSubject.endsWith("@facebook.temp")) {
                                    tokenSubject = "facebook_" + user.getFacebookId();
                                }
                                String accessToken = jwtProvider.generateToken(tokenSubject);

                                // Tạo URL redirect với thông tin user
                                String redirectUrl = domainConfig.getFrontend() + "/oauth/callback?oauth2=true&provider=" + provider
                                        + "&accessToken=" + java.net.URLEncoder.encode(accessToken, "UTF-8")
                                        + "&userId=" + user.getId()
                                        + "&userName=" + java.net.URLEncoder.encode(user.getFirstName() != null ? user.getFirstName() : "", "UTF-8")
                                        + "&email=" + java.net.URLEncoder.encode(email != null ? email : "", "UTF-8")
                                        + "&facebookId=" + (facebookId != null ? facebookId : "")
                                        + "&needCompleteProfile=" + (user.getFirstName() == null || user.getFirstName().isEmpty());

                                response.sendRedirect(redirectUrl);
                            } catch (Exception e) {
                                e.printStackTrace();
                                response.sendRedirect(domainConfig.getFrontend() + "/oauth/callback?oauth2=false&error=" + e.getMessage());
                            }
                        })
                        .failureHandler((request, response, exception) -> {
                            // Chuyển hướng về frontend với thông tin lỗi
                            try {
                                String errorMessage = exception.getMessage().replaceAll(" ", "_");
                                response.sendRedirect(domainConfig.getFrontend() + "/oauth/callback?oauth2=false&error=" + errorMessage);
                            } catch (Exception e) {
                                response.sendRedirect(domainConfig.getFrontend() + "/oauth/callback?oauth2=false&error=unknown_error");
                            }
                        })
                )
                .authenticationProvider(authenticationProvider())
                // Cho phép session cho OAuth2 login
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .exceptionHandling(
                        exception -> exception
                                .authenticationEntryPoint(jwtEntryPoint)
                                .accessDeniedHandler(accessDenied)
                )
                .addFilterAfter(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder()
    {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider()
    {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(passwordEncoder());
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration auth) throws Exception
    {
        return auth.getAuthenticationManager();
    }

    @Bean
    public AuthorizationRequestRepository<OAuth2AuthorizationRequest> customAuthorizationRequestRepository() {
        return new CustomAuthorizationRequestRepository();
    }

    public static class CustomAuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
        private static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
        private static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";
        private static final int COOKIE_EXPIRE_SECONDS = 180;

        @Override
        public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
            return getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                    .map(this::deserialize)
                    .orElse(null);
        }

        @Override
        public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
            if (authorizationRequest == null) {
                deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
                deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
                return;
            }

            addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, serialize(authorizationRequest), COOKIE_EXPIRE_SECONDS);
            String redirectUriAfterLogin = request.getParameter(REDIRECT_URI_PARAM_COOKIE_NAME);
            if (redirectUriAfterLogin != null && !redirectUriAfterLogin.isBlank()) {
                addCookie(response, REDIRECT_URI_PARAM_COOKIE_NAME, redirectUriAfterLogin, COOKIE_EXPIRE_SECONDS);
            }
        }

        @Override
        public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
            OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
            if (authorizationRequest != null) {
                deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
                deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
            }
            return authorizationRequest;
        }

        private java.util.Optional<String> getCookie(HttpServletRequest request, String name) {
            jakarta.servlet.http.Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (jakarta.servlet.http.Cookie cookie : cookies) {
                    if (name.equals(cookie.getName())) {
                        return java.util.Optional.of(cookie.getValue());
                    }
                }
            }
            return java.util.Optional.empty();
        }

        private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
            jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie(name, value);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setMaxAge(maxAge);
            cookie.setSecure(true); // HTTPS only for production
            cookie.setAttribute("SameSite", "Lax");
            response.addCookie(cookie);
        }

        private void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
            jakarta.servlet.http.Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (jakarta.servlet.http.Cookie cookie : cookies) {
                    if (name.equals(cookie.getName())) {
                        cookie.setValue("");
                        cookie.setPath("/");
                        cookie.setMaxAge(0);
                        response.addCookie(cookie);
                    }
                }
            }
        }

        private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
            try {
                java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                try (java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(bos)) {
                    oos.writeObject(authorizationRequest);
                }
                return java.util.Base64.getEncoder().encodeToString(bos.toByteArray());
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize OAuth2AuthorizationRequest", e);
            }
        }

        private OAuth2AuthorizationRequest deserialize(String serialized) {
            try {
                byte[] data = java.util.Base64.getDecoder().decode(serialized);
                try (java.io.ObjectInputStream ois = new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(data))) {
                    return (OAuth2AuthorizationRequest) ois.readObject();
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize OAuth2AuthorizationRequest", e);
            }
        }
    }
}
