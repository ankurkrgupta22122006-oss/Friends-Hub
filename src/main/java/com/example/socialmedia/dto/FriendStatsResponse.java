package com.example.socialmedia.dto;

import java.util.List;

public class FriendStatsResponse {

    private long totalMessages;
    private long likesGiven;       // requester liked friend's posts
    private long likesReceived;    // friend liked requester's posts
    private long commentsGiven;
    private long commentsReceived;
    private long sharedActivity;   // sum of all cross-interactions
    private List<WeeklyPoint> weeklyMessages;

    public FriendStatsResponse(long totalMessages, long likesGiven, long likesReceived,
                               long commentsGiven, long commentsReceived,
                               List<WeeklyPoint> weeklyMessages) {
        this.totalMessages = totalMessages;
        this.likesGiven = likesGiven;
        this.likesReceived = likesReceived;
        this.commentsGiven = commentsGiven;
        this.commentsReceived = commentsReceived;
        this.sharedActivity = likesGiven + likesReceived + commentsGiven + commentsReceived + totalMessages;
        this.weeklyMessages = weeklyMessages;
    }

    public long getTotalMessages() { return totalMessages; }
    public long getLikesGiven() { return likesGiven; }
    public long getLikesReceived() { return likesReceived; }
    public long getCommentsGiven() { return commentsGiven; }
    public long getCommentsReceived() { return commentsReceived; }
    public long getSharedActivity() { return sharedActivity; }
    public List<WeeklyPoint> getWeeklyMessages() { return weeklyMessages; }

    public static class WeeklyPoint {
        private String day;
        private long count;

        public WeeklyPoint(String day, long count) {
            this.day = day;
            this.count = count;
        }

        public String getDay() { return day; }
        public long getCount() { return count; }
    }
}
