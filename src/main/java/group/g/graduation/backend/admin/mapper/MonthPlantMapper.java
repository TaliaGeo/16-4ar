package group.g.graduation.backend.admin.mapper;

import group.g.graduation.backend.admin.dto.MonthPlantResponse;
import group.g.graduation.backend.common.model.MonthPlant;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for MonthPlant - محوّل علاقة الشهر بالنبتة
 */
@Component
public class MonthPlantMapper {
    
    /**
     * Convert MonthPlant entity to MonthPlantResponse DTO
     */
    public MonthPlantResponse toResponse(MonthPlant entity) {
        if (entity == null) return null;
        
        MonthPlantResponse.MonthPlantResponseBuilder builder = MonthPlantResponse.builder()
                .id(entity.getId())
                .plantingNoteAr(entity.getPlantingNoteAr())
                .plantingNoteEn(entity.getPlantingNoteEn());
        
        // Month info
        try {
            if (entity.getMonth() != null) {
                builder.monthId(entity.getMonth().getId())
                       .monthNumber(entity.getMonth().getMonthNumber())
                       .monthNameAr(entity.getMonth().getNameAr())
                       .monthNameEn(entity.getMonth().getNameEn());
            }
        } catch (Exception e) {
            // LazyInitializationException
        }
        
        // Plant info
        try {
            if (entity.getPlant() != null) {
                builder.plantId(entity.getPlant().getId())
                       .plantNameAr(entity.getPlant().getNameAr())
                       .plantNameEn(entity.getPlant().getNameEn());
            }
        } catch (Exception e) {
            // LazyInitializationException
        }
        
        return builder.build();
    }
    
    /**
     * Convert list of MonthPlant entities to MonthPlantResponse DTOs
     */
    public List<MonthPlantResponse> toResponseList(List<MonthPlant> entities) {
        if (entities == null) return Collections.emptyList();
        
        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
