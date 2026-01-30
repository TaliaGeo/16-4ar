package group.g.graduation.backend.common.repository;

import group.g.graduation.backend.Security.model.User;
import group.g.graduation.backend.common.enums.PlantStatus;
import group.g.graduation.backend.common.model.Plant;
import group.g.graduation.backend.common.model.UserPlant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPlantRepository extends JpaRepository<UserPlant, Long> {
    
    // جلب كل نباتات مستخدم
    List<UserPlant> findByUser(User user);
    
    List<UserPlant> findByUserId(Long userId);
    
    // جلب نباتات مستخدم بحالة معينة
    List<UserPlant> findByUserIdAndStatus(Long userId, PlantStatus status);
    
    // جلب النباتات المخطط لزراعتها
    List<UserPlant> findByUserIdAndStatusOrderByPlannedDateDesc(Long userId, PlantStatus status);
    
    // جلب النباتات المزروعة حالياً
    List<UserPlant> findByUserIdAndStatusOrderByPlantedDateDesc(Long userId, PlantStatus status);
    
    // التحقق من وجود نبتة لدى المستخدم
    boolean existsByUserIdAndPlantIdAndStatus(Long userId, Long plantId, PlantStatus status);
    
    // جلب نبتة مستخدم مع التفاصيل
    @Query("SELECT up FROM UserPlant up JOIN FETCH up.plant WHERE up.id = :id")
    Optional<UserPlant> findByIdWithPlant(@Param("id") Long id);
    
    // جلب نباتات مستخدم مع تفاصيل النبات
    @Query("SELECT up FROM UserPlant up JOIN FETCH up.plant WHERE up.user.id = :userId AND up.status = :status")
    List<UserPlant> findByUserIdAndStatusWithPlant(@Param("userId") Long userId, @Param("status") PlantStatus status);
    
    // عدد نباتات المستخدم بحالة معينة
    long countByUserIdAndStatus(Long userId, PlantStatus status);
}
