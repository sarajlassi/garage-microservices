package com.garage.stock.config;

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
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${kafka.topics.product-added}") private String productAddedTopic;
    @Value("${kafka.topics.product-low-stock}") private String productLowStockTopic;
    @Value("${kafka.topics.stock-reserved}") private String stockReservedTopic;
    @Value("${kafka.topics.supplier-order-placed}") private String supplierOrderPlacedTopic;

    @Bean public NewTopic productAddedTopic() { return TopicBuilder.name(productAddedTopic).partitions(1).replicas(1).build(); }
    @Bean public NewTopic productLowStockTopic() { return TopicBuilder.name(productLowStockTopic).partitions(1).replicas(1).build(); }
    @Bean public NewTopic stockReservedTopic() { return TopicBuilder.name(stockReservedTopic).partitions(1).replicas(1).build(); }
    @Bean public NewTopic supplierOrderPlacedTopic() { return TopicBuilder.name(supplierOrderPlacedTopic).partitions(1).replicas(1).build(); }

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
}

