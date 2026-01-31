package group.g.graduation.backend.admin.dto;

import group.g.graduation.backend.common.enums.QuoteCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for updating a daily quote - طلب تعديل اقتباس يومي
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "طلب تعديل اقتباس يومي")
public class QuoteUpdateRequest {
    
    @Size(max = 1000, message = "نص الاقتباس يجب أن لا يتجاوز 1000 حرف")
    @Schema(description = "نص الاقتباس بالعربي", example = "ازرع اليوم لتحصد غداً")
    private String quoteTextAr;
    
    @Size(max = 1000, message = "Quote text must not exceed 1000 characters")
    @Schema(description = "نص الاقتباس بالإنجليزي", example = "Plant today to harvest tomorrow")
    private String quoteTextEn;
    
    @Size(max = 200, message = "اسم المؤلف يجب أن لا يتجاوز 200 حرف")
    @Schema(description = "المصدر أو القائل", example = "مثل شعبي فلسطيني")
    private String author;
    
    @Schema(description = "تصنيف الاقتباس", example = "PROVERB")
    private QuoteCategory category;
    
    @Schema(description = "تاريخ العرض المحدد (اختياري - null للعشوائي)", example = "2026-03-21")
    private LocalDate displayDate;
    
    @Schema(description = "هل الاقتباس مفعّل", example = "true")
    private Boolean isActive;
}
