package group.g.graduation.backend.common.repository;

import group.g.graduation.backend.common.model.Plant;
import group.g.graduation.backend.common.model.PlantSuitability;
import group.g.graduation.backend.common.model.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlantSuitabilityRepository extends JpaRepository<PlantSuitability, Long> {
    
    // جلب ملاءمة نبات لخيار معين
    Optional<PlantSuitability> findByPlantAndOption(Plant plant, QuestionOption option);
    
    Optional<PlantSuitability> findByPlantIdAndOptionId(Long plantId, Long optionId);
    
    // جلب كل ملاءمات نبات
    List<PlantSuitability> findByPlant(Plant plant);
    
    List<PlantSuitability> findByPlantId(Long plantId);
    
    // جلب كل ملاءمات خيار
    List<PlantSuitability> findByOption(QuestionOption option);
    
    List<PlantSuitability> findByOptionId(Long optionId);
    
    // جلب النباتات المناسبة لمجموعة خيارات (للاقتراحات)
    @Query("SELECT ps.plant.id, SUM(ps.score) as totalScore FROM PlantSuitability ps WHERE ps.option.id IN :optionIds GROUP BY ps.plant.id ORDER BY totalScore DESC")
    List<Object[]> findPlantScoresByOptions(@Param("optionIds") List<Long> optionIds);
    
    // جلب ملاءمات نبات مع تفاصيل الخيارات
    @Query("SELECT ps FROM PlantSuitability ps JOIN FETCH ps.option o JOIN FETCH o.question WHERE ps.plant.id = :plantId")
    List<PlantSuitability> findByPlantIdWithOptionDetails(@Param("plantId") Long plantId);
}
