package com.example.socialmedia.dto;

public class FollowToggleResponse {
    private boolean isFollowing;
    private boolean isFollowRequested;
    private long followerCount;
    private String message;

    public FollowToggleResponse() {
    }

    public FollowToggleResponse(boolean isFollowing, boolean isFollowRequested, long followerCount, String message) {
        this.isFollowing = isFollowing;
        this.isFollowRequested = isFollowRequested;
        this.followerCount = followerCount;
        this.message = message;
    }

    public boolean isFollowing() {
        return isFollowing;
    }

    public void setFollowing(boolean following) {
        isFollowing = following;
    }

    public boolean isFollowRequested() {
        return isFollowRequested;
    }

    public void setFollowRequested(boolean followRequested) {
        isFollowRequested = followRequested;
    }

    public long getFollowerCount() {
        return followerCount;
    }

    public void setFollowerCount(long followerCount) {
        this.followerCount = followerCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
