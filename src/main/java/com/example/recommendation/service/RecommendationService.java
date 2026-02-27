package com.example.recommendation.service;

import com.example.recommendation.repository.InteractionRepository;
import com.example.recommendation.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RecommendationService {

    @Autowired
    private InteractionRepository interactionRepository;

    @Autowired
    private ProductRepository productRepository;

    // Temporary simple recommendation logic
    public List<Long> computeRecommendations(Long userId) {
        // For now, return first 5 products
        List<Long> recommendations = new ArrayList<>();
        productRepository.findAll().stream().limit(5).forEach(p -> recommendations.add(p.getId()));
        return recommendations;
    }

    // You will later update this to use Collaborative & Content-based filtering
}