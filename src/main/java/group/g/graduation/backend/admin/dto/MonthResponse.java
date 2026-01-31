package group.g.graduation.backend.admin.dto;

import group.g.graduation.backend.common.enums.Season;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for month response - استجابة الشهر
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "استجابة الشهر")
public class MonthResponse {
    
    @Schema(description = "معرّف الشهر", example = "1")
    private Long id;
    
    @Schema(description = "رقم الشهر (1-12)", example = "3")
    private Integer monthNumber;
    
    @Schema(description = "اسم الشهر بالعربي", example = "آذار")
    private String nameAr;
    
    @Schema(description = "اسم الشهر بالإنجليزي", example = "March")
    private String nameEn;
    
    @Schema(description = "الفصل", example = "SPRING")
    private Season season;
    
    @Schema(description = "اسم الفصل بالعربي", example = "ربيع")
    private String seasonNameAr;
    
    @Schema(description = "وصف حالة الطقس بالعربي")
    private String weatherDescriptionAr;
    
    @Schema(description = "وصف حالة الطقس بالإنجليزي")
    private String weatherDescriptionEn;
    
    @Schema(description = "رابط الصورة")
    private String imageUrl;
    
    @Schema(description = "عدد النباتات المناسبة لهذا الشهر", example = "15")
    private Integer plantsCount;
    
    @Schema(description = "قائمة النباتات المناسبة لهذا الشهر (ملخص)")
    private List<MonthPlantSummary> plants;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthPlantSummary {
        private Long plantId;
        private String plantNameAr;
        private String plantNameEn;
        private String plantingNoteAr;
    }
}
