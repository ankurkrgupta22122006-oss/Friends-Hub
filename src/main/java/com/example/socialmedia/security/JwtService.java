package com.example.socialmedia.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    private final RedisTemplate<String, String> redisTemplate;

    public JwtService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void validateJwtSecret() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                "STARTUP FAILED: jwt.secret is not set. Set JWT_SECRET environment variable.");
        }
        if (secretKey.length() < 64) {
            throw new IllegalStateException(
                "STARTUP FAILED: jwt.secret must be at least 64 characters long. " +
                "Generate one with: openssl rand -base64 64");
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", userDetails.getAuthorities().iterator().next().getAuthority());
        return generateToken(claims, userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token) && !isTokenBlacklisted(token);
    }

    /**
     * Check if a token has been blacklisted (revoked on logout).
     * Blacklisted tokens are stored in Redis with expiration time matching token expiry.
     */
    public boolean isTokenBlacklisted(String token) {
        String blacklistKey = "jwt:blacklist:" + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
    }

    /**
     * Add a token to the blacklist when user logs out.
     * Store with TTL equal to token's remaining lifetime to avoid memory leak.
     */
    public void blacklistToken(String token) {
        try {
            long expirationTime = extractExpiration(token).getTime();
            long now = System.currentTimeMillis();
            long remainingTimeMs = Math.max(0, expirationTime - now);

            if (remainingTimeMs > 0) {
                String blacklistKey = "jwt:blacklist:" + token;
                redisTemplate.opsForValue().set(blacklistKey, "revoked", remainingTimeMs, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            // Log but don't fail logout if blacklist operation fails
            org.slf4j.LoggerFactory.getLogger(this.getClass())
                    .warn("Failed to add token to blacklist: {}", e.getMessage());
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
