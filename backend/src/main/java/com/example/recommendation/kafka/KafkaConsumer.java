package com.example.recommendation.kafka;

import com.example.recommendation.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumer {

    @Autowired
    private RecommendationService recommendationService;

    @KafkaListener(topics = "user-events", groupId = "recommendation_group")
    public void consume(String message) {
        // Here you can parse the event and update recommendations
        // For now, just a placeholder
        System.out.println("Received event: " + message);
    }
}