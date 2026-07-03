package com.garage.auth.security;

import com.garage.auth.entity.Role;
import com.garage.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION = 86400000L;
    private static final long REFRESH_EXPIRATION = 604800000L;

    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", REFRESH_EXPIRATION);

        user = User.builder()
                .id(1L)
                .username("john")
                .email("john@example.com")
                .password("encoded")
                .role(Role.MECANICIEN)
                .build();
    }

    @Test
    void generateToken_extractUsername_returnsCorrectUsername() {
        String token = jwtService.generateToken(user);
        assertThat(jwtService.extractUsername(token)).isEqualTo("john");
    }

    @Test
    void generateToken_withExtraClaims_isValid() {
        Map<String, Object> claims = Map.of("userId", 1L, "role", "MECANICIEN");
        String token = jwtService.generateToken(claims, user);
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void generateRefreshToken_isValidAndExtractsUsername() {
        String refresh = jwtService.generateRefreshToken(user);
        assertThat(jwtService.extractUsername(refresh)).isEqualTo("john");
        assertThat(jwtService.isTokenValid(refresh, user)).isTrue();
    }

    @Test
    void isTokenValid_wrongUser_returnsFalse() {
        String token = jwtService.generateToken(user);
        User other = User.builder()
                .id(2L).username("jane").email("jane@example.com")
                .password("encoded").role(Role.ADMIN).build();
        assertThat(jwtService.isTokenValid(token, other)).isFalse();
    }

    @Test
    void isTokenExpired_freshToken_returnsFalse() {
        String token = jwtService.generateToken(user);
        assertThat(jwtService.isTokenExpired(token)).isFalse();
    }

    @Test
    void generateToken_noAuthorities_stillGenerates() {
        User noAuthUser = User.builder()
                .id(3L).username("noauth").email("noauth@x.com")
                .password("p").role(Role.FOURNISSEUR).build();
        String token = jwtService.generateToken(noAuthUser);
        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("noauth");
    }

    @Test
    void extractClaim_returnsSubject() {
        String token = jwtService.generateToken(user);
        String subject = jwtService.extractClaim(token,
                io.jsonwebtoken.Claims::getSubject);
        assertThat(subject).isEqualTo("john");
    }
}
