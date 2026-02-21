package group.g.graduation.backend.user.service;

import group.g.graduation.backend.common.enums.QuoteCategory;
import group.g.graduation.backend.common.model.DailyQuote;
import group.g.graduation.backend.common.repository.DailyQuoteRepository;
import group.g.graduation.backend.user.dto.home.DailyQuoteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Daily Quote Service - خدمة المثل/التحفيز اليومي
 * تجلب اقتباس يومي يتغير كل يوم
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserDailyQuoteService {

    private final DailyQuoteRepository dailyQuoteRepository;

    /**
     * جلب اقتباس اليوم
     * الأولوية: 1. اقتباس محدد لتاريخ اليوم → 2. اقتباس عشوائي نشط
     */
    public DailyQuoteResponse getTodayQuote() {
        LocalDate today = LocalDate.now();

        // أولاً: نبحث عن اقتباس مخصص لتاريخ اليوم
        Optional<DailyQuote> todayQuote = dailyQuoteRepository.findTodayQuote(today);

        if (todayQuote.isPresent()) {
            log.debug("📜 Found today's quote (id: {})", todayQuote.get().getId());
            return toResponse(todayQuote.get());
        }

        // ثانياً: اقتباس عشوائي
        Optional<DailyQuote> randomQuote = dailyQuoteRepository.findRandomQuote();
        if (randomQuote.isPresent()) {
            log.debug("🎲 Using random quote (id: {})", randomQuote.get().getId());
            return toResponse(randomQuote.get());
        }

        // Fallback: اقتباس افتراضي
        log.info("📜 No quotes found in database, using default");
        return buildDefaultQuote();
    }

    /**
     * تحويل Entity إلى Response
     */
    private DailyQuoteResponse toResponse(DailyQuote quote) {
        return DailyQuoteResponse.builder()
                .id(quote.getId())
                .quoteTextAr(quote.getQuoteTextAr())
                .quoteTextEn(quote.getQuoteTextEn())
                .author(quote.getAuthor())
                .category(quote.getCategory())
                .categoryNameAr(getCategoryNameAr(quote.getCategory()))
                .build();
    }

    /**
     * اقتباس افتراضي عندما لا يوجد اقتباسات في قاعدة البيانات
     */
    private DailyQuoteResponse buildDefaultQuote() {
        return DailyQuoteResponse.builder()
                .id(0L)
                .quoteTextAr("اللي بزرع خير بحصد خير 🌱")
                .quoteTextEn("Who plants good, harvests good")
                .author("مثل شعبي")
                .category(QuoteCategory.PROVERB)
                .categoryNameAr("مثل شعبي")
                .build();
    }

    /**
     * اسم التصنيف بالعربي
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
