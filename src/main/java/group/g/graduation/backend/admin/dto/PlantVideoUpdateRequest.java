package group.g.graduation.backend.admin.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating plant video URL - طلب تحديث رابط فيديو النبتة
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantVideoUpdateRequest {
    
    @Size(max = 500, message = "رابط الفيديو يجب أن لا يتجاوز 500 حرف")
    private String plantingVideoUrl;
}
