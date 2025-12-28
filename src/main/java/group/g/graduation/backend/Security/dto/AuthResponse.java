package group.g.graduation.backend.Security.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String tokenId;  
    private String tokenType = "Bearer";
    private Long userId;
    private String email;
    private String roles;
     private String refreshToken;

    public AuthResponse(String accessToken, String tokenId, Long userId, String email, String roles, String refreshToken) {
        this.accessToken = accessToken;
        this.tokenId = tokenId;
        this.userId = userId;
        this.email = email;
        this.roles = roles;
         this.refreshToken = refreshToken;
        this.tokenType = "Bearer";
    }
     public AuthResponse(String accessToken, String tokenId, Long userId, String email, String roles) {
        this.accessToken = accessToken;
        this.tokenId = tokenId;
        this.userId = userId;
        this.email = email;
        this.roles = roles;
        this.tokenType = "Bearer";
        this.refreshToken = null; // Will be null if not provided
    }
}