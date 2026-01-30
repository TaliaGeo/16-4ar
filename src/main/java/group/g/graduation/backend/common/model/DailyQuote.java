package group.g.graduation.backend.common.model;

import group.g.graduation.backend.common.enums.QuoteCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DailyQuote entity - الأقوال والتحفيز اليومي
 * أمثال شعبية، تحفيز، حكم، نصائح زراعية
 */
@Entity
@Table(name = "daily_quotes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyQuote {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String quoteTextAr;  // النص بالعربي
    
    @Column(columnDefinition = "TEXT")
    private String quoteTextEn;  // النص بالإنجليزي
    
    private String author;  // المصدر أو القائل (اختياري)
    
    @Enumerated(EnumType.STRING)
    private QuoteCategory category;  // نوع القول
    
    private LocalDate displayDate;  // تاريخ عرض محدد (null = عشوائي)
    
    private Boolean isActive = true;  // هل مفعّل
}
