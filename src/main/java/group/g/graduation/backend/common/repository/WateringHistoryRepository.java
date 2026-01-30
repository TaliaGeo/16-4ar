package group.g.graduation.backend.common.repository;

import group.g.graduation.backend.common.model.UserPlant;
import group.g.graduation.backend.common.model.WateringHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WateringHistoryRepository extends JpaRepository<WateringHistory, Long> {
    
    // جلب سجل ري نبتة معينة
    List<WateringHistory> findByUserPlantOrderByWateredAtDesc(UserPlant userPlant);
    
    List<WateringHistory> findByUserPlantIdOrderByWateredAtDesc(Long userPlantId);
    
    // جلب آخر عملية ري
    Optional<WateringHistory> findFirstByUserPlantIdOrderByWateredAtDesc(Long userPlantId);
    
    // جلب سجل الري خلال فترة معينة
    List<WateringHistory> findByUserPlantIdAndWateredAtBetweenOrderByWateredAtDesc(
            Long userPlantId, LocalDateTime start, LocalDateTime end);
    
    // عدد مرات الري لنبتة
    long countByUserPlantId(Long userPlantId);
    
    // جلب آخر ري لكل نباتات المستخدم
    @Query("SELECT wh FROM WateringHistory wh WHERE wh.userPlant.user.id = :userId AND wh.wateredAt = (SELECT MAX(wh2.wateredAt) FROM WateringHistory wh2 WHERE wh2.userPlant = wh.userPlant)")
    List<WateringHistory> findLastWateringForAllUserPlants(@Param("userId") Long userId);
}
