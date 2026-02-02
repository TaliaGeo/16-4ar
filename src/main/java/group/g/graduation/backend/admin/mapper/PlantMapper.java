package group.g.graduation.backend.admin.mapper;

import group.g.graduation.backend.admin.dto.*;
import group.g.graduation.backend.common.model.Plant;
import group.g.graduation.backend.common.model.PlantImage;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for Plant entity and DTOs - تحويل بين الـ Entity والـ DTOs
 */
@Component
public class PlantMapper {
    
    /**
     * Convert PlantCreateRequest to Plant entity
     */
    public Plant toEntity(PlantCreateRequest request) {
        Plant plant = new Plant();
        
        // الأسماء
        plant.setNameAr(request.getNameAr());
        plant.setNameEn(request.getNameEn());
        plant.setNameScientific(request.getNameScientific());
        
        // الوصف المختصر
        plant.setShortDescriptionAr(request.getShortDescriptionAr());
        plant.setShortDescriptionEn(request.getShortDescriptionEn());
        
        // معلومات الضوء
        plant.setLightInfoAr(request.getLightInfoAr());
        plant.setLightInfoEn(request.getLightInfoEn());
        
        // معلومات التربة
        plant.setSoilInfoAr(request.getSoilInfoAr());
        plant.setSoilInfoEn(request.getSoilInfoEn());
        
        // معلومات الري
        plant.setWateringInfoAr(request.getWateringInfoAr());
        plant.setWateringInfoEn(request.getWateringInfoEn());
        plant.setWateringIntervalDays(request.getWateringIntervalDays());
        
        // العناية
        plant.setCareInfoAr(request.getCareInfoAr());
        plant.setCareInfoEn(request.getCareInfoEn());
        
        // الحصاد
        plant.setHarvestInfoAr(request.getHarvestInfoAr());
        plant.setHarvestInfoEn(request.getHarvestInfoEn());
        
        // الاستخدامات
        plant.setUsesInfoAr(request.getUsesInfoAr());
        plant.setUsesInfoEn(request.getUsesInfoEn());
        
        // خطوات الزراعة
        plant.setPlantingStepsAr(request.getPlantingStepsAr());
        plant.setPlantingStepsEn(request.getPlantingStepsEn());
        plant.setPlantingVideoUrl(request.getPlantingVideoUrl());
        
        // معلومات إضافية
        plant.setSpacingCm(request.getSpacingCm());
        plant.setDaysToHarvest(request.getDaysToHarvest());
        plant.setGerminationDays(request.getGerminationDays());
        plant.setMinTemp(request.getMinTemp());
        plant.setMaxTemp(request.getMaxTemp());
        
        // التصنيفات
        plant.setDifficultyLevel(request.getDifficultyLevel());
        plant.setCategory(request.getCategory());
        
        return plant;
    }
    
    /**
     * Update Plant entity from PlantUpdateRequest
     * Only updates non-null fields
     */
    public void updateEntity(Plant plant, PlantUpdateRequest request) {
        // الأسماء
        if (request.getNameAr() != null) plant.setNameAr(request.getNameAr());
        if (request.getNameEn() != null) plant.setNameEn(request.getNameEn());
        if (request.getNameScientific() != null) plant.setNameScientific(request.getNameScientific());
        
        // الوصف المختصر
        if (request.getShortDescriptionAr() != null) plant.setShortDescriptionAr(request.getShortDescriptionAr());
        if (request.getShortDescriptionEn() != null) plant.setShortDescriptionEn(request.getShortDescriptionEn());
        
        // معلومات الضوء
        if (request.getLightInfoAr() != null) plant.setLightInfoAr(request.getLightInfoAr());
        if (request.getLightInfoEn() != null) plant.setLightInfoEn(request.getLightInfoEn());
        
        // معلومات التربة
        if (request.getSoilInfoAr() != null) plant.setSoilInfoAr(request.getSoilInfoAr());
        if (request.getSoilInfoEn() != null) plant.setSoilInfoEn(request.getSoilInfoEn());
        
        // معلومات الري
        if (request.getWateringInfoAr() != null) plant.setWateringInfoAr(request.getWateringInfoAr());
        if (request.getWateringInfoEn() != null) plant.setWateringInfoEn(request.getWateringInfoEn());
        if (request.getWateringIntervalDays() != null) plant.setWateringIntervalDays(request.getWateringIntervalDays());
        
        // العناية
        if (request.getCareInfoAr() != null) plant.setCareInfoAr(request.getCareInfoAr());
        if (request.getCareInfoEn() != null) plant.setCareInfoEn(request.getCareInfoEn());
        
        // الحصاد
        if (request.getHarvestInfoAr() != null) plant.setHarvestInfoAr(request.getHarvestInfoAr());
        if (request.getHarvestInfoEn() != null) plant.setHarvestInfoEn(request.getHarvestInfoEn());
        
        // الاستخدامات
        if (request.getUsesInfoAr() != null) plant.setUsesInfoAr(request.getUsesInfoAr());
        if (request.getUsesInfoEn() != null) plant.setUsesInfoEn(request.getUsesInfoEn());
        
        // خطوات الزراعة
        if (request.getPlantingStepsAr() != null) plant.setPlantingStepsAr(request.getPlantingStepsAr());
        if (request.getPlantingStepsEn() != null) plant.setPlantingStepsEn(request.getPlantingStepsEn());
        if (request.getPlantingVideoUrl() != null) plant.setPlantingVideoUrl(request.getPlantingVideoUrl());
        
        // معلومات إضافية
        if (request.getSpacingCm() != null) plant.setSpacingCm(request.getSpacingCm());
        if (request.getDaysToHarvest() != null) plant.setDaysToHarvest(request.getDaysToHarvest());
        if (request.getGerminationDays() != null) plant.setGerminationDays(request.getGerminationDays());
        if (request.getMinTemp() != null) plant.setMinTemp(request.getMinTemp());
        if (request.getMaxTemp() != null) plant.setMaxTemp(request.getMaxTemp());
        
        // التصنيفات
        if (request.getDifficultyLevel() != null) plant.setDifficultyLevel(request.getDifficultyLevel());
        if (request.getCategory() != null) plant.setCategory(request.getCategory());
    }
    
