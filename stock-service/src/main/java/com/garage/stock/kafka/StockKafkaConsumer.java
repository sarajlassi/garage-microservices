package com.garage.stock.kafka;

import com.garage.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockKafkaConsumer {

    private final StockService stockService;

    /**
     * Listens to service-scheduled events from vehicle-service.
     * When a service is scheduled, we need to:
     * 1. Determine what parts/products are needed
     * 2. Reserve stock if available
     * 3. Alert if stock is low or unavailable
     */
    @KafkaListener(
            topics = "${kafka.topics.service-scheduled}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeServiceScheduled(KafkaEvents.ServiceScheduledEvent event) {
        log.info("Received ServiceScheduledEvent - serviceRecordId: {}, vehicleId: {}, serviceType: {}",
                event.getServiceRecordId(), event.getVehicleId(), event.getServiceType());

        try {
            // Handle stock allocation based on service type
            handleServiceScheduled(event);
        } catch (Exception e) {
            log.error("Error processing ServiceScheduledEvent", e);
        }
    }

    private void handleServiceScheduled(KafkaEvents.ServiceScheduledEvent event) {
        // Map service type to required products
        switch (event.getServiceType()) {
            case "OIL_CHANGE" -> {
                log.info("Service OIL_CHANGE scheduled for vehicle: {}", event.getLicensePlate());
                // Check and reserve oil stock
                stockService.checkAndAlertLowStock("OIL");
            }
            case "TIRE_ROTATION" -> {
                log.info("Service TIRE_ROTATION scheduled for vehicle: {}", event.getLicensePlate());
                // Check tire-related products
                stockService.checkAndAlertLowStock("TIRE");
            }
            case "BRAKE_SERVICE" -> {
                log.info("Service BRAKE_SERVICE scheduled for vehicle: {}", event.getLicensePlate());
                // Check brake pads, fluid, etc.
                stockService.checkAndAlertLowStock("BRAKE_PAD");
                stockService.checkAndAlertLowStock("BRAKE_FLUID");
            }
            case "BATTERY_REPLACEMENT" -> {
                log.info("Service BATTERY_REPLACEMENT scheduled for vehicle: {}", event.getLicensePlate());
                // Check battery stock
                stockService.checkAndAlertLowStock("BATTERY");
            }
            case "COOLANT_FLUSH" -> {
                log.info("Service COOLANT_FLUSH scheduled for vehicle: {}", event.getLicensePlate());
                // Check coolant stock
                stockService.checkAndAlertLowStock("COOLANT");
            }
            case "AIR_FILTER_REPLACEMENT" -> {
                log.info("Service AIR_FILTER_REPLACEMENT scheduled for vehicle: {}", event.getLicensePlate());
                // Check air filter stock
                stockService.checkAndAlertLowStock("AIR_FILTER");
            }
            default -> log.info("Service type {} registered for vehicle {}", 
                    event.getServiceType(), event.getLicensePlate());
        }
    }
}

