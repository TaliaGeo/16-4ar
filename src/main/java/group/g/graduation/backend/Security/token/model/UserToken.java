package group.g.graduation.backend.Security.token.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "user_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String tokenId;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private Instant expiryDate;
    
    @Column(nullable = false)
    private boolean revoked = false;
    
    // Track device/browser information
    private String userAgent;
    private String ipAddress;
    
    @CreationTimestamp
    private Instant createdAt;
    
    @UpdateTimestamp
    private Instant updatedAt;
    
    public boolean isValid() {
        return !revoked && Instant.now().isBefore(expiryDate);
    }
    
    public boolean isExpired() {
        return Instant.now().isAfter(expiryDate);
    }
}