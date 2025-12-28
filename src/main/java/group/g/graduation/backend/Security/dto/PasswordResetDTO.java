package group.g.graduation.backend.Security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Password reset request")
public class PasswordResetDTO {
    
    @NotBlank(message = "Token is required")
    @Schema(description = "Password reset token", required = true)
    private String token;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$",
        message = "Password must contain at least one digit, one lowercase letter, one uppercase letter, one special character, and no whitespace"
    )
    @Schema(description = "New password", required = true)
    private String password;
    
    @NotBlank(message = "Password confirmation is required")
    @Schema(description = "Password confirmation", required = true)
    private String confirmPassword;
}