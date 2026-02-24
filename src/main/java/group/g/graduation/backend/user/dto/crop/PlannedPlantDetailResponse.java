package group.g.graduation.backend.user.dto.crop;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * تفاصيل النبتة المخططة - قبل الزراعة
 * يظهر عند الضغط على نبتة في قسم "مخطط لزراعتها"
 * ملاحظة: خطوات الزراعة نقلت لإندبوينت منفصل: GET /{id}/planting-steps
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "تفاصيل النبتة المخططة (بدون خطوات الزراعة - لها صفحة لوحدها)")
public class PlannedPlantDetailResponse {
    private Long userPlantId;
    private Long plantId;
    private String plantNameAr;
    private String plantNameEn;
    private String nameScientific;
    private String imageUrl;
    private List<String> allImageUrls;

    // معلومات الزراعة
    private String lightInfoAr;
    private String lightInfoEn;
    private String soilInfoAr;
    private String soilInfoEn;
    private String wateringInfoAr;
    private String wateringInfoEn;

    // معلومات إضافية
    private String careInfoAr;
    private String careInfoEn;
    private String difficultyLevel;
    private String category;
    private Integer wateringIntervalDays;
    private Integer daysToHarvest;
    private String nickname;
}
