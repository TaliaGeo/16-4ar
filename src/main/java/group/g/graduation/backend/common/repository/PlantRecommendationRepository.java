package group.g.graduation.backend.common.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import group.g.graduation.backend.common.model.PlantRecommendation;

@Repository
public interface PlantRecommendationRepository extends JpaRepository<PlantRecommendation, Long> {
    
    // جلب توصيات جلسة معينة
    List<PlantRecommendation> findBySessionIdOrderByMatchPercentageDesc(String sessionId);
    
    // جلب توصيات مستخدم لجلسة معينة
    List<PlantRecommendation> findByUserIdAndSessionIdOrderByMatchPercentageDesc(Long userId, String sessionId);

    // جلب توصيات مستخدم لجلسة معينة مع تفاصيل النبات
    @Query("SELECT pr FROM PlantRecommendation pr JOIN FETCH pr.plant WHERE pr.user.id = :userId AND pr.sessionId = :sessionId ORDER BY pr.matchPercentage DESC")
    List<PlantRecommendation> findByUserIdAndSessionIdWithPlant(@Param("userId") Long userId, @Param("sessionId") String sessionId);
    
    // جلب توصية لنبتة في جلسة
    Optional<PlantRecommendation> findBySessionIdAndPlantId(String sessionId, Long plantId);
    
    // جلب التوصيات المختارة
    List<PlantRecommendation> findByUserIdAndIsSelectedTrue(Long userId);
    
    // حذف توصيات جلسة
    void deleteBySessionId(String sessionId);
    
    // جلب توصيات مع تفاصيل النبات
    @Query("SELECT pr FROM PlantRecommendation pr JOIN FETCH pr.plant WHERE pr.sessionId = :sessionId ORDER BY pr.matchPercentage DESC")
    List<PlantRecommendation> findBySessionIdWithPlant(@Param("sessionId") String sessionId);
    
    // جلب أعلى التوصيات لجلسة
    @Query("SELECT pr FROM PlantRecommendation pr JOIN FETCH pr.plant WHERE pr.sessionId = :sessionId ORDER BY pr.matchPercentage DESC")
    List<PlantRecommendation> findTopRecommendationsBySessionId(@Param("sessionId") String sessionId);
}
