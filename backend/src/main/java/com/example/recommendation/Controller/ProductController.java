package com.example.recommendation.Controller;

import com.example.recommendation.Interaction;
import com.example.recommendation.Product;
import com.example.recommendation.kafka.KafkaProducer;
import com.example.recommendation.repository.InteractionRepository;
import com.example.recommendation.repository.ProductRepository;
import com.example.recommendation.service.RecommendationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductRepository productRepository;
    private final InteractionRepository interactionRepository;
    private final KafkaProducer kafkaProducer;
    private final ObjectMapper objectMapper;
    private final RecommendationService recommendationService;

    public ProductController(ProductRepository productRepository,
                             InteractionRepository interactionRepository,
                             KafkaProducer kafkaProducer,
                             ObjectMapper objectMapper,
                             RecommendationService recommendationService) {
        this.productRepository = productRepository;
        this.interactionRepository = interactionRepository;
        this.kafkaProducer = kafkaProducer;
        this.objectMapper = objectMapper;
        this.recommendationService = recommendationService;
    }

    @GetMapping("/products")
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @GetMapping("/products/search")
    public ResponseEntity<List<Map<String, Object>>> searchProducts(
            @RequestParam String keyword) {
        List<Map<String, Object>> results = recommendationService.searchProducts(keyword);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/products/recommendations")
    public ResponseEntity<List<Map<String, Object>>> getRecommendations(
            @RequestParam String category) {
        List<Map<String, Object>> recs = recommendationService.getRecommendationsByCategory(category);
        return ResponseEntity.ok(recs);
    }

    @PostMapping("/interact")
    public ResponseEntity<Void> recordInteraction(@RequestBody Interaction interaction) {
        try {
            interaction.setTimestamp(LocalDateTime.now());
            interactionRepository.save(interaction);
            String message = objectMapper.writeValueAsString(interaction);
            kafkaProducer.sendEvent("user-events", message);
            return ResponseEntity.accepted().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}