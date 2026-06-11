package com.garage.invoice.kafka;

import com.garage.invoice.service.IDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceKafkaConsumer {

    private final IDashboardService dashboardService;

    @KafkaListener(
        topics = "${kafka.topics.service-scheduled}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onServiceScheduled(@Payload KafkaEvents.ServiceScheduledEvent event, Acknowledgment ack) {
        try {
            log.info("ServiceScheduledEvent received for vehicle {}", event.getVehicleId());
            dashboardService.incrementActiveRepairs();
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing ServiceScheduledEvent: {}", e.getMessage());
            ack.acknowledge();
        }
    }

    @KafkaListener(
        topics = "${kafka.topics.user-registered}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onUserRegistered(@Payload KafkaEvents.UserRegisteredEvent event, Acknowledgment ack) {
        try {
            log.info("UserRegisteredEvent received for user {}", event.getUsername());
            dashboardService.incrementNewClients();
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing UserRegisteredEvent: {}", e.getMessage());
            ack.acknowledge();
        }
    }
}
