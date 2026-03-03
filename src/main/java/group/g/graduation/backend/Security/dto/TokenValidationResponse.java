package group.g.graduation.backend.Security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Token validation response")
public class TokenValidationResponse {
    
    @Schema(description = "Whether the token is valid")
    private Boolean valid;
    
    @Schema(description = "Optional message")
    private String message;

    public TokenValidationResponse(Boolean valid) {
        this.valid = valid;
    }
}
