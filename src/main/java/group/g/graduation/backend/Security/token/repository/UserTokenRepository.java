package group.g.graduation.backend.Security.token.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import group.g.graduation.backend.Security.token.model.UserToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, Long> {
    /**
     * Find a token by its unique identifier
     */
    Optional<UserToken> findByTokenId(String tokenId);
    
    /**
     * Find all tokens belonging to a specific user
     */
    List<UserToken> findByUserId(Long userId);
    
    /**
     * Find all non-revoked tokens for a user
     */
    List<UserToken> findByUserIdAndRevokedFalse(Long userId);
    
    /**
     * Find all expired tokens
     */
    List<UserToken> findByExpiryDateBefore(Instant date);
    
    /**
     * Check if a non-revoked token exists with the given ID
     */
    boolean existsByTokenIdAndRevokedFalse(String tokenId);
    
    /**
     * Update revocation status for a specific token
     */
    @Modifying
    int deleteByTokenId(String tokenId);
    
    /**
     * Delete expired tokens
     */
    @Modifying
    int deleteByExpiryDateBefore(Instant date);
    
    /**
     * Count active tokens for a user
     */
    long countByUserIdAndRevokedFalse(Long userId);
}