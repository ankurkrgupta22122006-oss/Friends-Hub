package com.example.socialmedia.dto;

import java.util.List;

public class MilestoneResponse {

    private long daysConnected;
    private String friendsSince;        // ISO date string of the earliest follow
    private boolean isMutual;           // both follow each other
    private List<Badge> badges;
    private List<TimelineEvent> timeline;
    private String nextMilestone;       // label of the next upcoming badge
    private long daysToNextMilestone;

    public MilestoneResponse(long daysConnected, String friendsSince, boolean isMutual,
                             List<Badge> badges, List<TimelineEvent> timeline,
                             String nextMilestone, long daysToNextMilestone) {
        this.daysConnected = daysConnected;
        this.friendsSince = friendsSince;
        this.isMutual = isMutual;
        this.badges = badges;
        this.timeline = timeline;
        this.nextMilestone = nextMilestone;
        this.daysToNextMilestone = daysToNextMilestone;
    }

    public long getDaysConnected() { return daysConnected; }
    public String getFriendsSince() { return friendsSince; }
    public boolean isMutual() { return isMutual; }
    public List<Badge> getBadges() { return badges; }
    public List<TimelineEvent> getTimeline() { return timeline; }
    public String getNextMilestone() { return nextMilestone; }
    public long getDaysToNextMilestone() { return daysToNextMilestone; }

    public static class Badge {
        private String id;
        private String label;
        private String emoji;
        private String description;
        private boolean unlocked;
        private String unlockedAt;   // date string, null if locked

        public Badge(String id, String label, String emoji, String description,
                     boolean unlocked, String unlockedAt) {
            this.id = id;
            this.label = label;
            this.emoji = emoji;
            this.description = description;
            this.unlocked = unlocked;
            this.unlockedAt = unlockedAt;
        }

        public String getId() { return id; }
        public String getLabel() { return label; }
        public String getEmoji() { return emoji; }
        public String getDescription() { return description; }
        public boolean isUnlocked() { return unlocked; }
        public String getUnlockedAt() { return unlockedAt; }
    }

    public static class TimelineEvent {
        private String date;
        private String label;
        private String emoji;

        public TimelineEvent(String date, String label, String emoji) {
            this.date = date;
            this.label = label;
            this.emoji = emoji;
        }

        public String getDate() { return date; }
        public String getLabel() { return label; }
        public String getEmoji() { return emoji; }
    }
}
