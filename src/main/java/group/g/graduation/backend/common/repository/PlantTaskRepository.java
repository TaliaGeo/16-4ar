package group.g.graduation.backend.common.repository;

import group.g.graduation.backend.common.model.Plant;
import group.g.graduation.backend.common.model.PlantTask;
import group.g.graduation.backend.common.model.TaskType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlantTaskRepository extends JpaRepository<PlantTask, Long> {
    
    // جلب مهام نبتة معينة
    List<PlantTask> findByPlant(Plant plant);
    
    List<PlantTask> findByPlantId(Long plantId);
    
    // جلب مهام بنوع معين
    List<PlantTask> findByTaskType(TaskType taskType);
    
    // جلب المهام المتكررة لنبتة
    List<PlantTask> findByPlantIdAndIsRecurringTrue(Long plantId);
    
    // جلب مهام نبتة مع تفاصيل نوع المهمة
    @Query("SELECT pt FROM PlantTask pt JOIN FETCH pt.taskType WHERE pt.plant.id = :plantId")
    List<PlantTask> findByPlantIdWithTaskType(@Param("plantId") Long plantId);
}
