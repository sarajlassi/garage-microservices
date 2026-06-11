package com.garage.auth.kafka;

import com.garage.auth.entity.VehicleOwnerCache;
import com.garage.auth.repository.VehicleOwnerCacheRepository;
import com.garage.auth.service.IClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthKafkaConsumer {

    private final IClientService clientService;
    private final VehicleOwnerCacheRepository vehicleOwnerCacheRepository;

    @KafkaListener(
        topics = "${kafka.topics.vehicle-created}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onVehicleCreated(@Payload KafkaEvents.VehicleCreatedEvent event, Acknowledgment ack) {
        try {
            log.info("Received VehicleCreatedEvent for vehicle {} owner {}", event.getVehicleId(), event.getOwnerId());
            if (event.getOwnerId() != null) {
                vehicleOwnerCacheRepository.save(
                    VehicleOwnerCache.builder()
                        .vehicleId(event.getVehicleId())
                        .clientId(event.getOwnerId())
                        .build()
                );
                clientService.incrementVehicleCount(event.getOwnerId());
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing VehicleCreatedEvent: {}", e.getMessage());
            ack.acknowledge();
        }
    }

    @KafkaListener(
        topics = "${kafka.topics.service-scheduled}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onServiceScheduled(@Payload KafkaEvents.ServiceScheduledEvent event, Acknowledgment ack) {
        try {
            log.info("Received ServiceScheduledEvent for vehicle {}", event.getVehicleId());
            vehicleOwnerCacheRepository.findByVehicleId(event.getVehicleId()).ifPresent(cache -> {
                LocalDateTime visitDate = event.getScheduledAt() != null
                        ? event.getScheduledAt() : LocalDateTime.now();
                clientService.incrementRepairCount(cache.getClientId(), visitDate);
            });
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing ServiceScheduledEvent: {}", e.getMessage());
            ack.acknowledge();
        }
    }

    @KafkaListener(
        topics = "${kafka.topics.invoice-paid}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onInvoicePaid(@Payload KafkaEvents.InvoicePaidEvent event, Acknowledgment ack) {
        try {
            log.info("Received InvoicePaidEvent for client {} amount {}", event.getClientId(), event.getAmount());
            if (event.getClientId() != null && event.getAmount() != null) {
                clientService.addTotalSpent(event.getClientId(), event.getAmount());
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing InvoicePaidEvent: {}", e.getMessage());
            ack.acknowledge();
        }
    }
}
