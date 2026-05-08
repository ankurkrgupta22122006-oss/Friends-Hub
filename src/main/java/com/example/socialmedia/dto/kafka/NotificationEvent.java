package com.example.socialmedia.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private String type;          // "LIKE", "COMMENT", "FOLLOW", "MESSAGE", "FOLLOW_REQUEST"
    private Long actorId;         // who did the action
    private Long targetUserId;    // who should be notified
    private Long resourceId;      // postId / commentId etc.
    private String resourceType;  // "POST", "COMMENT", "STORY", "GROUP_MESSAGE"
    private Instant timestamp;
}
