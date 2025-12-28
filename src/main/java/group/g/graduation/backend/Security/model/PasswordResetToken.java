package group.g.graduation.backend.Security.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String token;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private String userEmail;
    
    @Column(nullable = false)
    private Instant expiryDate;
    
    @Column(nullable = false)
    private boolean used = false;
    
    @CreationTimestamp
    private Instant createdAt;
    
    @UpdateTimestamp
    private Instant updatedAt;
    
    /**
     * Factory method to create a new token for a user
     * 
     * @param userId The user ID
     * @param userEmail The user email
     * @param expirationMinutes How long the token is valid (in minutes)
     * @return New PasswordResetToken
     */
    public static PasswordResetToken createTokenForUser(Long userId, String userEmail, int expirationMinutes) {
        return PasswordResetToken.builder()
                .userId(userId)
                .userEmail(userEmail)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusSeconds(expirationMinutes * 60L))
                .used(false)
                .build();
    }
    
    /**
     * Check if the token is expired
     * 
     * @return true if expired, false otherwise
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiryDate);
    }
    
    /**
     * Check if the token is valid (not expired and not used)
     * 
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return !isExpired() && !isUsed();
    }
    
    /**
     * Mark the token as used
     */
    public void markAsUsed() {
        this.used = true;
    }
}