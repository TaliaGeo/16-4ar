package group.g.graduation.backend.admin.mapper;

import group.g.graduation.backend.admin.dto.PlantSuitabilityRequest;
import group.g.graduation.backend.admin.dto.PlantSuitabilityResponse;
import group.g.graduation.backend.common.model.Plant;
import group.g.graduation.backend.common.model.PlantSuitability;
import group.g.graduation.backend.common.model.QuestionOption;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for PlantSuitability - محوّل ملاءمة النباتات
 */
@Component
public class PlantSuitabilityMapper {
    
    /**
     * Convert PlantSuitability entity to PlantSuitabilityResponse DTO
     */
    public PlantSuitabilityResponse toResponse(PlantSuitability entity) {
        if (entity == null) return null;
        
        PlantSuitabilityResponse.PlantSuitabilityResponseBuilder builder = PlantSuitabilityResponse.builder()
                .id(entity.getId())
                .score(entity.getScore())
                .scoreLevel(getScoreLevel(entity.getScore()))
                .adjustmentTipAr(entity.getAdjustmentTipAr())
                .adjustmentTipEn(entity.getAdjustmentTipEn());
        
        // Plant info
        try {
            Plant plant = entity.getPlant();
            if (plant != null) {
                builder.plantId(plant.getId())
                       .plantNameAr(plant.getNameAr())
                       .plantNameEn(plant.getNameEn());
            }
        } catch (Exception e) {
            // LazyInitializationException
        }
        
        // Option info
        try {
            QuestionOption option = entity.getOption();
            if (option != null) {
                builder.optionId(option.getId())
                       .optionKey(option.getOptionKey())
                       .optionTextAr(option.getOptionTextAr())
                       .optionTextEn(option.getOptionTextEn());
                
                // Question info
                try {
                    if (option.getQuestion() != null) {
                        builder.questionId(option.getQuestion().getId())
                               .questionKey(option.getQuestion().getQuestionKey())
                               .questionTextAr(option.getQuestion().getQuestionTextAr());
                    }
                } catch (Exception e) {
                    // LazyInitializationException for nested question
                }
            }
        } catch (Exception e) {
            // LazyInitializationException
        }
        
        return builder.build();
    }
    
    /**
     * Convert PlantSuitabilityRequest DTO to PlantSuitability entity
     * Note: Plant and Option must be set separately
     */
    public PlantSuitability toEntity(PlantSuitabilityRequest request) {
        if (request == null) return null;
        
        PlantSuitability entity = new PlantSuitability();
        entity.setScore(request.getScore());
        entity.setAdjustmentTipAr(request.getAdjustmentTipAr());
        entity.setAdjustmentTipEn(request.getAdjustmentTipEn());
        
        return entity;
    }
    
    /**
     * Update PlantSuitability entity from PlantSuitabilityRequest DTO
     */
    public void updateEntity(PlantSuitability entity, PlantSuitabilityRequest request) {
        if (entity == null || request == null) return;
        
        if (request.getScore() != null) {
            entity.setScore(request.getScore());
        }
        if (request.getAdjustmentTipAr() != null) {
            entity.setAdjustmentTipAr(request.getAdjustmentTipAr());
        }
        if (request.getAdjustmentTipEn() != null) {
            entity.setAdjustmentTipEn(request.getAdjustmentTipEn());
        }
    }
    
    /**
     * Convert list of PlantSuitability entities to PlantSuitabilityResponse DTOs
     */
    public List<PlantSuitabilityResponse> toResponseList(List<PlantSuitability> entities) {
        if (entities == null) return Collections.emptyList();
        
        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get score level text based on score value
     */
    public String getScoreLevel(Integer score) {
        if (score == null) return "unknown";
        if (score >= 80) return "excellent";  // ممتاز
        if (score >= 60) return "good";       // جيد
        if (score >= 40) return "fair";       // مقبول
        return "poor";                         // ضعيف
    }
    
    /**
     * Get score level in Arabic
     */
    public String getScoreLevelAr(Integer score) {
        if (score == null) return "غير معروف";
        if (score >= 80) return "ممتاز";
        if (score >= 60) return "جيد";
        if (score >= 40) return "مقبول";
        return "ضعيف";
    }
}
