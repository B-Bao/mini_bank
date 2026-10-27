package com.example.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisIdempotencyService {

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper = new ObjectMapper();

  private static final String IDEMPOTENCY_PREFIX = "idempotency:";
  private static final String STATUS_PROCESSING = "PROCESSING";

  public boolean lockKey(String idempotencyKey, long ttlSeconds) {
    if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
      return false;
    }
    String redisKey = IDEMPOTENCY_PREFIX + idempotencyKey.trim();

    Boolean success =
        redisTemplate
            .opsForValue()
            .setIfAbsent(redisKey, STATUS_PROCESSING, Duration.ofSeconds(ttlSeconds));

    return Boolean.TRUE.equals(success);
  }

  public void releaseKey(String idempotencyKey) {
    if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
      return;
    }
    String redisKey = IDEMPOTENCY_PREFIX + idempotencyKey.trim();
    redisTemplate.delete(redisKey);
  }

  public void cacheResponse(
      String idempotencyKey, Object responseBody, int httpStatus, long ttlSeconds) {
    if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
      return;
    }

    try {
      Map<String, Object> cachePayload = new HashMap<>();
      cachePayload.put("status", httpStatus);
      cachePayload.put("body", responseBody);

      String jsonValue = objectMapper.writeValueAsString(cachePayload);
      String redisKey = IDEMPOTENCY_PREFIX + idempotencyKey.trim();

      redisTemplate.opsForValue().set(redisKey, jsonValue, Duration.ofSeconds(ttlSeconds));
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize idempotency response cache for key: {}", idempotencyKey, e);
    }
  }

  public void saveResponse(String idempotencyKey, String jsonResponse, long ttlSeconds) {
    if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
      return;
    }
    String redisKey = IDEMPOTENCY_PREFIX + idempotencyKey.trim();
    redisTemplate.opsForValue().set(redisKey, jsonResponse, Duration.ofSeconds(ttlSeconds));
  }

  public String getCachedResponse(String idempotencyKey) {
    if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
      return null;
    }
    String redisKey = IDEMPOTENCY_PREFIX + idempotencyKey.trim();
    return redisTemplate.opsForValue().get(redisKey);
  }
}
