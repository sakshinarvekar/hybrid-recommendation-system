package com.example.recommendation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RedisService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    public void saveRecommendations(Long userId, List<Long> recommendations) {
        String key = "recommendations:" + userId;

        // Convert list to comma-separated string
        String value = recommendations.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        redisTemplate.opsForValue().set(key, value);
    }

    public List<Long> getRecommendations(Long userId) {
        String key = "recommendations:" + userId;

        String value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return null;
        }

        return Arrays.stream(value.split(","))
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }
}