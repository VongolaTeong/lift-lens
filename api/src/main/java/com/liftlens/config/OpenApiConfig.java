package com.liftlens.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata + the API-token security scheme so Swagger UI's "Authorize" sends the
 * {@code X-API-Token} header on write calls (CLAUDE.md §7 — self-documenting API).
 */
@Configuration
public class OpenApiConfig {

    private static final String API_TOKEN_SCHEME = "apiToken";

    @Bean
    OpenAPI liftLensOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("LiftLens API")
                        .version("v1")
                        .description("Analytics over Hevy workout exports: ingestion, materialized "
                                + "training stats, and insight detection. Reads are public; writes "
                                + "require the X-API-Token header."))
                .components(new Components().addSecuritySchemes(API_TOKEN_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-Token")
                                .description("Static API token protecting write endpoints.")));
    }
}
