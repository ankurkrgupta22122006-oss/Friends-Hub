package com.example.socialmedia.controller;

import com.example.socialmedia.dto.FriendStatsResponse;
import com.example.socialmedia.service.FriendStatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class FriendStatsController {

    private final FriendStatsService friendStatsService;

    public FriendStatsController(FriendStatsService friendStatsService) {
        this.friendStatsService = friendStatsService;
    }

    @GetMapping("/friend/{userId}")
    public ResponseEntity<FriendStatsResponse> getFriendStats(
            @PathVariable Long userId,
            Authentication authentication) {
        return ResponseEntity.ok(friendStatsService.getStats(userId, authentication.getName()));
    }
}