    /**
     * Convert Plant entity to PlantResponse DTO
     */
    public PlantResponse toResponse(Plant plant) {
        // Defensive: handle null images list
        List<PlantImage> images = plant.getImages() != null ? plant.getImages() : List.of();
        
        List<PlantImageResponse> imageResponses = images.stream()
                .map(this::toImageResponse)
                .collect(Collectors.toList());
        
        String primaryImageUrl = images.stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .findFirst()
                .map(PlantImage::getImageUrl)
                .orElse(images.isEmpty() ? null : images.get(0).getImageUrl());
        
        return PlantResponse.builder()
                .id(plant.getId())
                // الأسماء
                .nameAr(plant.getNameAr())
                .nameEn(plant.getNameEn())
                .nameScientific(plant.getNameScientific())
                // الوصف المختصر
                .shortDescriptionAr(plant.getShortDescriptionAr())
                .shortDescriptionEn(plant.getShortDescriptionEn())
                // معلومات الضوء
                .lightInfoAr(plant.getLightInfoAr())
                .lightInfoEn(plant.getLightInfoEn())
                // معلومات التربة
                .soilInfoAr(plant.getSoilInfoAr())
                .soilInfoEn(plant.getSoilInfoEn())
                // معلومات الري
                .wateringInfoAr(plant.getWateringInfoAr())
                .wateringInfoEn(plant.getWateringInfoEn())
                .wateringIntervalDays(plant.getWateringIntervalDays())
                // العناية
                .careInfoAr(plant.getCareInfoAr())
                .careInfoEn(plant.getCareInfoEn())
                // الحصاد
                .harvestInfoAr(plant.getHarvestInfoAr())
                .harvestInfoEn(plant.getHarvestInfoEn())
                // الاستخدامات
                .usesInfoAr(plant.getUsesInfoAr())
                .usesInfoEn(plant.getUsesInfoEn())
                // خطوات الزراعة
                .plantingStepsAr(plant.getPlantingStepsAr())
                .plantingStepsEn(plant.getPlantingStepsEn())
                .plantingVideoUrl(plant.getPlantingVideoUrl())
                // معلومات إضافية
                .spacingCm(plant.getSpacingCm())
                .daysToHarvest(plant.getDaysToHarvest())
                .germinationDays(plant.getGerminationDays())
                .minTemp(plant.getMinTemp())
                .maxTemp(plant.getMaxTemp())
                // التصنيفات
                .difficultyLevel(plant.getDifficultyLevel())
                .category(plant.getCategory())
                // الصور
                .images(imageResponses)
                .primaryImageUrl(primaryImageUrl)
                // التواريخ
                .createdAt(plant.getCreatedAt())
                .updatedAt(plant.getUpdatedAt())
                .build();
    }
    
    /**
     * Convert Plant entity to PlantSummaryResponse DTO (for lists)
     */
    public PlantSummaryResponse toSummaryResponse(Plant plant) {
        // Defensive: handle null images list
        List<PlantImage> images = plant.getImages() != null ? plant.getImages() : List.of();
        
        String primaryImageUrl = images.stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .findFirst()
                .map(PlantImage::getImageUrl)
                .orElse(images.isEmpty() ? null : images.get(0).getImageUrl());
        
        return PlantSummaryResponse.builder()
                .id(plant.getId())
                .nameAr(plant.getNameAr())
                .nameEn(plant.getNameEn())
                .nameScientific(plant.getNameScientific())
                .shortDescriptionAr(plant.getShortDescriptionAr())
                .shortDescriptionEn(plant.getShortDescriptionEn())
                .difficultyLevel(plant.getDifficultyLevel())
                .category(plant.getCategory())
                .primaryImageUrl(primaryImageUrl)
                .imagesCount(images.size())
                .build();
    }
    
    /**
     * Convert Page of Plants to PlantListResponse
     */
    public PlantListResponse toListResponse(Page<Plant> page) {
        List<PlantSummaryResponse> plants = page.getContent().stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());
        
        return PlantListResponse.builder()
                .plants(plants)
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .build();
    }
    
    /**
     * Convert PlantImageRequest to PlantImage entity
     */
    public PlantImage toImageEntity(PlantImageRequest request, Plant plant) {
        PlantImage image = new PlantImage();
        image.setPlant(plant);
        image.setImageUrl(request.getImageUrl());
        image.setAltTextAr(request.getAltTextAr());
        image.setAltTextEn(request.getAltTextEn());
        image.setIsPrimary(request.getIsPrimary() != null ? request.getIsPrimary() : false);
        image.setDisplayOrder(request.getDisplayOrder());
        return image;
    }
    
    /**
     * Convert PlantImage entity to PlantImageResponse DTO
     */
    public PlantImageResponse toImageResponse(PlantImage image) {
        return PlantImageResponse.builder()
                .id(image.getId())
                .plantId(image.getPlant().getId())
                .imageUrl(image.getImageUrl())
                .altTextAr(image.getAltTextAr())
                .altTextEn(image.getAltTextEn())
                .isPrimary(image.getIsPrimary())
                .displayOrder(image.getDisplayOrder())
                .build();
    }
}
