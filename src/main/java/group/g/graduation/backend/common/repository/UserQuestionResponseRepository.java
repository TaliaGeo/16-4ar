package group.g.graduation.backend.common.repository;

import group.g.graduation.backend.common.model.UserQuestionResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserQuestionResponseRepository extends JpaRepository<UserQuestionResponse, Long> {
    
    // جلب إجابات جلسة معينة
    List<UserQuestionResponse> findBySessionId(String sessionId);
    
    // جلب إجابات مستخدم لجلسة معينة
    List<UserQuestionResponse> findByUserIdAndSessionId(Long userId, String sessionId);
    
    // جلب إجابات مستخدم لسؤال في جلسة معينة
    List<UserQuestionResponse> findByUserIdAndSessionIdAndQuestionId(Long userId, String sessionId, Long questionId);
    
    // حذف إجابات جلسة
    void deleteBySessionId(String sessionId);
    
    // حذف إجابات مستخدم لجلسة
    void deleteByUserIdAndSessionId(Long userId, String sessionId);
    
    // جلب الخيارات المختارة في جلسة
    @Query("SELECT uqr.option.id FROM UserQuestionResponse uqr WHERE uqr.sessionId = :sessionId")
    List<Long> findSelectedOptionIdsBySessionId(@Param("sessionId") String sessionId);
    
    // جلب إجابات جلسة مع تفاصيل السؤال والخيار
    @Query("SELECT uqr FROM UserQuestionResponse uqr JOIN FETCH uqr.question JOIN FETCH uqr.option WHERE uqr.sessionId = :sessionId")
    List<UserQuestionResponse> findBySessionIdWithDetails(@Param("sessionId") String sessionId);
}
