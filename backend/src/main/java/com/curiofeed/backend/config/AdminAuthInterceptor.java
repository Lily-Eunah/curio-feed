package com.curiofeed.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Guards {@code /api/admin/**} endpoints with a shared bearer-style token.
 *
 * <p>The expected token is read from {@code admin.api.token} (env {@code ADMIN_API_TOKEN}).
 * Requests must send it in the {@code X-Admin-Token} header. Behaviour is fail-closed:
 * if no token is configured, every admin request is rejected with 503 so that a
 * mis-configured deployment locks the admin surface instead of exposing it.
 *
 * <p>CORS pre-flight ({@code OPTIONS}) requests are allowed through so the browser can
 * complete the pre-flight before sending the authenticated request. This interceptor runs
 * after Spring MVC's CORS handling, so rejection responses still carry the CORS headers.
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthInterceptor.class);
    private static final String TOKEN_HEADER = "X-Admin-Token";

    private final String expectedToken;

    public AdminAuthInterceptor(@Value("${admin.api.token:}") String expectedToken) {
        this.expectedToken = expectedToken == null ? "" : expectedToken.trim();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Let CORS pre-flight through — it never carries custom headers.
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        if (expectedToken.isEmpty()) {
            log.warn("Admin API request to {} rejected: ADMIN_API_TOKEN is not configured", request.getRequestURI());
            writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Admin API is not configured");
            return false;
        }

        String provided = request.getHeader(TOKEN_HEADER);
        if (provided == null || !constantTimeEquals(provided.trim(), expectedToken)) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or missing admin token");
            return false;
        }

        return true;
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }

    private static void writeError(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        String body = "{\"error\":\"" + (status == HttpServletResponse.SC_UNAUTHORIZED ? "Unauthorized" : "Service Unavailable")
                + "\",\"message\":\"" + message + "\"}";
        response.getWriter().write(body);
    }
}
