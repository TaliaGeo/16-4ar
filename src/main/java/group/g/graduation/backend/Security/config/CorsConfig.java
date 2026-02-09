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
        
        // Allow localhost and configured origins
        List<String> origins = new java.util.ArrayList<>(Arrays.asList(allowedOrigins.split(",")));
        origins.add("http://localhost:*");
        origins.add("http://127.0.0.1:*");
        config.setAllowedOriginPatterns(origins);
        
        // Allow credentials
        config.setAllowCredentials(true);
        
        // Allow ALL HTTP methods including PATCH
        List<String> methods = new java.util.ArrayList<>(Arrays.asList(allowedMethods.split(",")));
        if (!methods.contains("PATCH")) {
            methods.add("PATCH");
        }
        if (!methods.contains("OPTIONS")) {
            methods.add("OPTIONS");
        }
        config.setAllowedMethods(methods);
        
        // Allow all headers
        config.setAllowedHeaders(Arrays.asList("*"));
        
        // Expose headers
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));
        
        // Preflight cache duration
        config.setMaxAge(maxAge);
        
        // Apply to all paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
}