package com.garage.invoice.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.errors.SerializationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${kafka.topics.invoice-created}")
    private String invoiceCreatedTopic;

    @Value("${kafka.topics.invoice-paid}")
    private String invoicePaidTopic;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(new FixedBackOff(1000, 3));
        errorHandler.addNotRetryableExceptions(SerializationException.class);
        factory.setCommonErrorHandler(errorHandler);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3);
        return factory;
    }

    @Bean
    public NewTopic invoiceCreatedTopic() {
        return TopicBuilder.name(invoiceCreatedTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic invoicePaidTopic() {
        return TopicBuilder.name(invoicePaidTopic).partitions(3).replicas(1).build();
    }
}
