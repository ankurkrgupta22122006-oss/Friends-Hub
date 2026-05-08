package com.example.socialmedia.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOffWithMaxRetries;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic notificationsTopic() {
        return TopicBuilder.name("notifications")
                .partitions(3)   // parallel processing by user shard
                .replicas(1)     // increase to 3 in prod
                .build();
    }

    @Bean
    public NewTopic emailTopic() {
        return TopicBuilder.name("email-queue")
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic activityTopic() {
        return TopicBuilder.name("user-activity")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationsDLT() {
        return TopicBuilder.name("notifications.DLT")
            .partitions(1)
            .replicas(1)
            .build();
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        ExponentialBackOffWithMaxRetries backoff = new ExponentialBackOffWithMaxRetries(3);
        backoff.setInitialInterval(1000L);
        backoff.setMultiplier(2.0);
        backoff.setMaxInterval(10000L);
        return new DefaultErrorHandler(backoff);
    }
}

