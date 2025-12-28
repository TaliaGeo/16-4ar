package group.g.graduation.backend.Security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Password reset token information")
public class PasswordResetTokenDTO {
    
    @Schema(description = "Token ID", example = "1")
    private Long id;
    
    @Schema(description = "Password reset token", example = "a6c81d0a-f1b3-4b9d-9203-3758c17a48b8")
    private String token;
    
    @Schema(description = "User ID", example = "123")
    private Long userId;
    
    @Schema(description = "User email", example = "user@example.com")
    private String userEmail;
    
    @Schema(description = "Token expiry date")
    private Instant expiryDate;
    
    @Schema(description = "Whether the token has been used", example = "false")
    private boolean used;
    
    @Schema(description = "Token creation date")
    private Instant createdAt;
    
    /**
     * Check if the token is valid
     */
    public boolean isValid() {
        return !used && Instant.now().isBefore(expiryDate);
    }
}