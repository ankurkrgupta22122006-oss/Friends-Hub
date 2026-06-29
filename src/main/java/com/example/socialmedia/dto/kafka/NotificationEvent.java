package com.example.socialmedia.dto.kafka;

import java.time.Instant;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NotificationEvent {
    private String type;          // "LIKE", "COMMENT", "FOLLOW", "MESSAGE", "FOLLOW_REQUEST"
    private Long actorId;         // who did the action
    private Long targetUserId;    // who should be notified
    private Long resourceId;      // postId / commentId etc.
    private String resourceType;  // "POST", "COMMENT", "STORY", "GROUP_MESSAGE"
    private String content;       // text content of the notification
    private Instant timestamp;
public Long getActorId() { return actorId; }
public Long getTargetUserId() { return targetUserId; }
public String getType() { return type; }
public String getContent() { return content; }

public NotificationEvent(String type, Long actorId, Long targetUserId,
                         Long resourceId, String resourceType,
                         String content, Instant timestamp) {
    this.type = type;
    this.actorId = actorId;
    this.targetUserId = targetUserId;
    this.resourceId = resourceId;
    this.resourceType = resourceType;
    this.content = content;
    this.timestamp = timestamp;
}
}


