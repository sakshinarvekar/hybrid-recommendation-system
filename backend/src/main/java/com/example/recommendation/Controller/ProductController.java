package com.example.recommendation.controller;

import com.example.recommendation.Interaction;
import com.example.recommendation.Product;
import com.example.recommendation.kafka.KafkaProducer;
import com.example.recommendation.repository.InteractionRepository;
import com.example.recommendation.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
public class ProductController {

    private final ProductRepository productRepository;
    private final InteractionRepository interactionRepository;
    private final KafkaProducer kafkaProducer;
    private final ObjectMapper objectMapper;

    public ProductController(ProductRepository productRepository,
                             InteractionRepository interactionRepository,
                             KafkaProducer kafkaProducer,
                             ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.interactionRepository = interactionRepository;
        this.kafkaProducer = kafkaProducer;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/products")
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @PostMapping("/interact")
    public ResponseEntity<Void> recordInteraction(@RequestBody Interaction interaction) {
        try {
            interaction.setTimestamp(LocalDateTime.now());
            interactionRepository.save(interaction);                        // 1. Save to PostgreSQL
            String message = objectMapper.writeValueAsString(interaction);
            kafkaProducer.sendEvent("user-events", message);                // 2. Publish to Kafka
            return ResponseEntity.accepted().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}