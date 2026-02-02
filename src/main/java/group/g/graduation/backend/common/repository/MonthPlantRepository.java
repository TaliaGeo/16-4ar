package group.g.graduation.backend.common.repository;

import group.g.graduation.backend.common.model.Month;
import group.g.graduation.backend.common.model.MonthPlant;
import group.g.graduation.backend.common.model.Plant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonthPlantRepository extends JpaRepository<MonthPlant, Long> {
    
    // جلب النباتات المناسبة لشهر معين
    List<MonthPlant> findByMonth(Month month);
    
    List<MonthPlant> findByMonthId(Long monthId);
    
    // جلب الأشهر المناسبة لنبتة معينة
    List<MonthPlant> findByPlant(Plant plant);
    
    List<MonthPlant> findByPlantId(Long plantId);
    
    // التحقق من وجود علاقة
    boolean existsByMonthIdAndPlantId(Long monthId, Long plantId);
    
    // جلب علاقة معينة بحسب معرف الشهر والنبتة
    Optional<MonthPlant> findByMonthIdAndPlantId(Long monthId, Long plantId);
    
    // جلب علاقة معينة بتفاصيل الشهر والنبتة
    @Query("SELECT mp FROM MonthPlant mp JOIN FETCH mp.month JOIN FETCH mp.plant WHERE mp.id = :id")
    Optional<MonthPlant> findByIdWithDetails(@Param("id") Long id);
    
    // جلب النباتات لشهر مع تفاصيل النبات
    @Query("SELECT mp FROM MonthPlant mp JOIN FETCH mp.plant WHERE mp.month.id = :monthId")
    List<MonthPlant> findByMonthIdWithPlantDetails(@Param("monthId") Long monthId);
}
