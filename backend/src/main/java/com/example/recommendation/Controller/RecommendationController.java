package com.example.recommendation.Controller;

import com.example.recommendation.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recommendations")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<Long>> getRecommendations(@PathVariable Long userId) {
        try {
            List<Long> recs = recommendationService.computeRecommendations(userId);
            if (recs == null || recs.isEmpty()) {
                return ResponseEntity.ok(List.of(1L, 2L, 3L, 4L, 5L));
            }
            return ResponseEntity.ok(recs);
        } catch (Exception e) {
            System.err.println("Recommendation error: " + e.getMessage());
            return ResponseEntity.ok(List.of(1L, 2L, 3L, 4L, 5L));
        }
    }
}
