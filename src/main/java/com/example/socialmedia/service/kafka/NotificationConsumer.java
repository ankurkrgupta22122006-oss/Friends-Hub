package com.example.socialmedia.service.kafka;

import com.example.socialmedia.dto.kafka.NotificationEvent;
import com.example.socialmedia.entity.Notification;
import com.example.socialmedia.entity.User;
import com.example.socialmedia.repository.NotificationRepository;
import com.example.socialmedia.repository.UserRepository;
import com.example.socialmedia.service.PresenceService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class NotificationConsumer {
        private static final Logger log =
        LoggerFactory.getLogger(NotificationConsumer.class);

        public NotificationConsumer(
        NotificationRepository notificationRepository,
        UserRepository userRepository,
        SimpMessagingTemplate messagingTemplate,
        PresenceService presenceService) {

    this.notificationRepository = notificationRepository;
    this.userRepository = userRepository;
    this.messagingTemplate = messagingTemplate;
    this.presenceService = presenceService;
}

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final PresenceService presenceService;

    @KafkaListener(
            topics = "notifications",
            groupId = "friendshub-group",
            concurrency = "3"  // 3 threads = process 3 partitions in parallel
    )
    public void handleNotification(NotificationEvent event,
                                   @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            // Validate required fields to prevent NullPointerException crashes
            if (event == null) {
                log.error("Received null notification event, skipping");
                return;
            }
            if (event.getActorId() == null) {
                log.error("Notification event missing actorId, skipping: {}", event);
                return;
            }
            if (event.getTargetUserId() == null) {
                log.error("Notification event missing targetUserId, skipping: {}", event);
                return;
            }
            if (event.getType() == null || event.getType().trim().isEmpty()) {
                log.error("Notification event missing or empty type, skipping: {}", event);
                return;
            }

            // 1. Find users
            User actor = userRepository.findById(event.getActorId())
                    .orElseThrow(() -> new RuntimeException("Actor not found: " + event.getActorId()));
            User targetUser = userRepository.findById(event.getTargetUserId())
                    .orElseThrow(() -> new RuntimeException("Target user not found: " + event.getTargetUserId()));

            // Validate notification type before enum conversion
            try {
                Notification.NotificationType.valueOf(event.getType());
            } catch (IllegalArgumentException e) {
                log.error("Invalid notification type '{}', skipping event", event.getType());
                return;
            }

            // 2. Persist to DB
            Notification saved = notificationRepository.save(
                    new Notification(targetUser, Notification.NotificationType.valueOf(event.getType()),
                            event.getContent() != null ? event.getContent() : "", actor)
            );

            // 3. If user is online, push via WebSocket immediately
            if (presenceService.isUserOnline(event.getTargetUserId())) {
                messagingTemplate.convertAndSendToUser(
                        event.getTargetUserId().toString(),
                        "/queue/notifications",
                        buildNotificationDto(saved)
                );
            }
            // If offline, they'll see it on next login from DB

            log.info("Notification event processed: type={}, actor={}, target={}",
                    event.getType(), event.getActorId(), event.getTargetUserId());

        } catch (RuntimeException e) {
            // Log but don't rethrow for missing users (they may be deleted).
            // This prevents Kafka consumer from getting stuck in infinite retry loop.
            log.warn("Failed to process notification event due to missing entity: {}", e.getMessage());
        } catch (Exception e) {
            // For unexpected errors, log and skip rather than crash the consumer
            log.error("Unexpected error processing notification event: {}", e.getMessage(), e);
        }
    }

    private NotificationDto buildNotificationDto(Notification n) {
        String actorName = "Unknown";
        if (n.getActor() != null && n.getActor().getUserInfo() != null) {
            actorName = n.getActor().getUserInfo().getFirstName() + " " +
                    (n.getActor().getUserInfo().getLastName() != null ?
                            n.getActor().getUserInfo().getLastName() : "");
        }
        return new NotificationDto(
                n.getId(),
                n.getType().name(),
                n.getContent(),
                n.getIsRead(),
                n.getCreatedAt().toString(),
                n.getActor() != null ? n.getActor().getId() : null,
                actorName
        );
    }

    // DTO for WebSocket
    public static class NotificationDto {
        private Long id;
        private String type;
        private String content;
        private Boolean isRead;
        private String createdAt;
        private Long actorId;
        private String actorName;

        public NotificationDto(Long id, String type, String content, Boolean isRead,
                               String createdAt, Long actorId, String actorName) {
            this.id = id;
            this.type = type;
            this.content = content;
            this.isRead = isRead;
            this.createdAt = createdAt;
            this.actorId = actorId;
            this.actorName = actorName;
        }

        public Long getId() { return id; }
        public String getType() { return type; }
        public String getContent() { return content; }
        public Boolean getIsRead() { return isRead; }
        public String getCreatedAt() { return createdAt; }
        public Long getActorId() { return actorId; }
        public String getActorName() { return actorName; }
    }
}
