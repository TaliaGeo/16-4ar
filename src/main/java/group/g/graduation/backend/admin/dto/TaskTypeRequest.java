package group.g.graduation.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating/updating task type - طلب إنشاء/تعديل نوع مهمة
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "طلب إنشاء/تعديل نوع مهمة")
public class TaskTypeRequest {
    
    @NotBlank(message = "اسم المهمة بالعربي مطلوب")
    @Size(max = 100, message = "اسم المهمة يجب أن لا يتجاوز 100 حرف")
    @Schema(description = "اسم المهمة بالعربي", example = "ري", required = true)
    private String nameAr;
    
    @Size(max = 100, message = "Task name must not exceed 100 characters")
    @Schema(description = "اسم المهمة بالإنجليزي", example = "Watering")
    private String nameEn;
    
    @Size(max = 10, message = "الأيقونة يجب أن لا تتجاوز 10 أحرف")
    @Schema(description = "أيقونة المهمة", example = "💧")
    private String icon;
    
    @Size(max = 500, message = "الوصف يجب أن لا يتجاوز 500 حرف")
    @Schema(description = "وصف المهمة بالعربي", example = "سقاية النبتة بالماء")
    private String descriptionAr;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Schema(description = "وصف المهمة بالإنجليزي", example = "Watering the plant")
    private String descriptionEn;
}
