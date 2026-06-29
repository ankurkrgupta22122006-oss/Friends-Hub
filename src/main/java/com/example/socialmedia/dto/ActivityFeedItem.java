package com.example.socialmedia.dto;

public class ActivityFeedItem {

    public enum EventType { POST, NEW_FRIEND }

    private EventType type;
    private String timestamp;

    // Actor (person who did the action)
    private Long actorId;
    private String actorName;
    private String actorPicUrl;

    // Post fields (only for POST events)
    private Long postId;
    private String postContent;
    private String postImageUrl;
    private int likeCount;
    private int commentCount;

    // Target friend (only for NEW_FRIEND events)
    private Long targetId;
    private String targetName;
    private String targetPicUrl;

    private ActivityFeedItem() {}

    public static ActivityFeedItem forPost(Long actorId, String actorName, String actorPicUrl,
                                           Long postId, String postContent, String postImageUrl,
                                           int likeCount, int commentCount, String timestamp) {
        ActivityFeedItem item = new ActivityFeedItem();
        item.type = EventType.POST;
        item.actorId = actorId;
        item.actorName = actorName;
        item.actorPicUrl = actorPicUrl;
        item.postId = postId;
        item.postContent = postContent;
        item.postImageUrl = postImageUrl;
        item.likeCount = likeCount;
        item.commentCount = commentCount;
        item.timestamp = timestamp;
        return item;
    }

    public static ActivityFeedItem forNewFriend(Long actorId, String actorName, String actorPicUrl,
                                                Long targetId, String targetName, String targetPicUrl,
                                                String timestamp) {
        ActivityFeedItem item = new ActivityFeedItem();
        item.type = EventType.NEW_FRIEND;
        item.actorId = actorId;
        item.actorName = actorName;
        item.actorPicUrl = actorPicUrl;
        item.targetId = targetId;
        item.targetName = targetName;
        item.targetPicUrl = targetPicUrl;
        item.timestamp = timestamp;
        return item;
    }

    public EventType getType() { return type; }
    public String getTimestamp() { return timestamp; }
    public Long getActorId() { return actorId; }
    public String getActorName() { return actorName; }
    public String getActorPicUrl() { return actorPicUrl; }
    public Long getPostId() { return postId; }
    public String getPostContent() { return postContent; }
    public String getPostImageUrl() { return postImageUrl; }
    public int getLikeCount() { return likeCount; }
    public int getCommentCount() { return commentCount; }
    public Long getTargetId() { return targetId; }
    public String getTargetName() { return targetName; }
    public String getTargetPicUrl() { return targetPicUrl; }
}
