package com.example.recommendation.controller;

import com.example.recommendation.service.RecommendationService;
import com.example.recommendation.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recommendations")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private RedisService redisService;

    @GetMapping("/{userId}")
    public List<Long> getRecommendations(@PathVariable Long userId) {
        // Check Redis cache first
        List<Long> recs = redisService.getRecommendations(userId);

        if (recs == null) {
            // Compute recommendations
            recs = recommendationService.computeRecommendations(userId);
            // Save to Redis
            redisService.saveRecommendations(userId, recs);
        }

        return recs;
    }
}