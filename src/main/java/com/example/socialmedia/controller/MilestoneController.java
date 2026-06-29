package com.example.socialmedia.controller;

import com.example.socialmedia.dto.MilestoneResponse;
import com.example.socialmedia.service.MilestoneService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/milestones")
public class MilestoneController {

    private final MilestoneService milestoneService;

    public MilestoneController(MilestoneService milestoneService) {
        this.milestoneService = milestoneService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<MilestoneResponse> getMilestones(
            @PathVariable Long userId,
            Authentication authentication) {
        return ResponseEntity.ok(milestoneService.getMilestones(userId, authentication.getName()));
    }
}
