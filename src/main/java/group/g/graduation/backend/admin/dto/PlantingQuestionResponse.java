package group.g.graduation.backend.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for planting question response - استجابة سؤال الزراعة
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantingQuestionResponse {
    
    private Long id;
    
    private String questionTextAr;
    
    private String questionTextEn;
    
    private String questionKey;
    
    private Boolean isRequired;
    
    private Boolean allowMultiple;
    
    private Integer displayOrder;
    
    private Boolean isActive;
    
    private Integer optionsCount;
    
    private List<QuestionOptionResponse> options;
}
