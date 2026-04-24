package com.example.recommendation.service;

import com.example.recommendation.Interaction;
import com.example.recommendation.Product;
import com.example.recommendation.repository.InteractionRepository;
import com.example.recommendation.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    @Autowired
    private InteractionRepository interactionRepository;

    @Autowired
    private ProductRepository productRepository;

    public List<Long> computeRecommendations(Long userId) {
        Set<Long> seen = interactionRepository.findAll().stream()
                .filter(i -> i.getUserId().equals(userId))
                .map(Interaction::getProductId)
                .collect(Collectors.toSet());

        return productRepository.findAll().stream()
                .filter(p -> !seen.contains(p.getId()))
                .limit(5)
                .map(Product::getId)
                .collect(Collectors.toList());
    }
}