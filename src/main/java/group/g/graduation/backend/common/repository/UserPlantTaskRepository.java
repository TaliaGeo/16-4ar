package group.g.graduation.backend.common.repository;

import group.g.graduation.backend.common.enums.TaskStatus;
import group.g.graduation.backend.common.model.UserPlant;
import group.g.graduation.backend.common.model.UserPlantTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface UserPlantTaskRepository extends JpaRepository<UserPlantTask, Long> {
    
    // جلب مهام نبتة مستخدم
    List<UserPlantTask> findByUserPlant(UserPlant userPlant);
    
    List<UserPlantTask> findByUserPlantId(Long userPlantId);
    
    // جلب مهام بحالة معينة
    List<UserPlantTask> findByUserPlantIdAndStatus(Long userPlantId, TaskStatus status);
    
    // جلب المهام المستحقة اليوم
    List<UserPlantTask> findByUserPlantIdAndDueDateAndStatusNot(Long userPlantId, LocalDate dueDate, TaskStatus status);
    
    // جلب المهام المتأخرة
    List<UserPlantTask> findByUserPlantIdAndDueDateBeforeAndStatusNotIn(Long userPlantId, LocalDate date, List<TaskStatus> excludedStatuses);
    
    // جلب كل مهام مستخدم المستحقة اليوم
    @Query("SELECT upt FROM UserPlantTask upt JOIN upt.userPlant up WHERE up.user.id = :userId AND upt.dueDate = :date AND upt.status NOT IN :excludedStatuses")
    List<UserPlantTask> findTodayTasksForUser(@Param("userId") Long userId, @Param("date") LocalDate date, @Param("excludedStatuses") List<TaskStatus> excludedStatuses);
    
    // جلب المهام المتأخرة لمستخدم
    @Query("SELECT upt FROM UserPlantTask upt JOIN upt.userPlant up WHERE up.user.id = :userId AND upt.dueDate < :date AND upt.status NOT IN :excludedStatuses")
    List<UserPlantTask> findOverdueTasksForUser(@Param("userId") Long userId, @Param("date") LocalDate date, @Param("excludedStatuses") List<TaskStatus> excludedStatuses);
    
    // جلب مهام نبتة مع تفاصيل نوع المهمة
    @Query("SELECT upt FROM UserPlantTask upt JOIN FETCH upt.taskType WHERE upt.userPlant.id = :userPlantId ORDER BY upt.dueDate ASC")
    List<UserPlantTask> findByUserPlantIdWithTaskType(@Param("userPlantId") Long userPlantId);
    
    // عدد المهام المتأخرة لمستخدم
    @Query("SELECT COUNT(upt) FROM UserPlantTask upt JOIN upt.userPlant up WHERE up.user.id = :userId AND upt.dueDate < :date AND upt.status = :status")
    long countOverdueTasksForUser(@Param("userId") Long userId, @Param("date") LocalDate date, @Param("status") TaskStatus status);
}
