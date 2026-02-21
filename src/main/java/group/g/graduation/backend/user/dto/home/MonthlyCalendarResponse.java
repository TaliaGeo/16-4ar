package group.g.graduation.backend.user.dto.home;

import group.g.graduation.backend.common.enums.Season;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Monthly Calendar Response DTO - استجابة الكالندر الزراعية
 * يعرض الأشهر مع وصف حالة الطقس ونباتات كل شهر
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "استجابة التقويم الزراعي الشهري")
public class MonthlyCalendarResponse {

    @Schema(description = "قائمة الأشهر الـ 12 مع معلوماتها")
    private List<CalendarMonth> months;

    @Schema(description = "الشهر الحالي", example = "2")
    private Integer currentMonth;

    @Schema(description = "السنة الحالية", example = "2026")
    private Integer currentYear;

    // ===== الشهر في الكالندر =====
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "بيانات شهر واحد في الكالندر")
    public static class CalendarMonth {

        @Schema(description = "معرف الشهر", example = "1")
        private Long id;

        @Schema(description = "رقم الشهر (1-12)", example = "3")
        private Integer monthNumber;

        @Schema(description = "اسم الشهر بالعربي", example = "آذار")
        private String nameAr;

        @Schema(description = "اسم الشهر بالانجليزي", example = "March")
        private String nameEn;

        @Schema(description = "الفصل", example = "SPRING")
        private Season season;

        @Schema(description = "اسم الفصل بالعربي (كلمة أو كلمتين)", example = "ربيع")
        private String seasonNameAr;

        @Schema(description = "وصف حالة الطقس بالعربي (كلمة أو كلمتين)", example = "معتدل دافئ")
        private String weatherDescriptionAr;

        @Schema(description = "وصف حالة الطقس بالانجليزي", example = "Mild warm")
        private String weatherDescriptionEn;

        @Schema(description = "صورة الشهر", example = "/uploads/months/march.jpg")
        private String imageUrl;

        @Schema(description = "عدد النباتات المقترحة لهذا الشهر", example = "8")
        private Integer plantsCount;

        @Schema(description = "هل هذا الشهر الحالي؟", example = "true")
        private Boolean isCurrent;
    }
}
