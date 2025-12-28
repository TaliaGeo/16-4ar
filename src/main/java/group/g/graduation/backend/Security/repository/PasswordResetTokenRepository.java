package group.g.graduation.backend.Security.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import group.g.graduation.backend.Security.model.PasswordResetToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    
    /**
     * Find a password reset token by its token value
     */
    Optional<PasswordResetToken> findByToken(String token);
    
    /**
     * Find all unused tokens for a user
     */
    List<PasswordResetToken> findByUserIdAndUsedFalse(Long userId);
    
    /**
     * Find all tokens for a user by email
     */
    List<PasswordResetToken> findByUserEmail(String email);
    
    /**
     * Find tokens by user ID and used status
     */
    List<PasswordResetToken> findByUserIdAndUsed(Long userId, boolean used);
    
    /**
     * Check if a token exists by its value
     */
    boolean existsByToken(String token);
    
    /**
     * Find expired tokens
     */
    List<PasswordResetToken> findByExpiryDateBefore(Instant date);
    
    /**
     * Delete expired tokens
     */
    @Modifying
    int deleteByExpiryDateBefore(Instant date);
    
    /**
     * Count tokens by user ID, used status and expiry date after
     */
    long countByUserIdAndUsedAndExpiryDateAfter(Long userId, boolean used, Instant expiryDate);
    
    /**
     * Find tokens by user ID, used status and expiry date after
     */
    List<PasswordResetToken> findByUserIdAndUsedAndExpiryDateAfter(Long userId, boolean used, Instant expiryDate);
}