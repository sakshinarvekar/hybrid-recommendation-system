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
    private final String FAKESTORE_URL  = "https://fakestoreapi.com/products";
    private final String PYTHON_ML_URL  = "http://localhost:5000";

    // ─────────────────────────────────────────
    // Rupali's methods — unchanged
    // ─────────────────────────────────────────

    public List<Map<String, Object>> fetchAllProducts() {
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
            FAKESTORE_URL,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );
        return response.getBody();
    }

    public List<Map<String, Object>> searchProducts(String keyword) {
        List<Map<String, Object>> all = fetchAllProducts();
        System.out.println("Total products fetched: " + all.size());
        List<Map<String, Object>> results = all.stream()
            .filter(p -> p.get("title").toString()
                .toLowerCase().contains(keyword.toLowerCase()))
            .collect(Collectors.toList());
        System.out.println("Search results for '" + keyword + "': " + results.size());
        return results;
    }

    public List<Map<String, Object>> getRecommendationsByCategory(String category) {
        List<Map<String, Object>> all = fetchAllProducts();
        return all.stream()
            .filter(p -> p.get("category").toString().equalsIgnoreCase(category))
            .limit(5)
            .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────
    // Sakshi's ML — calls Python Flask API
    // Falls back to DB filter if Python is down
    // ─────────────────────────────────────────

    public List<Long> computeRecommendations(Long userId) {
        try {
            String url = PYTHON_ML_URL + "/recommend/" + userId;
            System.out.println("Calling Python ML API: " + url);

            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, null, Map.class
            );

            Map body = response.getBody();
            if (body == null) return fallback();

            List<Map<String, Object>> recommendations =
                (List<Map<String, Object>>) body.get("recommendations");

            if (recommendations == null || recommendations.isEmpty()) return fallback();

            List<Long> ids = recommendations.stream()
                .map(p -> Long.valueOf(p.get("id").toString()))
                .collect(Collectors.toList());

            System.out.println("Python ML returned IDs: " + ids);
            return ids;

        } catch (Exception e) {
            System.err.println("Python ML API unavailable: " + e.getMessage());
            System.err.println("Falling back to DB filter...");
            return fallbackFromDB(userId);
        }
    }

    // Get full product objects from Python ML
    public List<Map<String, Object>> getMLRecommendations(Long userId) {
        try {
            String url = PYTHON_ML_URL + "/recommend/" + userId;
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, null, Map.class
            );
            Map body = response.getBody();
            if (body == null) return Collections.emptyList();
            return (List<Map<String, Object>>) body.get("recommendations");
        } catch (Exception e) {
            System.err.println("Python ML API unavailable: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    // ─────────────────────────────────────────
    // Fallbacks
    // ─────────────────────────────────────────

    private List<Long> fallback() {
        return List.of(1L, 2L, 3L, 4L, 5L);
    }

    private List<Long> fallbackFromDB(Long userId) {
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