package group.g.graduation.backend.admin.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for reordering plant images - طلب إعادة ترتيب صور النبتة
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageReorderRequest {
    
    @NotEmpty(message = "قائمة معرفات الصور مطلوبة")
    private List<Long> imageIds;  // الصور بالترتيب الجديد
}
