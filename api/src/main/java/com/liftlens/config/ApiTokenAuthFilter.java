package com.liftlens.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates the single app principal from a static API token header (CLAUDE.md §7). When a valid
 * token is presented the request is authenticated as {@code ROLE_API}; otherwise it passes through
 * unauthenticated so public reads still work and {@link SecurityConfig} can reject writes with 401.
 */
public class ApiTokenAuthFilter extends OncePerRequestFilter {

    static final String HEADER = "X-API-Token";

    private final String expectedToken;

    public ApiTokenAuthFilter(String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        if (StringUtils.hasText(expectedToken) && expectedToken.equals(provided)) {
            var authentication = new UsernamePasswordAuthenticationToken(
                    "api", null, List.of(new SimpleGrantedAuthority("ROLE_API")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }
}
