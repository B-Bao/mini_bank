package com.example.demo.security.interceptor;

import com.example.demo.service.RedisIdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class IdempotencyInterceptor implements HandlerInterceptor {

    public static final String IDEMPOTENCY_HEADER_KEY = "Idempotency-Key";
    private final RedisIdempotencyService redisIdempotencyService;

    public IdempotencyInterceptor(RedisIdempotencyService redisIdempotencyService) {
        this.redisIdempotencyService = redisIdempotencyService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String method = request.getMethod();
        String requestURI = request.getRequestURI();

        boolean isPostOrPut = "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method);
        boolean isFinancialEndpoint = requestURI.contains("/transfers") || requestURI.contains("/deposits");

        if (isPostOrPut && isFinancialEndpoint) {
            String idempotencyKey = request.getHeader(IDEMPOTENCY_HEADER_KEY);

            if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\": \"Missing required header: Idempotency-Key for " + method + " " + requestURI + "\"}");
                return false;
            }

            try {
                UUID.fromString(idempotencyKey.trim());
            } catch (IllegalArgumentException e) {
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\": \"Invalid Idempotency-Key format. Must be a valid UUID v4.\"}" );
                return false;
            }

            String key = idempotencyKey.trim();

            boolean isLocked = redisIdempotencyService.lockKey(key, 300);

            if (!isLocked) {
                String cachedValue = redisIdempotencyService.getCachedResponse(key);

                response.setContentType("application/json;charset=UTF-8");
                if ("PROCESSING".equals(cachedValue)) {
                    response.setStatus(HttpStatus.CONFLICT.value()); // 409 Conflict
                    response.getWriter().write("{\"error\": \"Giao dịch đang được xử lý.....\"}");
                } else if (cachedValue != null) {
                    response.setStatus(HttpStatus.OK.value());
                    response.getWriter().write(cachedValue);
                } else {
                    response.setStatus(HttpStatus.CONFLICT.value());
                    response.getWriter().write("{\"error\": \"Giao dịch đang xử lý.....\"}");
                }
                return false;
            }
            request.setAttribute("IDEMPOTENCY_KEY", key);
            request.setAttribute("IDEMPOTENCY_KEY", idempotencyKey.trim());
        }

        return true;
    }
}