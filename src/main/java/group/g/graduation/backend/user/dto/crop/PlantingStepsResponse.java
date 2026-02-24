package group.g.graduation.backend.user.dto.crop;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * خطوات الزراعة - تظهر بصفحة منفصلة عند الضغط على "ابدأ الزراعة"
 * Planting Steps - shown on a separate page when user clicks "Start Planting"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "خطوات زراعة النبتة - تظهر بصفحة لحالها")
public class PlantingStepsResponse {

    @Schema(description = "معرّف نبتة المستخدم")
    private Long userPlantId;

    @Schema(description = "معرّف النبتة")
    private Long plantId;

    @Schema(description = "اسم النبتة بالعربي")
    private String plantNameAr;

    @Schema(description = "اسم النبتة بالإنجليزي")
    private String plantNameEn;

    @Schema(description = "صورة النبتة الرئيسية")
    private String imageUrl;

    @Schema(description = "خطوات الزراعة بالعربي")
    private String plantingStepsAr;

    @Schema(description = "خطوات الزراعة بالإنجليزي")
    private String plantingStepsEn;

    @Schema(description = "رابط فيديو الزراعة")
    private String plantingVideoUrl;

    @Schema(description = "مستوى الصعوبة")
    private String difficultyLevel;

    @Schema(description = "المسافة بين النباتات بالسنتيمتر")
    private Integer spacingCm;

    @Schema(description = "أيام الإنبات")
    private Integer germinationDays;

    @Schema(description = "أقل درجة حرارة")
    private Integer minTemp;

    @Schema(description = "أعلى درجة حرارة")
    private Integer maxTemp;

    @Schema(description = "اللقب الذي أعطاه المستخدم")
    private String nickname;
}
