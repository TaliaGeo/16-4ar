package group.g.graduation.backend.admin.dto;

import group.g.graduation.backend.common.enums.DifficultyLevel;
import group.g.graduation.backend.common.enums.PlantCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for plant response - رد معلومات النبتة
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantResponse {
    
    private Long id;
    
    // ===== الأسماء =====
    private String nameAr;
    private String nameEn;
    private String nameScientific;
    
    // ===== الوصف المختصر =====
    private String shortDescriptionAr;
    private String shortDescriptionEn;
    
    // ===== معلومات الضوء =====
    private String lightInfoAr;
    private String lightInfoEn;
    
    // ===== معلومات التربة =====
    private String soilInfoAr;
    private String soilInfoEn;
    
    // ===== معلومات الري =====
    private String wateringInfoAr;
    private String wateringInfoEn;
    private Integer wateringIntervalDays;
    
    // ===== العناية =====
    private String careInfoAr;
    private String careInfoEn;
    
    // ===== الحصاد =====
    private String harvestInfoAr;
    private String harvestInfoEn;
    
    // ===== الاستخدامات =====
    private String usesInfoAr;
    private String usesInfoEn;
    
    // ===== خطوات الزراعة =====
    private String plantingStepsAr;
    private String plantingStepsEn;
    private String plantingVideoUrl;
    
    // ===== معلومات إضافية =====
    private Integer spacingCm;
    private Integer daysToHarvest;
    private Integer germinationDays;
    private Integer minTemp;
    private Integer maxTemp;
    
    // ===== التصنيفات =====
    private DifficultyLevel difficultyLevel;
    private PlantCategory category;
    
    // ===== الصور =====
    @Builder.Default
    private List<PlantImageResponse> images = new ArrayList<>();
    
    private String primaryImageUrl;  // رابط الصورة الرئيسية للعرض السريع
    
    // ===== التواريخ =====
    private Instant createdAt;
    private Instant updatedAt;
}
