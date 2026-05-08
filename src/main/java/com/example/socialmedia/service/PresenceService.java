package com.example.socialmedia.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PresenceService {

    private final RedisTemplate<String, Object> redisTemplate;
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
        List<String> keys = userIds.stream()
            .map(id -> ONLINE_PREFIX + id)
            .toList();
        List<Object> results = redisTemplate.opsForValue().multiGet(keys);
        Set<Long> online = new HashSet<>();
        if (results == null) return online;
        for (int i = 0; i < userIds.size(); i++) {
            if (results.get(i) != null) online.add(userIds.get(i));
        }
        return online;
    }
}
