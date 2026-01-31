package group.g.graduation.backend.admin.dto;

import group.g.graduation.backend.common.enums.QuoteCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for daily quote response - استجابة الاقتباس اليومي
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "استجابة الاقتباس اليومي")
public class QuoteResponse {
    
    @Schema(description = "معرّف الاقتباس", example = "1")
    private Long id;
    
    @Schema(description = "نص الاقتباس بالعربي", example = "ازرع اليوم لتحصد غداً")
    private String quoteTextAr;
    
    @Schema(description = "نص الاقتباس بالإنجليزي", example = "Plant today to harvest tomorrow")
    private String quoteTextEn;
    
    @Schema(description = "المصدر أو القائل", example = "مثل شعبي فلسطيني")
    private String author;
    
    @Schema(description = "تصنيف الاقتباس", example = "PROVERB")
    private QuoteCategory category;
    
    @Schema(description = "اسم التصنيف بالعربي", example = "مثل شعبي")
    private String categoryNameAr;
    
    @Schema(description = "تاريخ العرض المحدد", example = "2026-03-21")
    private LocalDate displayDate;
    
    @Schema(description = "هل الاقتباس مفعّل", example = "true")
    private Boolean isActive;
}
