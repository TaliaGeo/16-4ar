package group.g.graduation.backend.admin.dto;

import group.g.graduation.backend.common.enums.QuoteCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for creating a new daily quote - طلب إنشاء اقتباس يومي جديد
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "طلب إنشاء اقتباس يومي جديد")
public class QuoteCreateRequest {
    
    @NotBlank(message = "نص الاقتباس بالعربي مطلوب")
    @Size(max = 1000, message = "نص الاقتباس يجب أن لا يتجاوز 1000 حرف")
    @Schema(description = "نص الاقتباس بالعربي", example = "ازرع اليوم لتحصد غداً", required = true)
    private String quoteTextAr;
    
    @Size(max = 1000, message = "Quote text must not exceed 1000 characters")
    @Schema(description = "نص الاقتباس بالإنجليزي", example = "Plant today to harvest tomorrow")
    private String quoteTextEn;
    
    @Size(max = 200, message = "اسم المؤلف يجب أن لا يتجاوز 200 حرف")
    @Schema(description = "المصدر أو القائل", example = "مثل شعبي فلسطيني")
    private String author;
    
    @NotNull(message = "تصنيف الاقتباس مطلوب")
    @Schema(description = "تصنيف الاقتباس", example = "PROVERB")
    private QuoteCategory category;
    
    @Schema(description = "تاريخ العرض المحدد (اختياري - null للعشوائي)", example = "2026-03-21")
    private LocalDate displayDate;
    
    @Schema(description = "هل الاقتباس مفعّل", example = "true", defaultValue = "true")
    private Boolean isActive = true;
}
