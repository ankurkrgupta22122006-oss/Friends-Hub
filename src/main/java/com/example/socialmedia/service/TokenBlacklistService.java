package com.example.socialmedia.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    public void blacklistToken(String token, Date expiry) {
        long ttlMillis = expiry.getTime() - System.currentTimeMillis();
        if (ttlMillis > 0) {
            redisTemplate.opsForValue()
                    .set(BLACKLIST_PREFIX + token, "1", Duration.ofMillis(ttlMillis));
            log.debug("Token blacklisted with TTL: {}ms", ttlMillis);
        }
    }

    public boolean isBlacklisted(String token) {
        Boolean hasKey = redisTemplate.hasKey(BLACKLIST_PREFIX + token);
        return Boolean.TRUE.equals(hasKey);
    }

    public void removeTokenFromBlacklist(String token) {
        redisTemplate.delete(BLACKLIST_PREFIX + token);
    }
}
