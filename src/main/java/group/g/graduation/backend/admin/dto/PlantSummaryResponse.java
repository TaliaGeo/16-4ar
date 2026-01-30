package group.g.graduation.backend.admin.dto;

import group.g.graduation.backend.common.enums.DifficultyLevel;
import group.g.graduation.backend.common.enums.PlantCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for plant summary in lists - ملخص النبتة للقوائم
 * يستخدم لعرض قائمة النباتات بدون كل التفاصيل
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantSummaryResponse {
    
    private Long id;
    
    // ===== الأسماء =====
    private String nameAr;
    private String nameEn;
    private String nameScientific;
    
    // ===== الوصف المختصر =====
    private String shortDescriptionAr;
    private String shortDescriptionEn;
    
    // ===== التصنيفات =====
    private DifficultyLevel difficultyLevel;
    private PlantCategory category;
    
    // ===== الصورة الرئيسية =====
    private String primaryImageUrl;
    
    // ===== إحصائيات سريعة =====
    private Integer imagesCount;  // عدد الصور
}
