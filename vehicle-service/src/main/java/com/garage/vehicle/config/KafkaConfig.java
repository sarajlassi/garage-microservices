package com.garage.vehicle.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
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
