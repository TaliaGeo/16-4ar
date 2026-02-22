package group.g.graduation.backend.user.dto.crop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * معلومات النبتة المزروعة - التبويب الثالث
 * مطابق لتصميم الشاشة: 4 أقسام فرعية (معلومات، العناية، الحصاد، الاستخدامات)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantedInfoResponse {
    private Long userPlantId;
    private Long plantId;
    private String plantNameAr;
    private String plantNameEn;
    private String nameScientific;
    private String imageUrl;

    // ===== قسم 1: معلومات النبتة =====
    private String shortDescriptionAr;     // ملخص قصير عن النبتة
    private String shortDescriptionEn;
    private String lightInfoAr;            // معلومات الضوء
    private String lightInfoEn;
    private String soilInfoAr;             // معلومات التربة
    private String soilInfoEn;
    private String wateringInfoAr;         // معلومات الري
    private String wateringInfoEn;
    private String difficultyLevel;
    private String category;
    private Integer wateringIntervalDays;
    private Integer minTemp;
    private Integer maxTemp;

    // ===== قسم 2: العناية =====
    private String careInfoAr;             // معلومات عامة عن الرعاية
    private String careInfoEn;
    private String plantingStepsAr;        // خطوات الزراعة
    private String plantingStepsEn;
    private String plantingVideoUrl;       // فيديو الزراعة
    private Integer spacingCm;
    private Integer germinationDays;

    // ===== قسم 3: الحصاد =====
    private String harvestInfoAr;          // متى وكيف أحصد
    private String harvestInfoEn;
    private Integer daysToHarvest;
    private String expectedHarvestDateAr;  // "بعد 45 يوم تقريباً"
    private String expectedHarvestDateEn;  // "Approximately in 45 days"

    // ===== قسم 4: الاستخدامات =====
    private String usesInfoAr;
    private String usesInfoEn;
    private List<String> useTagsAr;        // تاغات مثل ["تعم خفيف للهضم", "مهدئ ومساعد على الاسترخاء"]
    private List<String> useTagsEn;

    // ملاحظة تنبيه
    private String disclaimerAr;           // "ملاحظة: هذه المعلومات للتثقيف فقط..."
    private String disclaimerEn;

    // أشهر الزراعة
    private List<PlantingMonthInfo> plantingMonths;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlantingMonthInfo {
        private Integer monthNumber;
        private String monthNameAr;
        private String monthNameEn;
        private String plantingNoteAr;
        private String plantingNoteEn;
    }
}
