package com.garage.vehicle.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.vehicle-created}")
    private String vehicleCreatedTopic;

    @Value("${kafka.topics.vehicle-updated}")
    private String vehicleUpdatedTopic;

    @Value("${kafka.topics.vehicle-deleted}")
    private String vehicleDeletedTopic;

    @Value("${kafka.topics.service-scheduled}")
    private String serviceScheduledTopic;

    public void publishVehicleCreated(KafkaEvents.VehicleCreatedEvent event) {
        log.info("Publishing VehicleCreatedEvent for vehicle: {}", event.getLicensePlate());
        kafkaTemplate.send(vehicleCreatedTopic, event.getVehicleId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("VehicleCreatedEvent sent: {}", event.getLicensePlate());
                    } else {
                        log.error("Failed to send VehicleCreatedEvent", ex);
                    }
                });
    }

    public void publishVehicleUpdated(KafkaEvents.VehicleUpdatedEvent event) {
        log.info("Publishing VehicleUpdatedEvent for vehicle: {}", event.getLicensePlate());
        kafkaTemplate.send(vehicleUpdatedTopic, event.getVehicleId().toString(), event);
    }

    public void publishVehicleDeleted(KafkaEvents.VehicleDeletedEvent event) {
        log.info("Publishing VehicleDeletedEvent for vehicle: {}", event.getLicensePlate());
        kafkaTemplate.send(vehicleDeletedTopic, event.getVehicleId().toString(), event);
    }

    public void publishServiceScheduled(KafkaEvents.ServiceScheduledEvent event) {
        log.info("Publishing ServiceScheduledEvent for vehicle: {}", event.getLicensePlate());
        kafkaTemplate.send(serviceScheduledTopic, event.getVehicleId().toString(), event);
    }
}
