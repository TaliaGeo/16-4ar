package group.g.graduation.backend.Security.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = true) // Nullable for OAuth2 users
    private String password;
    
    @Column(nullable = false)
    private String fullName;
    
    @Column(nullable = false)
    private boolean active = true;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
    
    private String phoneNumber;
    
    // OAuth2 fields
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider authProvider = AuthProvider.LOCAL;
    
    @Column(name = "provider_id")
    private String providerId; // Google/Facebook user ID
    
    @Column(name = "image_url")
    private String imageUrl; // Profile picture URL
    
    @Column(updatable = false)
    private Instant createdAt = Instant.now();
    
    private Instant updatedAt = Instant.now();
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    // Helper method to check if user registered via OAuth2
    public boolean isOAuth2User() {
        return authProvider != AuthProvider.LOCAL;
    }
}
