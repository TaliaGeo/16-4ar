package group.g.graduation.backend.user.dto.home;

import group.g.graduation.backend.common.enums.QuoteCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Daily Quote Response DTO - استجابة المثل/التحفيز اليومي
 * مثل شعبي أو تحفيز يومي يتغير كل يوم
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "استجابة المثل أو التحفيز اليومي")
public class DailyQuoteResponse {

    @Schema(description = "معرف الاقتباس", example = "1")
    private Long id;

    @Schema(description = "نص الاقتباس بالعربي", example = "اللي بزرع خير بحصد خير")
    private String quoteTextAr;

    @Schema(description = "نص الاقتباس بالانجليزي", example = "Who plants good, harvests good")
    private String quoteTextEn;

    @Schema(description = "المؤلف أو المصدر", example = "مثل شعبي فلسطيني")
    private String author;

    @Schema(description = "تصنيف الاقتباس", example = "PROVERB")
    private QuoteCategory category;

    @Schema(description = "اسم التصنيف بالعربي", example = "مثل شعبي")
    private String categoryNameAr;
}
