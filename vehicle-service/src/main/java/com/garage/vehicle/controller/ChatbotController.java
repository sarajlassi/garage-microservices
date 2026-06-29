package com.garage.vehicle.controller;

import com.garage.vehicle.dto.ChatDto;
import com.garage.vehicle.service.ChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/message")
    @PreAuthorize("hasAnyRole('ADMIN', 'MECANICIEN')")
    public ResponseEntity<ChatDto.ChatResponse> sendMessage(
            @Valid @RequestBody ChatDto.ChatRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mechanicUsername = auth != null ? auth.getName() : "unknown";

        log.info("Chatbot request from mechanic: {}", mechanicUsername);

        String reply = chatbotService.chat(mechanicUsername, request.getMessage());

        return ResponseEntity.ok(ChatDto.ChatResponse.builder().reply(reply).build());
    }
}
