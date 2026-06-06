package com.garage.auth.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topics.user-registered}")
    private String userRegisteredTopic;

    @Value("${kafka.topics.user-login}")
    private String userLoginTopic;

    @Value("${kafka.topics.token-validated}")
    private String tokenValidatedTopic;

    @Bean
    public NewTopic userRegisteredTopic() {
        return TopicBuilder.name(userRegisteredTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userLoginTopic() {
        return TopicBuilder.name(userLoginTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic tokenValidatedTopic() {
        return TopicBuilder.name(tokenValidatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
