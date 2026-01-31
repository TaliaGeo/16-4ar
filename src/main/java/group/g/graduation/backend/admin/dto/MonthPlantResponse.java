package group.g.graduation.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for month-plant relation response - استجابة علاقة الشهر بالنبتة
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "استجابة علاقة الشهر بالنبتة")
public class MonthPlantResponse {
    
    @Schema(description = "معرّف العلاقة", example = "1")
    private Long id;
    
    @Schema(description = "معرّف الشهر", example = "3")
    private Long monthId;
    
    @Schema(description = "رقم الشهر", example = "3")
    private Integer monthNumber;
    
    @Schema(description = "اسم الشهر بالعربي", example = "آذار")
    private String monthNameAr;
    
    @Schema(description = "اسم الشهر بالإنجليزي", example = "March")
    private String monthNameEn;
    
    @Schema(description = "معرّف النبتة", example = "1")
    private Long plantId;
    
    @Schema(description = "اسم النبتة بالعربي", example = "نعناع بستاني")
    private String plantNameAr;
    
    @Schema(description = "اسم النبتة بالإنجليزي", example = "Spearmint")
    private String plantNameEn;
    
    @Schema(description = "ملاحظة الزراعة بالعربي")
    private String plantingNoteAr;
    
    @Schema(description = "ملاحظة الزراعة بالإنجليزي")
    private String plantingNoteEn;
}
