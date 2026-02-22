package group.g.graduation.backend.user.dto.home;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Weather Response DTO - استجابة حالة الطقس
 * يعرض حالة الطقس اليومية مع صورة تعبيرية
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "استجابة حالة الطقس اليومية")
public class WeatherResponse {

    @Schema(description = "درجة الحرارة الحالية بالمئوية", example = "25")
    private Double temperature;

    @Schema(description = "أعلى درجة حرارة اليوم", example = "30")
    private Double tempMax;

    @Schema(description = "أدنى درجة حرارة اليوم", example = "18")
    private Double tempMin;

    @Schema(description = "نسبة الرطوبة %", example = "45")
    private Integer humidity;

    @Schema(description = "وصف حالة الطقس بالعربي", example = "مشمس")
    private String descriptionAr;

    @Schema(description = "وصف حالة الطقس بالانجليزي", example = "Sunny")
    private String descriptionEn;

    @Schema(description = "أيقونة حالة الطقس (رمز)", example = "01d")
    private String weatherIcon;

    @Schema(description = "رابط صورة تعبيرية عن حالة الطقس", example = "https://openweathermap.org/img/wn/01d@4x.png")
    private String weatherImageUrl;

    @Schema(description = "اسم اليوم بالعربي", example = "السبت")
    private String dayNameAr;

    @Schema(description = "اسم اليوم بالانجليزي", example = "Saturday")
    private String dayNameEn;

    @Schema(description = "التاريخ بالتنسيق المقروء", example = "21 فبراير 2026")
    private String dateFormatted;

    @Schema(description = "اسم المدينة/الموقع بالعربي", example = "عمّان")
    private String locationAr;

    @Schema(description = "اسم المدينة/الموقع بالانجليزي", example = "Amman")
    private String locationEn;

    @Schema(description = "سرعة الرياح (كم/ساعة)", example = "15")
    private Double windSpeed;

    @Schema(description = "اسم الفصل الحالي بالعربي", example = "شتاء")
    private String seasonAr;

    @Schema(description = "اسم الفصل الحالي بالانجليزي", example = "Winter")
    private String seasonEn;

    @Schema(description = "هل البيانات تقريبية (true) أم حقيقية من API (false)", example = "false")
    private Boolean isEstimated;
}
