package group.g.graduation.backend.Security.controller;

import group.g.graduation.backend.common.ratelimit.RateLimit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import group.g.graduation.backend.Security.dto.AuthResponse;
import group.g.graduation.backend.Security.dto.LoginRequest;
import group.g.graduation.backend.Security.dto.MessageResponse;
import group.g.graduation.backend.Security.dto.RegisterRequest;
import group.g.graduation.backend.Security.dto.TokenRefreshRequest;
import group.g.graduation.backend.Security.dto.TokenRefreshResponse;
import group.g.graduation.backend.Security.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "API endpoints for user authentication and registration")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @RateLimit(requests = 5, duration = 60, message = "Too many login attempts. Please try again later")
    @Operation(summary = "User login", description = "Authenticate user and return JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        log.info("Login request received for: {}", loginRequest.getEmail());
        return ResponseEntity.ok(authService.login(loginRequest, request));
    }

    @PostMapping("/register")
    @RateLimit(requests = 3, duration = 60, message = "Too many registration attempts. Please try again later")
    @Operation(summary = "User registration", description = "Register a new user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registration successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest, HttpServletRequest request) {
        log.info("Registration request received for: {}", registerRequest.getEmail());
        return ResponseEntity.ok(authService.register(registerRequest, request));
    }
    
    @PostMapping("/refresh-token")
    @RateLimit(requests = 10, duration = 60, message = "Too many token refresh attempts")
    @Operation(summary = "Refresh JWT token", description = "Refresh the access token using the refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = TokenRefreshResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid refresh token"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    public ResponseEntity<TokenRefreshResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request,
                                                            HttpServletRequest httpRequest) {
        log.info("Token refresh request received");
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken(), httpRequest));
    }
    
    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Logout user and invalidate refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logout successful",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
     public ResponseEntity<MessageResponse> logoutUser(@Valid @RequestBody TokenRefreshRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(new MessageResponse("Logout successful"));
    }
}