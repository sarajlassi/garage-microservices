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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private AuthKafkaProducer kafkaProducer;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .username("john")
                .email("john@garage.com")
                .password("encoded")
                .firstName("John")
                .lastName("Doe")
                .phone("0600000000")
                .role(Role.MECANICIEN)
                .build();
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    void register_success_returnsAuthResponse() {
        AuthDto.RegisterRequest req = AuthDto.RegisterRequest.builder()
                .username("john")
                .email("john@garage.com")
                .password("secret123")
                .firstName("John")
                .lastName("Doe")
                .role(Role.MECANICIEN)
                .build();

        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@garage.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtService.generateToken(anyMap(), any(User.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-token");
        when(tokenRepository.save(any(Token.class))).thenReturn(mock(Token.class));
        doNothing().when(kafkaProducer).publishUserRegistered(any(KafkaEvents.UserRegisteredEvent.class));

        AuthDto.AuthResponse response = authService.register(req);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUsername()).isEqualTo("john");
        verify(userRepository).save(any(User.class));
        verify(kafkaProducer).publishUserRegistered(any());
    }

    @Test
    void register_usernameAlreadyExists_throwsIllegalArgument() {
        AuthDto.RegisterRequest req = AuthDto.RegisterRequest.builder()
                .username("john").email("john@garage.com").password("secret123").build();

        when(userRepository.existsByUsername("john")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    void register_emailAlreadyExists_throwsIllegalArgument() {
        AuthDto.RegisterRequest req = AuthDto.RegisterRequest.builder()
                .username("john").email("john@garage.com").password("secret123").build();

        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@garage.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    void register_nullUsername_usesEmailAsUsername() {
        AuthDto.RegisterRequest req = AuthDto.RegisterRequest.builder()
                .email("john@garage.com").password("secret123").build();

        when(userRepository.existsByUsername("john@garage.com")).thenReturn(false);
        when(userRepository.existsByEmail("john@garage.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(sampleUser);
        when(jwtService.generateToken(anyMap(), any())).thenReturn("token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh");
        when(tokenRepository.save(any())).thenReturn(mock(Token.class));

        authService.register(req);

        verify(userRepository).existsByUsername("john@garage.com");
    }

    @Test
    void register_nullRole_defaultsToMecanicien() {
        AuthDto.RegisterRequest req = AuthDto.RegisterRequest.builder()
                .username("john").email("john@garage.com").password("secret123").role(null).build();

        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@garage.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            assertThat(u.getRole()).isEqualTo(Role.MECANICIEN);
            return sampleUser;
        });
        when(jwtService.generateToken(anyMap(), any())).thenReturn("t");
        when(jwtService.generateRefreshToken(any())).thenReturn("r");
        when(tokenRepository.save(any())).thenReturn(mock(Token.class));

        authService.register(req);
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_success_returnsAuthResponse() {
        AuthDto.LoginRequest req = AuthDto.LoginRequest.builder()
                .username("john").password("secret").build();

        when(authenticationManager.authenticate(any())).thenReturn(mock(UsernamePasswordAuthenticationToken.class));
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(sampleUser));
        when(jwtService.generateToken(anyMap(), any())).thenReturn("access");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh");
        when(tokenRepository.findAllValidTokensByUser(1L)).thenReturn(List.of());
        when(tokenRepository.save(any())).thenReturn(mock(Token.class));
        doNothing().when(kafkaProducer).publishUserLogin(any());

        AuthDto.AuthResponse resp = authService.login(req, "127.0.0.1");

        assertThat(resp.getAccessToken()).isEqualTo("access");
        assertThat(resp.getUsername()).isEqualTo("john");
        verify(kafkaProducer).publishUserLogin(any());
    }

    @Test
    void login_userNotFound_throwsUsernameNotFoundException() {
        AuthDto.LoginRequest req = AuthDto.LoginRequest.builder()
                .username("nobody").password("pass").build();

        when(authenticationManager.authenticate(any())).thenReturn(mock(UsernamePasswordAuthenticationToken.class));
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req, "127.0.0.1"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void login_revokesExistingTokens() {
        AuthDto.LoginRequest req = AuthDto.LoginRequest.builder()
                .username("john").password("secret").build();

        Token oldToken = Token.builder().id(10L).token("old").expired(false).revoked(false)
                .tokenType(TokenType.BEARER).user(sampleUser).build();

        when(authenticationManager.authenticate(any())).thenReturn(mock(UsernamePasswordAuthenticationToken.class));
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(sampleUser));
        when(jwtService.generateToken(anyMap(), any())).thenReturn("new-access");
        when(jwtService.generateRefreshToken(any())).thenReturn("new-refresh");
        when(tokenRepository.findAllValidTokensByUser(1L)).thenReturn(List.of(oldToken));
        when(tokenRepository.saveAll(anyList())).thenReturn(List.of(oldToken));
        when(tokenRepository.save(any())).thenReturn(mock(Token.class));

        authService.login(req, "127.0.0.1");

        assertThat(oldToken.isExpired()).isTrue();
        assertThat(oldToken.isRevoked()).isTrue();
        verify(tokenRepository).saveAll(anyList());
    }

    // ── refreshToken ──────────────────────────────────────────────────────────

    @Test
    void refreshToken_missingHeader_throwsIllegalArgument() {
        HttpServletRequest httpReq = mock(HttpServletRequest.class);
        when(httpReq.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        assertThatThrownBy(() -> authService.refreshToken(httpReq))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void refreshToken_headerNotBearer_throwsIllegalArgument() {
        HttpServletRequest httpReq = mock(HttpServletRequest.class);
        when(httpReq.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic abc");

        assertThatThrownBy(() -> authService.refreshToken(httpReq))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refreshToken_success_returnsNewAccessToken() {
        HttpServletRequest httpReq = mock(HttpServletRequest.class);
        when(httpReq.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer old-refresh");
        when(jwtService.extractUsername("old-refresh")).thenReturn("john");
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(sampleUser));
        when(jwtService.isTokenValid("old-refresh", sampleUser)).thenReturn(true);
        when(jwtService.generateToken(anyMap(), any())).thenReturn("new-access");
        when(tokenRepository.findAllValidTokensByUser(1L)).thenReturn(List.of());
        when(tokenRepository.save(any())).thenReturn(mock(Token.class));

        AuthDto.AuthResponse resp = authService.refreshToken(httpReq);

        assertThat(resp.getAccessToken()).isEqualTo("new-access");
    }

    @Test
    void refreshToken_nullUsername_throwsIllegalArgument() {
        HttpServletRequest httpReq = mock(HttpServletRequest.class);
        when(httpReq.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer bad-token");
        when(jwtService.extractUsername("bad-token")).thenReturn(null);

        assertThatThrownBy(() -> authService.refreshToken(httpReq))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid token");
    }

    @Test
    void refreshToken_expiredToken_throwsIllegalArgument() {
        HttpServletRequest httpReq = mock(HttpServletRequest.class);
        when(httpReq.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer expired");
        when(jwtService.extractUsername("expired")).thenReturn("john");
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(sampleUser));
        when(jwtService.isTokenValid("expired", sampleUser)).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken(httpReq))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired or invalid");
    }

    // ── validateToken ─────────────────────────────────────────────────────────

    @Test
    void validateToken_validToken_returnsValid() {
        AuthDto.TokenValidationRequest req = AuthDto.TokenValidationRequest.builder()
                .token("good-token").build();

        Token dbToken = Token.builder().token("good-token").expired(false).revoked(false)
                .tokenType(TokenType.BEARER).user(sampleUser).build();

        when(jwtService.extractUsername("good-token")).thenReturn("john");
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(sampleUser));
        when(jwtService.isTokenValid("good-token", sampleUser)).thenReturn(true);
        when(tokenRepository.findByToken("good-token")).thenReturn(Optional.of(dbToken));
        doNothing().when(kafkaProducer).publishTokenValidated(any());

        AuthDto.TokenValidationResponse resp = authService.validateToken(req);

        assertThat(resp.isValid()).isTrue();
        assertThat(resp.getUsername()).isEqualTo("john");
    }

    @Test
    void validateToken_revokedToken_returnsInvalid() {
        AuthDto.TokenValidationRequest req = AuthDto.TokenValidationRequest.builder()
                .token("revoked-token").build();

        Token dbToken = Token.builder().token("revoked-token").expired(false).revoked(true)
                .tokenType(TokenType.BEARER).user(sampleUser).build();

        when(jwtService.extractUsername("revoked-token")).thenReturn("john");
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(sampleUser));
        when(jwtService.isTokenValid("revoked-token", sampleUser)).thenReturn(true);
        when(tokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(dbToken));
        doNothing().when(kafkaProducer).publishTokenValidated(any());

        AuthDto.TokenValidationResponse resp = authService.validateToken(req);

        assertThat(resp.isValid()).isFalse();
    }

    @Test
    void validateToken_userNotFound_returnsInvalid() {
        AuthDto.TokenValidationRequest req = AuthDto.TokenValidationRequest.builder()
                .token("orphan-token").build();

        when(jwtService.extractUsername("orphan-token")).thenReturn("ghost");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        AuthDto.TokenValidationResponse resp = authService.validateToken(req);

        assertThat(resp.isValid()).isFalse();
    }

    @Test
    void validateToken_jwtParseException_returnsInvalid() {
        AuthDto.TokenValidationRequest req = AuthDto.TokenValidationRequest.builder()
                .token("bad").build();

        when(jwtService.extractUsername("bad")).thenThrow(new RuntimeException("parse error"));

        AuthDto.TokenValidationResponse resp = authService.validateToken(req);

        assertThat(resp.isValid()).isFalse();
    }

    // ── user management ───────────────────────────────────────────────────────

    @Test
    void getAllUsers_returnsMappedList() {
        when(userRepository.findAll()).thenReturn(List.of(sampleUser));

        List<AuthDto.UserResponse> users = authService.getAllUsers();

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getUsername()).isEqualTo("john");
    }

    @Test
    void getSuppliers_returnsOnlyFournisseurs() {
        User supplier = User.builder().id(2L).username("sup").email("sup@g.com")
                .password("p").role(Role.FOURNISSEUR).build();
        when(userRepository.findByRole(Role.FOURNISSEUR)).thenReturn(List.of(supplier));

        List<AuthDto.UserResponse> result = authService.getSuppliers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRole()).isEqualTo("FOURNISSEUR");
    }

    @Test
    void toggleUser_disablesEnabledUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthDto.UserResponse resp = authService.toggleUser(1L);

        assertThat(resp.isActive()).isFalse();
    }

    @Test
    void toggleUser_userNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.toggleUser(99L))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void deleteUser_callsRepository() {
        doNothing().when(userRepository).deleteById(1L);

        authService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }
}
