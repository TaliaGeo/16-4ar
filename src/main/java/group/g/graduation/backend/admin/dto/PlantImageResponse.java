package group.g.graduation.backend.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for plant image response - رد معلومات صورة النبتة
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantImageResponse {
    
    private Long id;
    
    private Long plantId;
    
    private String plantNameAr;
    
    private String imageUrl;
    
    private String altTextAr;
    
    private String altTextEn;
    
    private Boolean isPrimary;
    
    private Integer displayOrder;
}
