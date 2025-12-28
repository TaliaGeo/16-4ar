package group.g.graduation.backend.Security.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/security")
@Tag(name = "Health Check", description = "API health check endpoints")
public class HealthController {

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the API is running")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Gharsih Backend API");
        response.put("timestamp", LocalDateTime.now());
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/endpoints")
    @Operation(summary = "List endpoints", description = "List all available API endpoints")
    public ResponseEntity<Map<String, Object>> listEndpoints() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Set<String> endpoints = requestMappingHandlerMapping.getHandlerMethods().keySet()
                    .stream()
                    .flatMap(mapping -> mapping.getPatternValues().stream())
                    .collect(Collectors.toSet());
            
            response.put("endpoints", endpoints);
            response.put("count", endpoints.size());
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Failed to retrieve endpoints: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(500).body(response);
        }
    }
}
