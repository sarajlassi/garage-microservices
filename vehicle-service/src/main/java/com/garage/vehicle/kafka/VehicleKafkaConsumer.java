package com.garage.vehicle.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VehicleKafkaConsumer {

    @KafkaListener(
            topics = "${kafka.topics.user-registered}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeUserRegistered(@Payload KafkaEvents.UserRegisteredEvent event, Acknowledgment ack) {
        try {
            log.info("Received UserRegisteredEvent - userId: {}, username: {}, role: {}",
                    event.getUserId(), event.getUsername(), event.getRole());
            switch (event.getRole()) {
                case "MECANICIEN" -> log.info("New mechanic joined: {} ({})", event.getUsername(), event.getEmail());
                case "FOURNISSEUR" -> log.info("New supplier joined: {} ({})", event.getUsername(), event.getEmail());
                case "ADMIN" -> log.info("New admin created: {}", event.getUsername());
                default -> log.warn("Unknown role {} for user {}", event.getRole(), event.getUsername());
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing UserRegisteredEvent for userId {}", event.getUserId(), e);
            ack.acknowledge();
        }
    }
}
