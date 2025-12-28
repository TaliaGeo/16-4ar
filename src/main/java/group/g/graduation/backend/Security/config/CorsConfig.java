package group.g.graduation.backend.Security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${app.cors.allowed-methods}")
    private String allowedMethods;

    @Value("${app.cors.allowed-headers}")
    private String allowedHeaders;

    @Value("${app.cors.max-age}")
    private long maxAge;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // Allow specified origins
        config.setAllowedOriginPatterns(Arrays.asList(allowedOrigins.split(",")));
        
        // Allow credentials
        config.setAllowCredentials(true);
        
        // Allow specified HTTP methods
        config.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
        
        // Allow specified headers
        config.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
        
        // Expose the Authorization header
        config.setExposedHeaders(List.of("Authorization"));
        
        // How long the response from a pre-flight request can be cached
        config.setMaxAge(maxAge);
        
        // Apply this configuration to all paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
}