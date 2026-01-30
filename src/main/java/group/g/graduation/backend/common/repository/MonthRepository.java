package group.g.graduation.backend.common.repository;

import group.g.graduation.backend.common.enums.Season;
import group.g.graduation.backend.common.model.Month;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonthRepository extends JpaRepository<Month, Long> {
    
    // جلب شهر برقمه
    Optional<Month> findByMonthNumber(Integer monthNumber);
    
    // جلب أشهر فصل معين
    List<Month> findBySeason(Season season);
    
    // جلب كل الأشهر مرتبة
    List<Month> findAllByOrderByMonthNumberAsc();
    
    // جلب شهر مع النباتات المناسبة له
    @Query("SELECT DISTINCT m FROM Month m LEFT JOIN FETCH m.monthPlants mp LEFT JOIN FETCH mp.plant WHERE m.monthNumber = :monthNumber")
    Optional<Month> findByMonthNumberWithPlants(@Param("monthNumber") Integer monthNumber);
}
