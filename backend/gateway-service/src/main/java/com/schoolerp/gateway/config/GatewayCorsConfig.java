package com.schoolerp.gateway.config;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class GatewayCorsConfig {
    private static final List<String> DEFAULT_ALLOWED_ORIGINS = List.of(
            "http://localhost:5173",
            "http://8.148.181.9:90"
    );

    @Bean
    public CorsWebFilter corsWebFilter(
            @Value("${CORS_ALLOWED_ORIGINS:}") String allowedOrigins,
            @Value("${CORS_ALLOWED_ORIGIN:}") String legacyAllowedOrigin
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(resolveAllowedOrigins(allowedOrigins, legacyAllowedOrigin));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("X-Request-Id"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return new CorsWebFilter(source);
    }

    private List<String> resolveAllowedOrigins(String allowedOrigins, String legacyAllowedOrigin) {
        LinkedHashSet<String> resolvedOrigins = new LinkedHashSet<>();
        appendOrigins(resolvedOrigins, allowedOrigins);
        appendOrigins(resolvedOrigins, legacyAllowedOrigin);

        if (resolvedOrigins.isEmpty()) {
            return DEFAULT_ALLOWED_ORIGINS;
        }

        return List.copyOf(resolvedOrigins);
    }

    private void appendOrigins(LinkedHashSet<String> target, String rawOrigins) {
        if (!StringUtils.hasText(rawOrigins)) {
            return;
        }

        Arrays.stream(rawOrigins.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .forEach(target::add);
    }
}
