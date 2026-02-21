package group.g.graduation.backend.user.dto.home;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Home Page Response DTO - استجابة الصفحة الرئيسية الكاملة
 * يجمع كل بيانات الهوم بيج في استجابة واحدة
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "استجابة الصفحة الرئيسية الكاملة")
public class HomePageResponse {

    @Schema(description = "حالة الطقس اليوم")
    private WeatherResponse weather;

    @Schema(description = "المثل/التحفيز اليومي")
    private DailyQuoteResponse dailyQuote;

    @Schema(description = "التقويم الزراعي الشهري")
    private MonthlyCalendarResponse calendar;
}
