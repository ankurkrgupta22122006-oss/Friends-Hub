package com.example.socialmedia.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

@Service
public class TokenBlacklistService {
    private static final Logger log =
        LoggerFactory.getLogger(TokenBlacklistService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    
    public TokenBlacklistService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

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
