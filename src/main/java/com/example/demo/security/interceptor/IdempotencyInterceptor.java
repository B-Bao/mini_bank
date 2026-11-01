package com.example.demo.security.interceptor;

import com.example.demo.service.RedisIdempotencyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
public class IdempotencyInterceptor implements HandlerInterceptor {

  public static final String IDEMPOTENCY_HEADER_KEY = "Idempotency-Key";
  private final RedisIdempotencyService redisIdempotencyService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public IdempotencyInterceptor(RedisIdempotencyService redisIdempotencyService) {
    this.redisIdempotencyService = redisIdempotencyService;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
          throws Exception {
    String method = request.getMethod();
    String requestURI = request.getRequestURI();

    boolean isPostOrPut = "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method);
    boolean isFinancialEndpoint =
            requestURI.contains("/transfers") || requestURI.contains("/deposits");

    if (isPostOrPut && isFinancialEndpoint) {
      String idempotencyKey = request.getHeader(IDEMPOTENCY_HEADER_KEY);

      if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType("application/json;charset=UTF-8");
        response
                .getWriter()
                .write(
                        "{\"error\": \"Missing required header: Idempotency-Key for "
                                + method
                                + " "
                                + requestURI
                                + "\"}");
        return false;
      }

      try {
        UUID.fromString(idempotencyKey.trim());
      } catch (IllegalArgumentException e) {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType("application/json;charset=UTF-8");
        response
                .getWriter()
                .write("{\"error\": \"Invalid Idempotency-Key format. Must be a valid UUID v4.\"}");
        return false;
      }

      String key = idempotencyKey.trim();
      boolean isLocked = redisIdempotencyService.lockKey(key, 300);

      if (!isLocked) {
        String cachedValue = redisIdempotencyService.getCachedResponse(key);

        response.setContentType("application/json;charset=UTF-8");
        if ("PROCESSING".equals(cachedValue)) {
          response.setStatus(HttpStatus.CONFLICT.value());
          response.getWriter().write("{\"error\": \"Giao dịch đang được xử lý, vui lòng thử lại sau.\"}");
        } else if (cachedValue != null) {
          // Khôi phục đúng cấu trúc JSON đã cache (bao gồm status thực tế và body)
          try {
            Map<String, Object> cachedMap = objectMapper.readValue(cachedValue, Map.class);
            Integer cachedStatus = (Integer) cachedMap.get("status");
            Object cachedBody = cachedMap.get("body");

            response.setStatus(cachedStatus != null ? cachedStatus : HttpStatus.OK.value());
            response.getWriter().write(objectMapper.writeValueAsString(cachedBody));
          } catch (Exception e) {
            response.setStatus(HttpStatus.OK.value());
            response.getWriter().write(cachedValue);
          }
        } else {
          response.setStatus(HttpStatus.CONFLICT.value());
          response.getWriter().write("{\"error\": \"Giao dịch đang được xử lý...\"}");
        }
        return false;
      }

      request.setAttribute("IDEMPOTENCY_KEY", key);
    }

    if (!(response instanceof ContentCachingResponseWrapper)) {
      request.setAttribute("CACHED_RESPONSE_WRAPPER", new ContentCachingResponseWrapper(response));
    }

    return true;
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
    String idempotencyKey = (String) request.getAttribute("IDEMPOTENCY_KEY");
    ContentCachingResponseWrapper wrapper = (ContentCachingResponseWrapper) request.getAttribute("CACHED_RESPONSE_WRAPPER");
    if (idempotencyKey != null && wrapper != null) {
      if (response.getStatus() >= 200 && response.getStatus() < 300 && ex == null) {
        byte[] content = wrapper.getContentAsByteArray();
        if (content.length > 0) {
          try {
            Object bodyObj = objectMapper.readValue(content, Object.class);
            redisIdempotencyService.cacheResponse(idempotencyKey, bodyObj, wrapper.getStatus(), 300);
          } catch (Exception ignored) {

          }
        }
      }
      wrapper.copyBodyToResponse();
    }
  }
}