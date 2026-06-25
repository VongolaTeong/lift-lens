package com.liftlens.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Single-user security (CLAUDE.md §7): reads are public; writes on {@code /api/**} require a static
 * API token ({@link ApiTokenAuthFilter}, header {@code X-API-Token}). {@code /internal/**} keeps its
 * own shared-secret guard ({@link InternalTokenFilter}) for the external-cron job entry point (§6).
 * Swagger UI and the OpenAPI docs are served openly.
 */
@Configuration
public class SecurityConfig {

    private final String internalToken;
    private final String apiToken;

    public SecurityConfig(
            @Value("${liftlens.internal.token:dev-internal-token}") String internalToken,
            @Value("${liftlens.api.token:dev-api-token}") String apiToken) {
        this.internalToken = internalToken;
        this.apiToken = apiToken;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // /internal/** is enforced by InternalTokenFilter, not by the authorize rules.
                        .requestMatchers("/internal/**").permitAll()
                        // Writes require the API token; reads (and Swagger/docs) stay public.
                        .requestMatchers(HttpMethod.POST, "/api/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        (request, response, ex) -> response.sendError(
                                HttpServletResponse.SC_UNAUTHORIZED, "API token required")))
                .addFilterBefore(new InternalTokenFilter(internalToken),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new ApiTokenAuthFilter(apiToken),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
