package com.garage.invoice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.invoice-created}")
    private String invoiceCreatedTopic;

    @Value("${kafka.topics.invoice-paid}")
    private String invoicePaidTopic;

    public void publishInvoicePaid(KafkaEvents.InvoicePaidEvent event) {
        log.info("Publishing InvoicePaidEvent invoice={} client={} amount={}",
                event.getInvoiceNumber(), event.getClientId(), event.getAmount());
        kafkaTemplate.send(invoicePaidTopic, event.getInvoiceId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) log.info("InvoicePaidEvent sent: {}", event.getInvoiceNumber());
                    else log.error("Failed to send InvoicePaidEvent", ex);
                });
    }

    public void publishInvoiceCreated(KafkaEvents.InvoiceCreatedEvent event) {
        log.info("Publishing InvoiceCreatedEvent invoice={}", event.getInvoiceNumber());
        kafkaTemplate.send(invoiceCreatedTopic, event.getInvoiceId().toString(), event);
    }
}
