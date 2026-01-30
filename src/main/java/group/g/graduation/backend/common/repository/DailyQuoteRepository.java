package group.g.graduation.backend.common.repository;

import group.g.graduation.backend.common.enums.QuoteCategory;
import group.g.graduation.backend.common.model.DailyQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyQuoteRepository extends JpaRepository<DailyQuote, Long> {
    
    // جلب الأقوال النشطة
    List<DailyQuote> findByIsActiveTrue();
    
    // جلب أقوال بتصنيف معين
    List<DailyQuote> findByCategoryAndIsActiveTrue(QuoteCategory category);
    
    // جلب قول ليوم معين
    Optional<DailyQuote> findByDisplayDateAndIsActiveTrue(LocalDate displayDate);
    
    // جلب قول عشوائي
    @Query(value = "SELECT * FROM daily_quotes WHERE is_active = true AND display_date IS NULL ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<DailyQuote> findRandomQuote();
    
    // جلب قول اليوم (أولاً المحدد لهذا اليوم، ثم عشوائي)
    @Query(value = "SELECT * FROM daily_quotes WHERE is_active = true AND (display_date = :today OR display_date IS NULL) ORDER BY display_date DESC NULLS LAST LIMIT 1", nativeQuery = true)
    Optional<DailyQuote> findTodayQuote(LocalDate today);
}
