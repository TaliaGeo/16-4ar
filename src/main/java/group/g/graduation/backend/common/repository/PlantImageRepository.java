package group.g.graduation.backend.common.repository;

import group.g.graduation.backend.common.model.Plant;
import group.g.graduation.backend.common.model.PlantImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlantImageRepository extends JpaRepository<PlantImage, Long> {
    
    // جلب صور نبتة معينة
    List<PlantImage> findByPlantOrderByDisplayOrderAsc(Plant plant);
    
    List<PlantImage> findByPlantIdOrderByDisplayOrderAsc(Long plantId);
    
    // جلب الصورة الرئيسية
    Optional<PlantImage> findByPlantAndIsPrimaryTrue(Plant plant);
    
    Optional<PlantImage> findByPlantIdAndIsPrimaryTrue(Long plantId);
    
    // حذف كل صور نبتة
    void deleteByPlantId(Long plantId);
}
