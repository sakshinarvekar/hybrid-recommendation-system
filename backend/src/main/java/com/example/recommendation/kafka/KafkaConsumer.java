package com.example.recommendation.kafka;

import com.example.recommendation.Interaction;
import com.example.recommendation.service.RecommendationService;
import com.example.recommendation.service.RedisService;          // ← missing
import com.fasterxml.jackson.databind.ObjectMapper;              // ← missing
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KafkaConsumer {

    @Autowired private RecommendationService recommendationService;
    @Autowired private RedisService redisService;
    @Autowired private ObjectMapper objectMapper;

    @KafkaListener(topics = "user-events", groupId = "recommendation-group")
    public void consume(String message) {
        try {
            Interaction event = objectMapper.readValue(message, Interaction.class);
            List<Long> recs = recommendationService.computeRecommendations(event.getUserId());
            redisService.saveRecommendations(event.getUserId(), recs);
        } catch (Exception e) {
            System.err.println("Failed to process event: " + e.getMessage());
        }
    }
}

