package com.garage.auth.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.user-registered}")
    private String userRegisteredTopic;

    @Value("${kafka.topics.user-login}")
    private String userLoginTopic;

    @Value("${kafka.topics.token-validated}")
    private String tokenValidatedTopic;

    public void publishUserRegistered(KafkaEvents.UserRegisteredEvent event) {
        log.info("Publishing UserRegisteredEvent for user: {}", event.getUsername());
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(userRegisteredTopic, event.getUserId().toString(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("UserRegisteredEvent sent successfully for user: {} | offset: {}",
                        event.getUsername(), result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send UserRegisteredEvent for user: {}", event.getUsername(), ex);
            }
        });
    }

    public void publishUserLogin(KafkaEvents.UserLoginEvent event) {
        log.info("Publishing UserLoginEvent for user: {}", event.getUsername());
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(userLoginTopic, event.getUserId().toString(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("UserLoginEvent sent successfully for user: {}", event.getUsername());
            } else {
                log.error("Failed to send UserLoginEvent for user: {}", event.getUsername(), ex);
            }
        });
    }

    public void publishTokenValidated(KafkaEvents.TokenValidatedEvent event) {
        log.debug("Publishing TokenValidatedEvent for user: {}", event.getUsername());
        kafkaTemplate.send(tokenValidatedTopic, event.getUserId().toString(), event);
    }
}
