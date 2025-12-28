package group.g.graduation.backend.Security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Logout request containing refresh token")
public class LogoutRequest {
    
    @NotBlank(message = "Refresh token is required")
    @Schema(description = "Refresh token to invalidate", example = "eyJhbGciOiJSUzI1NiJ9...", required = true)
    private String refreshToken;
}