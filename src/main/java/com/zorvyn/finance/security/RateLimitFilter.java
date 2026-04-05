package com.zorvyn.finance.security;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter{

    private final RateLimitingService rateLimitingService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        // 1. Only apply the rate limit to the sensitive Auth endpoints
        if (uri.startsWith("/api/auth/login") || uri.startsWith("/api/auth/register")) {

            String ipAddress = getClientIP(request);
            Bucket bucket = rateLimitingService.resolveBucket(ipAddress);

            // 2. Try to consume 1 token from the bucket
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

            if (probe.isConsumed()) {
                // Success: Add a header letting the frontend know how many attempts are left
                response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
                filterChain.doFilter(request, response);
            } else {
                // Denied: The bucket is empty
                long waitForRefillSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;

                response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefillSeconds));
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // Returns 429
                response.setContentType("application/json");

                // Match the project's standard ApiResponse envelope
                String jsonResponse = """
                        {
                          "success": false,
                          "message": "Too many attempts. Please try again in %d seconds.",
                          "data": null,
                          "timestamp": "%s"
                        }
                        """.formatted(waitForRefillSeconds, LocalDateTime.now().toString());

                response.getWriter().write(jsonResponse);
            }
        } else {
            // Let all non-auth requests pass through without rate limiting
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Extracts the real IP address, handling scenarios where the app is behind a proxy.
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        // If multiple IPs are forwarded, the first one is the true client IP
        return xfHeader.split(",")[0];
    }
}
