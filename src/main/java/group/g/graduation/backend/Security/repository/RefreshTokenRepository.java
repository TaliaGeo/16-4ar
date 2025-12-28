package group.g.graduation.backend.Security.repository;

import group.g.graduation.backend.Security.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    
    Optional<RefreshToken> findByToken(String token);
    
    Optional<RefreshToken> findByUserIdAndRevokedFalse(Long userId);
    
    List<RefreshToken> findByUserId(Long userId);
    
    List<RefreshToken> findByUserIdAndRevoked(Long userId, boolean revoked);
    
    boolean existsByUserIdAndRevokedFalse(Long userId);
    
    long countByUserId(Long userId);
    
    // Delete expired tokens
    @Modifying
    @Transactional
    void deleteByExpiryDateBefore(Instant expiryDate);
    
    // Find expired tokens
    List<RefreshToken> findByExpiryDateBefore(Instant expiryDate);
    
    // Delete all tokens for a user
    @Modifying
    @Transactional
    void deleteByUserId(Long userId);
}