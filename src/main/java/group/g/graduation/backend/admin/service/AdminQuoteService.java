package group.g.graduation.backend.admin.service;

import group.g.graduation.backend.admin.dto.*;
import group.g.graduation.backend.common.enums.QuoteCategory;
import org.springframework.data.domain.Pageable;

/**
 * Admin Quote Service Interface - واجهة خدمة إدارة الاقتباسات
 */
public interface AdminQuoteService {
    
    // ===== CRUD Operations =====
    
    /**
     * إنشاء اقتباس جديد
     */
    QuoteResponse createQuote(QuoteCreateRequest request);
    
    /**
     * تعديل اقتباس
     */
    QuoteResponse updateQuote(Long id, QuoteUpdateRequest request);
    
    /**
     * حذف اقتباس
     */
    void deleteQuote(Long id);
    
    /**
     * جلب اقتباس بالمعرف
     */
    QuoteResponse getQuoteById(Long id);
    
    // ===== List & Filter Operations =====
    
    /**
     * جلب كل الاقتباسات مع الترقيم
     */
    QuoteListResponse getAllQuotes(Pageable pageable);
    
    /**
     * جلب الاقتباسات حسب التصنيف
     */
    QuoteListResponse getQuotesByCategory(QuoteCategory category, Pageable pageable);
    
    /**
     * جلب الاقتباسات النشطة فقط
     */
    QuoteListResponse getActiveQuotes(Pageable pageable);
    
    /**
     * البحث في الاقتباسات
     */
    QuoteListResponse searchQuotes(String keyword, Pageable pageable);
    
    // ===== Status Operations =====
    
    /**
     * تفعيل/تعطيل اقتباس
     */
    QuoteResponse toggleQuoteStatus(Long id);
}
