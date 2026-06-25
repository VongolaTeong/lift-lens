package com.liftlens.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Guards {@code /internal/**} with a shared secret header so an external cron can trigger jobs while
 * the rest of the app stays open in Phase 1–3 (CLAUDE.md §6). Phase 4 layers full token auth on top.
 */
public class InternalTokenFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Internal-Token";

    private final String expectedToken;

    public InternalTokenFilter(String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        if (!StringUtils.hasText(expectedToken) || !expectedToken.equals(provided)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid internal token");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
