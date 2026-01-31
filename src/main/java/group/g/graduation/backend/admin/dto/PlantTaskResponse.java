package group.g.graduation.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for plant task response - استجابة مهمة النبتة
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "استجابة مهمة النبتة")
public class PlantTaskResponse {
    
    @Schema(description = "معرّف المهمة", example = "1")
    private Long id;
    
    @Schema(description = "معرّف النبتة", example = "1")
    private Long plantId;
    
    @Schema(description = "اسم النبتة بالعربي", example = "نعناع")
    private String plantNameAr;
    
    @Schema(description = "معرّف نوع المهمة", example = "1")
    private Long taskTypeId;
    
    @Schema(description = "اسم المهمة بالعربي", example = "ري")
    private String taskTypeNameAr;
    
    @Schema(description = "اسم المهمة بالإنجليزي", example = "Watering")
    private String taskTypeNameEn;
    
    @Schema(description = "أيقونة المهمة", example = "💧")
    private String taskTypeIcon;
    
    @Schema(description = "فترة تكرار المهمة بالأيام", example = "3")
    private Integer intervalDays;
    
    @Schema(description = "وصف المهمة بالعربي")
    private String descriptionAr;
    
    @Schema(description = "وصف المهمة بالإنجليزي")
    private String descriptionEn;
    
    @Schema(description = "تبدأ بعد كم يوم من الزراعة", example = "0")
    private Integer startDayAfterPlanting;
    
    @Schema(description = "هل تتكرر", example = "true")
    private Boolean isRecurring;
}
