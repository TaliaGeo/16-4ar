package group.g.graduation.backend.common.repository;

import group.g.graduation.backend.common.model.PlantingQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlantingQuestionRepository extends JpaRepository<PlantingQuestion, Long> {
    
    // جلب سؤال بالمفتاح
    Optional<PlantingQuestion> findByQuestionKey(String questionKey);
    
    // جلب الأسئلة النشطة مرتبة
    List<PlantingQuestion> findByIsActiveTrueOrderByDisplayOrderAsc();
    
    // جلب الأسئلة الإجبارية
    List<PlantingQuestion> findByIsRequiredTrueAndIsActiveTrueOrderByDisplayOrderAsc();
    
    // جلب الأسئلة الاختيارية
    List<PlantingQuestion> findByIsRequiredFalseAndIsActiveTrueOrderByDisplayOrderAsc();
    
    // جلب الأسئلة مع خياراتها
    @Query("SELECT DISTINCT q FROM PlantingQuestion q LEFT JOIN FETCH q.options WHERE q.isActive = true ORDER BY q.displayOrder ASC")
    List<PlantingQuestion> findAllActiveWithOptions();
    
    // جلب سؤال مع خياراته
    @Query("SELECT q FROM PlantingQuestion q LEFT JOIN FETCH q.options WHERE q.questionKey = :key")
    Optional<PlantingQuestion> findByQuestionKeyWithOptions(String key);
}
