package com.garage.vehicle.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VehicleKafkaConsumer {

    /**
     * Listens to user-registered events from auth-service.
     * Can be used to pre-create customer profiles, send welcome notifications, etc.
     */
    @KafkaListener(
            topics = "${kafka.topics.user-registered}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeUserRegistered(KafkaEvents.UserRegisteredEvent event) {
        log.info("Received UserRegisteredEvent - userId: {}, username: {}, role: {}",
                event.getUserId(), event.getUsername(), event.getRole());

        // Business logic: e.g. notify garage management of a new customer,
        // pre-register them in the vehicle management system, etc.
        handleNewUserRegistration(event);
    }

    private void handleNewUserRegistration(KafkaEvents.UserRegisteredEvent event) {
        // Example: log new customers vs staff registrations
        switch (event.getRole()) {
            case "USER" -> log.info("New customer registered: {} ({})", event.getUsername(), event.getEmail());
            case "MECHANIC" -> log.info("New mechanic joined: {} ({})", event.getUsername(), event.getEmail());
            case "ADMIN" -> log.info("New admin created: {}", event.getUsername());
            default -> log.warn("Unknown role {} for user {}", event.getRole(), event.getUsername());
        }
        // Additional logic could include: database sync, notifications, etc.
    }
}
