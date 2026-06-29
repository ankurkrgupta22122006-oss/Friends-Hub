package com.example.socialmedia.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Service
public class PresenceService {

    private final RedisTemplate<String, String> redisTemplate;
    public PresenceService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    private static final String ONLINE_PREFIX = "presence:online:";
    private static final Duration ONLINE_TTL = Duration.ofSeconds(30);

    public void setUserOnline(Long userId) {
        redisTemplate.opsForValue().set(ONLINE_PREFIX + userId, "1", ONLINE_TTL);
    }

    public void setUserOffline(Long userId) {
        redisTemplate.delete(ONLINE_PREFIX + userId);
    }

    public boolean isUserOnline(Long userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(ONLINE_PREFIX + userId));
    }

    public Set<Long> getOnlineUsers(List<Long> userIds) {
    Set<Long> online = new HashSet<>();

    for (Long userId : userIds) {
        if (Boolean.TRUE.equals(
                redisTemplate.hasKey(ONLINE_PREFIX + userId))) {
            online.add(userId);
        }
    }

    return online;
}
}
