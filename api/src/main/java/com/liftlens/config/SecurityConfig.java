package com.liftlens.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Phase 1–3: open access for app endpoints + Swagger, except {@code /internal/**} which is guarded by
 * a shared-secret header ({@link InternalTokenFilter}) for the external-cron job entry point.
 * Phase 4 replaces this with a static API token protecting writes (CLAUDE.md §7).
 */
@Configuration
public class SecurityConfig {

    private final String internalToken;

    public SecurityConfig(@Value("${liftlens.internal.token:dev-internal-token}") String internalToken) {
        this.internalToken = internalToken;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(new InternalTokenFilter(internalToken),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
