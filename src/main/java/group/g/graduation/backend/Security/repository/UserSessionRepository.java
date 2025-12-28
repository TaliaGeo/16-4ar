package group.g.graduation.backend.Security.repository;

import group.g.graduation.backend.Security.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    /**
     * Find all active sessions for a user
     */
    List<UserSession> findByUserIdAndActiveOrderByLastActivityDesc(Long userId, boolean active);

    /**
     * Find session by token ID
     */
    Optional<UserSession> findByTokenId(String tokenId);

    /**
     * Find active session by token ID
     */
    Optional<UserSession> findByTokenIdAndActive(String tokenId, boolean active);

    /**
     * Count active sessions for a user
     */
    long countByUserIdAndActive(Long userId, boolean active);

    /**
     * Deactivate all sessions for a user (logout from all devices)
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.active = false, s.loggedOutAt = :now WHERE s.userId = :userId AND s.active = true")
    int deactivateAllUserSessions(@Param("userId") Long userId, @Param("now") Instant now);

    /**
     * Deactivate all sessions except current one
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.active = false, s.loggedOutAt = :now WHERE s.userId = :userId AND s.tokenId != :currentTokenId AND s.active = true")
    int deactivateOtherSessions(@Param("userId") Long userId, @Param("currentTokenId") String currentTokenId, @Param("now") Instant now);

    /**
     * Delete expired sessions (cleanup job)
     */
    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.expiresAt < :now")
    int deleteExpiredSessions(@Param("now") Instant now);

    /**
     * Find all sessions for a user (including inactive)
     */
    List<UserSession> findByUserIdOrderByLoginTimeDesc(Long userId);

    /**
     * Check if token is valid (exists and active)
     */
    boolean existsByTokenIdAndActive(String tokenId, boolean active);
}
