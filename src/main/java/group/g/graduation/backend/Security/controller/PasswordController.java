package group.g.graduation.backend.Security.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import group.g.graduation.backend.Security.dto.ForgotPasswordRequest;
import group.g.graduation.backend.Security.dto.MessageResponse;
import group.g.graduation.backend.Security.dto.PasswordResetDTO;
import group.g.graduation.backend.Security.dto.PasswordResetTokenDTO;
import group.g.graduation.backend.Security.dto.PasswordResetVerifyDTO;
import group.g.graduation.backend.Security.dto.TokenValidationResponse;
import group.g.graduation.backend.Security.service.PasswordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Password Management", description = "API endpoints for password management")
public class PasswordController {
    
    private final PasswordService passwordService;
    
    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot Password", description = "Process forgot password request")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password reset link sent successfully",
                content = @Content(schema = @Schema(implementation = MessageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Processing forgot password request for email: {}", request.getEmail());
        
        try {
            // For security reasons, we don't reveal if the email exists in our system
            passwordService.processForgotPassword(request);
            return ResponseEntity.ok(new MessageResponse(
                "If the email exists in our system, a password reset link will be sent shortly"));
        } catch (IllegalStateException e) {
            // Handle rate limiting
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error processing forgot password request", e);
            // Still return a success message to avoid user enumeration
            return ResponseEntity.ok(new MessageResponse(
                "If the email exists in our system, a password reset link will be sent shortly"));
        }
    }
    
    @PostMapping("/reset-password")
    @Operation(summary = "Reset Password", description = "Reset user password using token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password reset successfully",
                content = @Content(schema = @Schema(implementation = MessageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Invalid or expired token")
    })
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody PasswordResetDTO request) {
        log.info("Processing password reset request");
        
        try {
            passwordService.resetPassword(request);
            return ResponseEntity.ok(new MessageResponse("Password has been reset successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error processing password reset", e);
            return ResponseEntity.badRequest().body(new MessageResponse("Password reset failed"));
        }
    }
    
    @PostMapping("/reset-password/verify")
    @Operation(summary = "Verify Token or Reset Password", 
               description = "Verify password reset token if only token is provided, or reset password if token, password, and confirmPassword are provided")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Operation successful"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Invalid or expired token")
    })
    public ResponseEntity<?> verifyOrResetPassword(@Valid @RequestBody PasswordResetVerifyDTO request) {
        log.info("Processing reset-password/verify request");
        
        // Check if this is a verification request (token only) or a reset request (token + password)
        boolean isVerificationOnly = request.getPassword() == null || request.getPassword().isEmpty();
        
        if (isVerificationOnly) {
            // Verification only - validate token
            log.info("Verifying password reset token");
            boolean isValid = passwordService.validateToken(request.getToken());
            return ResponseEntity.ok(new TokenValidationResponse(isValid));
        } else {
            // Reset password
            log.info("Resetting password with token");
            
            // Validate that confirmPassword matches password
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.badRequest().body(
                    new MessageResponse("Password and confirm password do not match"));
            }
            
            try {
                // Create PasswordResetDTO from PasswordResetVerifyDTO
                PasswordResetDTO resetDTO = new PasswordResetDTO();
                resetDTO.setToken(request.getToken());
                resetDTO.setPassword(request.getPassword());
                resetDTO.setConfirmPassword(request.getConfirmPassword());
                
                passwordService.resetPassword(resetDTO);
                return ResponseEntity.ok(new MessageResponse("Password has been reset successfully"));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
            } catch (Exception e) {
                log.error("Error processing password reset", e);
                return ResponseEntity.badRequest().body(new MessageResponse("Password reset failed"));
            }
        }
    }
    
    @GetMapping("/validate-reset-token")
    @Operation(summary = "Validate Password Reset Token", description = "Validate the password reset token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token is valid",
                content = @Content(schema = @Schema(implementation = MessageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    public ResponseEntity<MessageResponse> validateResetToken(@RequestParam("token") String token) {
        log.info("Validating password reset token");
        
        boolean isValid = passwordService.validateToken(token);
        
        if (isValid) {
            return ResponseEntity.ok(new MessageResponse("Token is valid"));
        } else {
            return ResponseEntity.badRequest().body(new MessageResponse("Token is invalid or expired"));
        }
    }
    
    @GetMapping("/reset-token-info")
    @Operation(summary = "Get Password Reset Token Info", description = "Get information about a password reset token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token information retrieved",
                content = @Content(schema = @Schema(implementation = PasswordResetTokenDTO.class))),
        @ApiResponse(responseCode = "404", description = "Token not found")
    })
    public ResponseEntity<?> getTokenInfo(@RequestParam("token") String token) {
        Optional<PasswordResetTokenDTO> tokenInfo = passwordService.getTokenInfo(token);
        
        if (tokenInfo.isPresent()) {
            // Don't return sensitive information
            PasswordResetTokenDTO dto = tokenInfo.get();
            dto.setToken(null); // Remove actual token value for security
            return ResponseEntity.ok(dto);
        } else {
            return ResponseEntity.badRequest().body(new MessageResponse("Token not found"));
        }
    }
    
    // Admin endpoints
    @GetMapping("/admin/reset-tokens/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user's password reset tokens", description = "Get all valid password reset tokens for a user")
    public ResponseEntity<List<PasswordResetTokenDTO>> getUserResetTokens(@PathVariable Long userId) {
        return ResponseEntity.ok(passwordService.getValidTokensForUser(userId));
    }
}