package com.example.socialmedia.service.kafka;

import com.example.socialmedia.dto.kafka.EmailEvent;
import com.example.socialmedia.dto.kafka.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendNotification(NotificationEvent event) {
        // Key = targetUserId string ensures all notifications
        // for same user go to same partition (ordering guarantee)
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                "notifications",
                String.valueOf(event.getTargetUserId()),
                event
        );

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send notification event: {}", ex.getMessage());
                // TODO: fallback - write to DB directly as backup
            } else {
                log.debug("Notification event sent successfully to partition {}",
                        result.getRecordMetadata().partition());
            }
        });
    }

    public void sendEmail(EmailEvent event) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                "email-queue",
                event.getTo(),
                event
        );

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Email event failed for {}: {}", event.getTo(), ex.getMessage());
            } else {
                log.debug("Email event sent successfully to {}", event.getTo());
            }
        });
    }

    public void sendActivityEvent(Long userId, String action, Long resourceId) {
        var event = Map.of(
                "userId", userId,
                "action", action,       // "POST_VIEW", "STORY_VIEW", "LOGIN"
                "resourceId", resourceId,
                "timestamp", Instant.now()
        );
        kafkaTemplate.send("user-activity", String.valueOf(userId), event);
    }
}
