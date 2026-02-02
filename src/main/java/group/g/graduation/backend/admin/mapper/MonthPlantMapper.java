package group.g.graduation.backend.admin.mapper;

import group.g.graduation.backend.admin.dto.MonthPlantResponse;
import group.g.graduation.backend.common.model.MonthPlant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for MonthPlant - محوّل علاقة الشهر بالنبتة
 */
@Slf4j
@Component
public class MonthPlantMapper {
    
    /**
     * Convert MonthPlant entity to MonthPlantResponse DTO
     */
    public MonthPlantResponse toResponse(MonthPlant entity) {
        if (entity == null) {
            log.warn("MonthPlant entity is null");
            return null;
        }
        
        try {
            MonthPlantResponse.MonthPlantResponseBuilder builder = MonthPlantResponse.builder()
                    .id(entity.getId())
                    .plantingNoteAr(entity.getPlantingNoteAr())
                    .plantingNoteEn(entity.getPlantingNoteEn());
            
            // Month info - with null safety
            if (entity.getMonth() != null) {
                try {
                    builder.monthId(entity.getMonth().getId())
                           .monthNumber(entity.getMonth().getMonthNumber())
                           .monthNameAr(entity.getMonth().getNameAr())
                           .monthNameEn(entity.getMonth().getNameEn());
                } catch (Exception e) {
                    log.warn("Could not load month data for MonthPlant {}: {}", entity.getId(), e.getMessage());
                    builder.monthId(null).monthNumber(null).monthNameAr("غير متاح").monthNameEn("N/A");
                }
            } else {
                log.warn("Month is null for MonthPlant {}", entity.getId());
                builder.monthId(null).monthNumber(null).monthNameAr("غير متاح").monthNameEn("N/A");
            }
            
            // Plant info - with null safety
            if (entity.getPlant() != null) {
                try {
                    builder.plantId(entity.getPlant().getId())
                           .plantNameAr(entity.getPlant().getNameAr())
                           .plantNameEn(entity.getPlant().getNameEn());
                } catch (Exception e) {
                    log.warn("Could not load plant data for MonthPlant {}: {}", entity.getId(), e.getMessage());
                    builder.plantId(null).plantNameAr("غير متاح").plantNameEn("N/A");
                }
            } else {
                log.warn("Plant is null for MonthPlant {}", entity.getId());
                builder.plantId(null).plantNameAr("غير متاح").plantNameEn("N/A");
            }
            
            return builder.build();
            
        } catch (Exception e) {
            log.error("Error mapping MonthPlant entity to response: {}", e.getMessage(), e);
            // Return a minimal response with the ID if possible
            return MonthPlantResponse.builder()
                    .id(entity.getId())
                    .plantingNoteAr(entity.getPlantingNoteAr())
                    .plantingNoteEn(entity.getPlantingNoteEn())
                    .monthNameAr("خطأ في التحميل")
                    .monthNameEn("Loading Error")
                    .plantNameAr("خطأ في التحميل")
                    .plantNameEn("Loading Error")
                    .build();
        }
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
