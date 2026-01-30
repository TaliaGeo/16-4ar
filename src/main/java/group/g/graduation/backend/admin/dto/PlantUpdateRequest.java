package group.g.graduation.backend.admin.dto;

import group.g.graduation.backend.common.enums.DifficultyLevel;
import group.g.graduation.backend.common.enums.PlantCategory;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating a plant - طلب تعديل نبتة
 * كل الحقول اختيارية - يتم تحديث الحقول المرسلة فقط
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantUpdateRequest {
    
    // ===== الأسماء =====
    @Size(max = 100, message = "اسم النبتة يجب أن لا يتجاوز 100 حرف")
    private String nameAr;
    
    @Size(max = 100, message = "Plant name must not exceed 100 characters")
    private String nameEn;
    
    @Size(max = 150, message = "Scientific name must not exceed 150 characters")
    private String nameScientific;
    
    // ===== الوصف المختصر =====
    @Size(max = 150, message = "الوصف المختصر يجب أن لا يتجاوز 150 حرف")
    private String shortDescriptionAr;
    
    @Size(max = 150, message = "Short description must not exceed 150 characters")
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
    
    @Min(value = 1, message = "فترة الري يجب أن تكون يوم واحد على الأقل")
    @Max(value = 30, message = "فترة الري يجب أن لا تتجاوز 30 يوم")
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
    
    @Size(max = 500, message = "رابط الفيديو يجب أن لا يتجاوز 500 حرف")
    private String plantingVideoUrl;
    
    // ===== معلومات إضافية =====
    @Min(value = 1, message = "المسافة بين الشتلات يجب أن تكون 1 سم على الأقل")
    private Integer spacingCm;
    
    @Min(value = 1, message = "أيام الحصاد يجب أن تكون يوم واحد على الأقل")
    private Integer daysToHarvest;
    
    @Min(value = 1, message = "أيام الإنبات يجب أن تكون يوم واحد على الأقل")
    private Integer germinationDays;
    
    @Min(value = -10, message = "درجة الحرارة الدنيا غير صالحة")
    @Max(value = 50, message = "درجة الحرارة الدنيا غير صالحة")
    private Integer minTemp;
    
    @Min(value = -10, message = "درجة الحرارة القصوى غير صالحة")
    @Max(value = 60, message = "درجة الحرارة القصوى غير صالحة")
    private Integer maxTemp;
    
    // ===== التصنيفات =====
    private DifficultyLevel difficultyLevel;
    
    private PlantCategory category;
}
