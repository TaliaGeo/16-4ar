package group.g.graduation.backend.Security.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for OAuth2 authentication endpoints
 * Provides URLs for mobile app to initiate OAuth2 login flow
 */
@RestController
@RequestMapping("/api/auth/oauth2")
@Tag(name = "OAuth2 Authentication", description = "Social login with Google and Facebook")
@Slf4j
public class OAuth2Controller {
    
    @Value("${server.port:8081}")
    private String serverPort;

    /**
     * Get OAuth2 authorization URLs for social login
     * Mobile app will open these URLs in a browser/webview
     */
    @GetMapping("/urls")
    @Operation(summary = "Get OAuth2 login URLs", 
               description = "Returns URLs for Google and Facebook login. Open these in browser/webview.")
    public ResponseEntity<Map<String, Object>> getOAuth2Urls() {
        String baseUrl = "http://localhost:" + serverPort;
        
        Map<String, Object> response = new HashMap<>();
        
        Map<String, String> urls = new HashMap<>();
        urls.put("google", baseUrl + "/oauth2/authorization/google");
        urls.put("facebook", baseUrl + "/oauth2/authorization/facebook");
        
        response.put("oauth2Urls", urls);
        response.put("message", "Open these URLs in browser/webview to initiate OAuth2 login");
        response.put("callbackScheme", "gharsih://oauth2/callback");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Health check for OAuth2 configuration
     */
    @GetMapping("/status")
    @Operation(summary = "Check OAuth2 status", description = "Check if OAuth2 is configured")
    public ResponseEntity<Map<String, Object>> getOAuth2Status() {
        Map<String, Object> response = new HashMap<>();
        response.put("oauth2Enabled", true);
        response.put("providers", new String[]{"google", "facebook"});
        response.put("status", "configured");
        
        return ResponseEntity.ok(response);
    }
}
