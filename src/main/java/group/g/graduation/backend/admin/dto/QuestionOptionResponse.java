package group.g.graduation.backend.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for question option response - استجابة خيار السؤال
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionOptionResponse {
    
    private Long id;
    
    private Long questionId;
    
    private String optionTextAr;
    
    private String optionTextEn;
    
    private String optionKey;
    
    private Integer displayOrder;
    
    private Boolean isActive;
}
