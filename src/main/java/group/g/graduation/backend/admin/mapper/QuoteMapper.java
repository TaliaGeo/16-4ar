package group.g.graduation.backend.admin.mapper;

import group.g.graduation.backend.admin.dto.QuoteCreateRequest;
import group.g.graduation.backend.admin.dto.QuoteResponse;
import group.g.graduation.backend.admin.dto.QuoteUpdateRequest;
import group.g.graduation.backend.common.enums.QuoteCategory;
import group.g.graduation.backend.common.model.DailyQuote;
import org.springframework.stereotype.Component;

/**
 * Mapper for DailyQuote entity - محوّل الاقتباسات اليومية
 */
@Component
public class QuoteMapper {
    
    /**
     * تحويل من CreateRequest إلى Entity
     */
    public DailyQuote toEntity(QuoteCreateRequest request) {
        DailyQuote quote = new DailyQuote();
        quote.setQuoteTextAr(request.getQuoteTextAr());
        quote.setQuoteTextEn(request.getQuoteTextEn());
        quote.setAuthor(request.getAuthor());
        quote.setCategory(request.getCategory());
        quote.setDisplayDate(request.getDisplayDate());
        quote.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        return quote;
    }
    
    /**
     * تحويل من Entity إلى Response
     */
    public QuoteResponse toResponse(DailyQuote quote) {
        return QuoteResponse.builder()
                .id(quote.getId())
                .quoteTextAr(quote.getQuoteTextAr())
                .quoteTextEn(quote.getQuoteTextEn())
                .author(quote.getAuthor())
                .category(quote.getCategory())
                .categoryNameAr(getCategoryNameAr(quote.getCategory()))
                .displayDate(quote.getDisplayDate())
                .isActive(quote.getIsActive())
                .build();
    }
    
    /**
     * تحديث Entity من UpdateRequest
     */
    public void updateEntity(DailyQuote quote, QuoteUpdateRequest request) {
        if (request.getQuoteTextAr() != null) {
            quote.setQuoteTextAr(request.getQuoteTextAr());
        }
        if (request.getQuoteTextEn() != null) {
            quote.setQuoteTextEn(request.getQuoteTextEn());
        }
        if (request.getAuthor() != null) {
            quote.setAuthor(request.getAuthor());
        }
        if (request.getCategory() != null) {
            quote.setCategory(request.getCategory());
        }
        if (request.getDisplayDate() != null) {
            quote.setDisplayDate(request.getDisplayDate());
        }
        if (request.getIsActive() != null) {
            quote.setIsActive(request.getIsActive());
        }
    }
    
    /**
     * الحصول على اسم التصنيف بالعربي
     */
    private String getCategoryNameAr(QuoteCategory category) {
        if (category == null) return null;
        return switch (category) {
            case PROVERB -> "مثل شعبي";
            case MOTIVATION -> "تحفيز";
            case WISDOM -> "حكمة";
            case TIP -> "نصيحة زراعية";
        };
    }
}
