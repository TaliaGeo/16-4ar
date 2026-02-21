package group.g.graduation.backend.user.dto.home;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Month Detail Response DTO - تفاصيل الشهر عند الضغط عليه
 * يعرض اقتراحات النباتات لشهر محدد مع تفاصيل كل نبتة
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "تفاصيل شهر محدد مع اقتراحات النباتات")
public class MonthDetailResponse {

    @Schema(description = "رقم الشهر", example = "3")
    private Integer monthNumber;

    @Schema(description = "اسم الشهر بالعربي", example = "آذار")
    private String monthNameAr;

    @Schema(description = "اسم الشهر بالانجليزي", example = "March")
    private String monthNameEn;

    @Schema(description = "اسم الفصل بالعربي", example = "ربيع")
    private String seasonNameAr;

    @Schema(description = "وصف حالة الطقس", example = "معتدل دافئ")
    private String weatherDescriptionAr;

    @Schema(description = "المنطقة/الموقع", example = "فلسطين - مناخ البحر المتوسط")
    private String regionAr;

    @Schema(description = "المنطقة/الموقع بالانجليزي", example = "Palestine - Mediterranean climate")
    private String regionEn;

    @Schema(description = "عدد النباتات المقترحة", example = "5")
    private Integer plantsCount;

    @Schema(description = "قائمة النباتات المقترحة لهذا الشهر")
    private List<MonthPlantSuggestion> plants;

    // ===== نبتة مقترحة للشهر =====
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "نبتة مقترحة للزراعة في الشهر")
    public static class MonthPlantSuggestion {

        @Schema(description = "معرف النبتة", example = "1")
        private Long plantId;

        @Schema(description = "اسم النبتة بالعربي", example = "النعناع")
        private String nameAr;

        @Schema(description = "اسم النبتة بالانجليزي", example = "Mint")
        private String nameEn;

        @Schema(description = "وصف مختصر بالعربي (كلمتين)", example = "عشبة عطرية")
        private String shortDescriptionAr;

        @Schema(description = "وصف مختصر بالانجليزي", example = "Aromatic herb")
        private String shortDescriptionEn;

        @Schema(description = "ملخص عن النبتة بالعربي", example = "نبتة سهلة الزراعة، تحتاج ضوء جزئي ورطوبة معتدلة")
        private String summaryAr;

        @Schema(description = "ملخص عن النبتة بالانجليزي", example = "Easy to grow plant, needs partial light and moderate moisture")
        private String summaryEn;

        @Schema(description = "رابط الصورة الرئيسية للنبتة")
        private String primaryImageUrl;

        @Schema(description = "ملاحظة الزراعة لهذا الشهر بالعربي", example = "أفضل وقت لزراعة النعناع في الأصص")
        private String plantingNoteAr;

        @Schema(description = "ملاحظة الزراعة بالانجليزي")
        private String plantingNoteEn;

        @Schema(description = "مستوى الصعوبة", example = "EASY")
        private String difficultyLevel;

        @Schema(description = "تصنيف النبتة", example = "HERBS")
        private String category;
    }
}
