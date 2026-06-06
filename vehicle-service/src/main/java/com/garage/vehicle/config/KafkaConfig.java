package com.garage.vehicle.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
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

    @Value("${kafka.topics.vehicle-created}")
    private String vehicleCreatedTopic;

    @Value("${kafka.topics.vehicle-updated}")
    private String vehicleUpdatedTopic;

    @Value("${kafka.topics.vehicle-deleted}")
    private String vehicleDeletedTopic;

    @Value("${kafka.topics.service-scheduled}")
    private String serviceScheduledTopic;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
                new ConcurrentKafkaListenerContainerFactory<>();
        
        // Create error handler with retry logic
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                new FixedBackOff(1000, 3) // retry after 1 second, max 3 times
        );
        errorHandler.addNotRetryableExceptions(org.apache.kafka.common.errors.SerializationException.class);
        
        factory.setCommonErrorHandler(errorHandler);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3);
        
        return factory;
    }

    @Bean
    public NewTopic vehicleCreatedTopic() {
        return TopicBuilder.name(vehicleCreatedTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic vehicleUpdatedTopic() {
        return TopicBuilder.name(vehicleUpdatedTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic vehicleDeletedTopic() {
        return TopicBuilder.name(vehicleDeletedTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic serviceScheduledTopic() {
        return TopicBuilder.name(serviceScheduledTopic).partitions(3).replicas(1).build();
    }
}
