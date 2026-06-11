package com.garage.auth.service;

import com.garage.auth.dto.AuthDto;
import com.garage.auth.entity.Role;
import com.garage.auth.entity.Token;
import com.garage.auth.entity.TokenType;
import com.garage.auth.entity.User;
import com.garage.auth.kafka.AuthKafkaProducer;
import com.garage.auth.kafka.KafkaEvents;
import com.garage.auth.repository.TokenRepository;
import com.garage.auth.repository.UserRepository;
import com.garage.auth.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuthKafkaProducer kafkaProducer;

    @Transactional
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
        String username = (request.getUsername() != null && !request.getUsername().isBlank())
                ? request.getUsername() : request.getEmail();

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        Role role = request.getRole() != null ? request.getRole() : Role.MECANICIEN;

        User user = User.builder()
                .username(username)
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .company(request.getCompany())
                .role(role)
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered: {}", savedUser.getUsername());

        // Add userId and role to token claims
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", savedUser.getId());
        claims.put("role", savedUser.getRole().name());
        String accessToken = jwtService.generateToken(claims, savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);

        saveUserToken(savedUser, accessToken);

        // Publish Kafka event
        kafkaProducer.publishUserRegistered(KafkaEvents.UserRegisteredEvent.builder()
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .role(savedUser.getRole().name())
                .registeredAt(LocalDateTime.now())
                .build());

        return buildAuthResponse(savedUser, accessToken, refreshToken);
    }

    @Transactional
    public AuthDto.AuthResponse login(AuthDto.LoginRequest request, String ipAddress) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Add userId and role to token claims
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name());
        String accessToken = jwtService.generateToken(claims, user);
        String refreshToken = jwtService.generateRefreshToken(user);

        revokeAllUserTokens(user);
        saveUserToken(user, accessToken);

        // Publish Kafka event
        kafkaProducer.publishUserLogin(KafkaEvents.UserLoginEvent.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .loginAt(LocalDateTime.now())
                .ipAddress(ipAddress)
                .build());

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Transactional
    public AuthDto.AuthResponse refreshToken(HttpServletRequest request) {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String refreshToken = authHeader.substring(7);
        String username = jwtService.extractUsername(refreshToken);

        if (username == null) {
            throw new IllegalArgumentException("Invalid token");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new IllegalArgumentException("Refresh token expired or invalid");
        }

        String newAccessToken = jwtService.generateToken(user);
        revokeAllUserTokens(user);
        saveUserToken(user, newAccessToken);

        return buildAuthResponse(user, newAccessToken, refreshToken);
    }

    public AuthDto.TokenValidationResponse validateToken(AuthDto.TokenValidationRequest request) {
        String token = request.getToken();
        String username;

        try {
            username = jwtService.extractUsername(token);
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return AuthDto.TokenValidationResponse.builder().valid(false).build();
        }

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return AuthDto.TokenValidationResponse.builder().valid(false).build();
        }

        boolean isValid = jwtService.isTokenValid(token, user) &&
                tokenRepository.findByToken(token)
                        .map(t -> !t.isExpired() && !t.isRevoked())
                        .orElse(false);

        // Publish Kafka event
        kafkaProducer.publishTokenValidated(KafkaEvents.TokenValidatedEvent.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .valid(isValid)
                .validatedAt(LocalDateTime.now())
                .build());

        return AuthDto.TokenValidationResponse.builder()
                .valid(isValid)
                .username(isValid ? user.getUsername() : null)
                .role(isValid ? user.getRole().name() : null)
                .userId(isValid ? user.getId() : null)
                .build();
    }

    public List<AuthDto.UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .toList();
    }

    @Transactional
    public AuthDto.UserResponse toggleUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + id));
        user.setEnabled(!user.isEnabled());
        return mapToUserResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    private void saveUserToken(User user, String jwtToken) {
        Token token = Token.builder()
                .user(user)
                .token(jwtToken)
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }

    private void revokeAllUserTokens(User user) {
        List<Token> validTokens = tokenRepository.findAllValidTokensByUser(user.getId());
        if (validTokens.isEmpty()) return;

        validTokens.forEach(t -> {
            t.setExpired(true);
            t.setRevoked(true);
        });
        tokenRepository.saveAll(validTokens);
    }

    private AuthDto.AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthDto.AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    private AuthDto.UserResponse mapToUserResponse(User user) {
        return AuthDto.UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .phone(user.getPhone())
                .company(user.getCompany())
                .active(user.isEnabled())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .build();
    }
}
