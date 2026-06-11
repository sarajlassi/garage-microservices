package com.garage.stock.kafka;

import com.garage.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockKafkaConsumer {

    private final StockService stockService;

    @KafkaListener(
            topics = "${kafka.topics.service-scheduled}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeServiceScheduled(@Payload KafkaEvents.ServiceScheduledEvent event, Acknowledgment ack) {
        try {
            log.info("Received ServiceScheduledEvent - serviceRecordId: {}, vehicleId: {}, serviceType: {}",
                    event.getServiceRecordId(), event.getVehicleId(), event.getServiceType());
            handleServiceScheduled(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing ServiceScheduledEvent for serviceRecordId {}", event.getServiceRecordId(), e);
            ack.acknowledge();
        }
    }

    private void handleServiceScheduled(KafkaEvents.ServiceScheduledEvent event) {
        switch (event.getServiceType()) {
            case "OIL_CHANGE" -> {
                log.info("Service OIL_CHANGE scheduled for vehicle: {}", event.getLicensePlate());
                stockService.checkAndAlertLowStock("OIL");
            }
            case "TIRE_ROTATION" -> {
                log.info("Service TIRE_ROTATION scheduled for vehicle: {}", event.getLicensePlate());
                stockService.checkAndAlertLowStock("TIRE");
            }
            case "BRAKE_SERVICE" -> {
                log.info("Service BRAKE_SERVICE scheduled for vehicle: {}", event.getLicensePlate());
                stockService.checkAndAlertLowStock("BRAKE_PAD");
                stockService.checkAndAlertLowStock("BRAKE_FLUID");
            }
            case "BATTERY_REPLACEMENT" -> {
                log.info("Service BATTERY_REPLACEMENT scheduled for vehicle: {}", event.getLicensePlate());
                stockService.checkAndAlertLowStock("BATTERY");
            }
            case "COOLANT_FLUSH" -> {
                log.info("Service COOLANT_FLUSH scheduled for vehicle: {}", event.getLicensePlate());
                stockService.checkAndAlertLowStock("COOLANT");
            }
            case "AIR_FILTER_REPLACEMENT" -> {
                log.info("Service AIR_FILTER_REPLACEMENT scheduled for vehicle: {}", event.getLicensePlate());
                stockService.checkAndAlertLowStock("AIR_FILTER");
            }
            default -> log.info("Service type {} registered for vehicle {}",
                    event.getServiceType(), event.getLicensePlate());
        }
    }
}
