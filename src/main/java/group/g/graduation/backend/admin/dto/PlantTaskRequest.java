package group.g.graduation.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating/updating plant task - طلب إنشاء/تعديل مهمة نبتة
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "طلب إنشاء/تعديل مهمة نبتة")
public class PlantTaskRequest {
    
    @NotNull(message = "معرّف نوع المهمة مطلوب")
    @Schema(description = "معرّف نوع المهمة", example = "1", required = true)
    private Long taskTypeId;
    
    @Min(value = 1, message = "فترة التكرار يجب أن تكون يوم واحد على الأقل")
    @Schema(description = "فترة تكرار المهمة بالأيام", example = "3")
    private Integer intervalDays;
    
    @Size(max = 500, message = "الوصف يجب أن لا يتجاوز 500 حرف")
    @Schema(description = "وصف المهمة بالعربي", example = "اسق النبتة حتى تصبح التربة رطبة")
    private String descriptionAr;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Schema(description = "وصف المهمة بالإنجليزي", example = "Water the plant until soil is moist")
    private String descriptionEn;
    
    @Min(value = 0, message = "يوم البدء يجب أن يكون 0 أو أكثر")
    @Schema(description = "تبدأ المهمة بعد كم يوم من الزراعة", example = "0")
    private Integer startDayAfterPlanting;
    
    @Schema(description = "هل تتكرر المهمة", example = "true")
    private Boolean isRecurring;
}
