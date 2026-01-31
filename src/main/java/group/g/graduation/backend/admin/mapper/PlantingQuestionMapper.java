package group.g.graduation.backend.admin.mapper;

import group.g.graduation.backend.admin.dto.PlantingQuestionRequest;
import group.g.graduation.backend.admin.dto.PlantingQuestionResponse;
import group.g.graduation.backend.admin.dto.QuestionOptionRequest;
import group.g.graduation.backend.admin.dto.QuestionOptionResponse;
import group.g.graduation.backend.common.model.PlantingQuestion;
import group.g.graduation.backend.common.model.QuestionOption;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for PlantingQuestion and QuestionOption - محوّل الأسئلة والخيارات
 */
@Component
public class PlantingQuestionMapper {
    
    // ============ PlantingQuestion Mappings ============
    
    /**
     * Convert PlantingQuestion entity to PlantingQuestionResponse DTO (without options)
     */
    public PlantingQuestionResponse toResponse(PlantingQuestion entity) {
        if (entity == null) return null;
        
        return PlantingQuestionResponse.builder()
                .id(entity.getId())
                .questionTextAr(entity.getQuestionTextAr())
                .questionTextEn(entity.getQuestionTextEn())
                .questionKey(entity.getQuestionKey())
                .isRequired(entity.getIsRequired())
                .allowMultiple(entity.getAllowMultiple())
                .displayOrder(entity.getDisplayOrder())
                .isActive(entity.getIsActive())
                .optionsCount(entity.getOptions() != null ? entity.getOptions().size() : 0)
                .options(null)  // لا نحمّل الخيارات بشكل افتراضي
                .build();
    }
    
    /**
     * Convert PlantingQuestion entity to PlantingQuestionResponse DTO (with options)
     */
    public PlantingQuestionResponse toResponseWithOptions(PlantingQuestion entity) {
        if (entity == null) return null;
        
        return PlantingQuestionResponse.builder()
                .id(entity.getId())
                .questionTextAr(entity.getQuestionTextAr())
                .questionTextEn(entity.getQuestionTextEn())
                .questionKey(entity.getQuestionKey())
                .isRequired(entity.getIsRequired())
                .allowMultiple(entity.getAllowMultiple())
                .displayOrder(entity.getDisplayOrder())
                .isActive(entity.getIsActive())
                .optionsCount(entity.getOptions() != null ? entity.getOptions().size() : 0)
                .options(toOptionResponseList(entity.getOptions()))
                .build();
    }
    
    /**
     * Convert PlantingQuestionRequest DTO to PlantingQuestion entity
     */
    public PlantingQuestion toEntity(PlantingQuestionRequest request) {
        if (request == null) return null;
        
        PlantingQuestion entity = new PlantingQuestion();
        entity.setQuestionTextAr(request.getQuestionTextAr());
        entity.setQuestionTextEn(request.getQuestionTextEn());
        entity.setQuestionKey(request.getQuestionKey());
        entity.setIsRequired(request.getIsRequired() != null ? request.getIsRequired() : true);
        entity.setAllowMultiple(request.getAllowMultiple() != null ? request.getAllowMultiple() : false);
        entity.setDisplayOrder(request.getDisplayOrder());
        entity.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        
        return entity;
    }
    
    /**
     * Update PlantingQuestion entity from PlantingQuestionRequest DTO
     */
    public void updateEntity(PlantingQuestion entity, PlantingQuestionRequest request) {
        if (entity == null || request == null) return;
        
        if (request.getQuestionTextAr() != null) {
            entity.setQuestionTextAr(request.getQuestionTextAr());
        }
        if (request.getQuestionTextEn() != null) {
            entity.setQuestionTextEn(request.getQuestionTextEn());
        }
        if (request.getQuestionKey() != null) {
            entity.setQuestionKey(request.getQuestionKey());
        }
        if (request.getIsRequired() != null) {
            entity.setIsRequired(request.getIsRequired());
        }
        if (request.getAllowMultiple() != null) {
            entity.setAllowMultiple(request.getAllowMultiple());
        }
        if (request.getDisplayOrder() != null) {
            entity.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }
    }
    
    /**
     * Convert list of PlantingQuestion entities to PlantingQuestionResponse DTOs
     */
    public List<PlantingQuestionResponse> toResponseList(List<PlantingQuestion> entities) {
        if (entities == null) return Collections.emptyList();
        
        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Convert list of PlantingQuestion entities to PlantingQuestionResponse DTOs with options
     */
    public List<PlantingQuestionResponse> toResponseListWithOptions(List<PlantingQuestion> entities) {
        if (entities == null) return Collections.emptyList();
        
        return entities.stream()
                .map(this::toResponseWithOptions)
                .collect(Collectors.toList());
    }
    
    // ============ QuestionOption Mappings ============
    
    /**
     * Convert QuestionOption entity to QuestionOptionResponse DTO
     */
    public QuestionOptionResponse toOptionResponse(QuestionOption entity) {
        if (entity == null) return null;
        
        Long questionId = null;
        try {
            if (entity.getQuestion() != null) {
                questionId = entity.getQuestion().getId();
            }
        } catch (Exception e) {
            // LazyInitializationException - question not loaded
            questionId = null;
        }
        
        return QuestionOptionResponse.builder()
                .id(entity.getId())
                .questionId(questionId)
                .optionTextAr(entity.getOptionTextAr())
                .optionTextEn(entity.getOptionTextEn())
                .optionKey(entity.getOptionKey())
                .displayOrder(entity.getDisplayOrder())
                .isActive(entity.getIsActive())
                .build();
    }
    
    /**
     * Convert QuestionOptionRequest DTO to QuestionOption entity
     */
    public QuestionOption toOptionEntity(QuestionOptionRequest request) {
        if (request == null) return null;
        
        QuestionOption entity = new QuestionOption();
        entity.setOptionTextAr(request.getOptionTextAr());
        entity.setOptionTextEn(request.getOptionTextEn());
        entity.setOptionKey(request.getOptionKey());
        entity.setDisplayOrder(request.getDisplayOrder());
        entity.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        
        return entity;
    }
    
    /**
     * Update QuestionOption entity from QuestionOptionRequest DTO
     */
    public void updateOptionEntity(QuestionOption entity, QuestionOptionRequest request) {
        if (entity == null || request == null) return;
        
        if (request.getOptionTextAr() != null) {
            entity.setOptionTextAr(request.getOptionTextAr());
        }
        if (request.getOptionTextEn() != null) {
            entity.setOptionTextEn(request.getOptionTextEn());
        }
        if (request.getOptionKey() != null) {
            entity.setOptionKey(request.getOptionKey());
        }
        if (request.getDisplayOrder() != null) {
            entity.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }
    }
    
    /**
     * Convert list of QuestionOption entities to QuestionOptionResponse DTOs
     */
    public List<QuestionOptionResponse> toOptionResponseList(List<QuestionOption> entities) {
        if (entities == null) return Collections.emptyList();
        
        return entities.stream()
                .map(this::toOptionResponse)
                .collect(Collectors.toList());
    }
}
