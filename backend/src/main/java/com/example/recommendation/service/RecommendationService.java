package com.example.recommendation.service;

import com.example.recommendation.Interaction;
import com.example.recommendation.Product;
import com.example.recommendation.repository.InteractionRepository;
import com.example.recommendation.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    @Autowired
    private InteractionRepository interactionRepository;

    @Autowired
    private ProductRepository productRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final String FAKESTORE_URL = "https://fakestoreapi.com/products";

    // Fetch all products from FakeStore API
    public List<Map<String, Object>> fetchAllProducts() {
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
            FAKESTORE_URL,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );
        return response.getBody();
    }

    // Search products by keyword
  public List<Map<String, Object>> searchProducts(String keyword) {
    List<Map<String, Object>> all = fetchAllProducts();
    
    System.out.println("Total products fetched: " + all.size());
    
    List<Map<String, Object>> results = all.stream()
        .filter(p -> p.get("title").toString()
            .toLowerCase()
            .contains(keyword.toLowerCase()))
        .collect(Collectors.toList());
    
    System.out.println("Search results for '" + keyword + "': " + results.size());
    
    return results;
}

    // Get recommendations based on category of clicked product
    public List<Map<String, Object>> getRecommendationsByCategory(String category) {
        List<Map<String, Object>> all = fetchAllProducts();
        return all.stream()
            .filter(p -> p.get("category").toString()
                .equalsIgnoreCase(category))
            .limit(5)
            .collect(Collectors.toList());
    }

    // Original method (kept for Redis caching flow)
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