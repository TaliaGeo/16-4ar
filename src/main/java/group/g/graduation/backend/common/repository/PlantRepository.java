package group.g.graduation.backend.common.repository;

import group.g.graduation.backend.common.enums.DifficultyLevel;
import group.g.graduation.backend.common.enums.PlantCategory;
import group.g.graduation.backend.common.model.Plant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlantRepository extends JpaRepository<Plant, Long> {
    
    // البحث بالاسم
    List<Plant> findByNameArContainingIgnoreCase(String nameAr);
    List<Plant> findByNameEnContainingIgnoreCase(String nameEn);
    
    // البحث بالتصنيف
    List<Plant> findByCategory(PlantCategory category);
    
    // البحث بمستوى الصعوبة
    List<Plant> findByDifficultyLevel(DifficultyLevel difficultyLevel);
    
    // البحث بالتصنيف ومستوى الصعوبة
    List<Plant> findByCategoryAndDifficultyLevel(PlantCategory category, DifficultyLevel difficultyLevel);
    
    // جلب النباتات مع الصور
    @Query("SELECT DISTINCT p FROM Plant p LEFT JOIN FETCH p.images WHERE p.id = :id")
    Optional<Plant> findByIdWithImages(@Param("id") Long id);
    
    // جلب كل النباتات مع الصورة الرئيسية
    @Query("SELECT DISTINCT p FROM Plant p LEFT JOIN FETCH p.images i WHERE i.isPrimary = true OR i.isPrimary IS NULL")
    List<Plant> findAllWithPrimaryImage();
    
    // عدد النباتات حسب التصنيف
    long countByCategory(PlantCategory category);
    
    // عدد النباتات حسب مستوى الصعوبة
    long countByDifficultyLevel(DifficultyLevel difficultyLevel);
}
