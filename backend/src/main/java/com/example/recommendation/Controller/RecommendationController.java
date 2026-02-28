package com.example.recommendation.controller;

import com.example.recommendation.service.RecommendationService;
import com.example.recommendation.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recommendations")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private RedisService redisService;

    /**
     * Get recommendations for a user.
     * Returns dummy data if Redis/DB is not yet ready.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<List<Long>> getRecommendations(@PathVariable Long userId) {

        List<Long> recs = null;

        try {
            // Try Redis first (optional, skip if not running)
            if (redisService != null) {
                recs = redisService.getRecommendations(userId);
            }

            // If Redis empty or null, compute recommendations
            if (recs == null) {
                if (recommendationService != null) {
                    recs = recommendationService.computeRecommendations(userId);
                }

                // Save to Redis if available
                if (redisService != null && recs != null) {
                    redisService.saveRecommendations(userId, recs);
                }
            }

        } catch (Exception e) {
            // Catch all exceptions and print for debugging
            System.out.println("Warning: Could not fetch recommendations: " + e.getMessage());
        }

        // If still null, return dummy data for UI preview
        if (recs == null) {
            recs = List.of(1L, 2L, 3L, 4L, 5L);
        }

        return ResponseEntity.ok(recs);
    }
}
// package com.example.recommendation.controller;

// import com.example.recommendation.service.RecommendationService;
// import com.example.recommendation.service.RedisService;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.web.bind.annotation.*;

// import java.util.List;

// @RestController
// @RequestMapping("/recommendations")
// public class RecommendationController {

//     @Autowired
//     private RecommendationService recommendationService;

//     @Autowired
//     private RedisService redisService;

//     @GetMapping("/{userId}")
//     public List<Long> getRecommendations(@PathVariable Long userId) {
//         // Check Redis cache first
//         List<Long> recs = redisService.getRecommendations(userId);

//         if (recs == null) {
//             // Compute recommendations
//             recs = recommendationService.computeRecommendations(userId);
//             // Save to Redis
//             redisService.saveRecommendations(userId, recs);
//         }

//         return recs;
//     }
// }