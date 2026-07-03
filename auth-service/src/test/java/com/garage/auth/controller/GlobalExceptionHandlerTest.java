package com.garage.auth.controller;

import com.garage.auth.config.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleBadCredentials_returns401() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleBadCredentials(new BadCredentialsException("bad creds"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody()).containsKey("message");
        assertThat(resp.getBody().get("status")).isEqualTo(401);
    }

    @Test
    void handleUserNotFound_returns404() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleUserNotFound(new UsernameNotFoundException("User not found"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody().get("message")).isEqualTo("User not found");
    }

    @Test
    void handleIllegalArgument_returns409() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleIllegalArgument(new IllegalArgumentException("Duplicate email"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody().get("message")).isEqualTo("Duplicate email");
    }

    @Test
    void handleGeneral_returns500() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleGeneral(new RuntimeException("Unexpected failure"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody()).containsKey("timestamp");
        assertThat(resp.getBody().get("status")).isEqualTo(500);
    }

    @Test
    void handleGeneral_bodyContainsRequiredFields() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleGeneral(new Exception("anything"));

        Map<String, Object> body = resp.getBody();
        assertThat(body).containsKeys("timestamp", "status", "error", "message");
        assertThat(body.get("error")).isEqualTo("Internal Server Error");
    }

    @Test
    void handleIllegalArgument_bodyHasNoDetailsKey_whenNullDetails() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleIllegalArgument(new IllegalArgumentException("conflict"));

        assertThat(resp.getBody()).doesNotContainKey("details");
    }
}
