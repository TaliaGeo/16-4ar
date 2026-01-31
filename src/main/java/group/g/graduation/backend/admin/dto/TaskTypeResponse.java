package group.g.graduation.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for task type response - استجابة نوع المهمة
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "استجابة نوع المهمة")
public class TaskTypeResponse {
    
    @Schema(description = "معرّف نوع المهمة", example = "1")
    private Long id;
    
    @Schema(description = "اسم المهمة بالعربي", example = "ري")
    private String nameAr;
    
    @Schema(description = "اسم المهمة بالإنجليزي", example = "Watering")
    private String nameEn;
    
    @Schema(description = "أيقونة المهمة", example = "💧")
    private String icon;
    
    @Schema(description = "وصف المهمة بالعربي")
    private String descriptionAr;
    
    @Schema(description = "وصف المهمة بالإنجليزي")
    private String descriptionEn;
    
    @Schema(description = "عدد النباتات المرتبطة", example = "15")
    private Integer plantsCount;
}
