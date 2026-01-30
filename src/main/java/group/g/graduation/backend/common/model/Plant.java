package group.g.graduation.backend.common.model;

import group.g.graduation.backend.common.enums.DifficultyLevel;
import group.g.graduation.backend.common.enums.PlantCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Plant entity - النباتات الأساسية
 * يحتوي على كل معلومات النبات بلغتين (عربي وإنجليزي)
 */
@Entity
@Table(name = "plants")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Plant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // ===== الأسماء =====
    @Column(nullable = false)
    private String nameAr;  // اسم النبتة بالعربي
    
    private String nameEn;  // اسم النبتة بالإنجليزي
    
    private String nameScientific;  // الاسم العلمي
    
    // ===== الوصف المختصر =====
    @Column(length = 150)
    private String shortDescriptionAr;  // وصف مختصر عربي
    
    @Column(length = 150)
    private String shortDescriptionEn;  // وصف مختصر إنجليزي
    
    // ===== معلومات الضوء =====
    @Column(columnDefinition = "TEXT")
    private String lightInfoAr;
    
    @Column(columnDefinition = "TEXT")
    private String lightInfoEn;
    
    // ===== معلومات التربة =====
    @Column(columnDefinition = "TEXT")
    private String soilInfoAr;
    
    @Column(columnDefinition = "TEXT")
    private String soilInfoEn;
    
    // ===== معلومات الري =====
    @Column(columnDefinition = "TEXT")
    private String wateringInfoAr;
    
    @Column(columnDefinition = "TEXT")
    private String wateringInfoEn;
    
    private Integer wateringIntervalDays;  // فترة الري (كل كم يوم)
    
    // ===== العناية =====
    @Column(columnDefinition = "TEXT")
    private String careInfoAr;
    
    @Column(columnDefinition = "TEXT")
    private String careInfoEn;
    
    // ===== الحصاد =====
    @Column(columnDefinition = "TEXT")
    private String harvestInfoAr;
    
    @Column(columnDefinition = "TEXT")
    private String harvestInfoEn;
    
    // ===== الاستخدامات =====
    @Column(columnDefinition = "TEXT")
    private String usesInfoAr;
    
    @Column(columnDefinition = "TEXT")
    private String usesInfoEn;
    
    // ===== خطوات الزراعة =====
    @Column(columnDefinition = "TEXT")
    private String plantingStepsAr;  // خطوات الزراعة نص عربي
    
    @Column(columnDefinition = "TEXT")
    private String plantingStepsEn;  // خطوات الزراعة نص إنجليزي
    
    private String plantingVideoUrl;  // رابط فيديو الزراعة
    
    // ===== معلومات إضافية =====
    private Integer spacingCm;  // المسافة بين الشتلات (سم)
    
    private Integer daysToHarvest;  // أيام حتى الحصاد
    
    private Integer germinationDays;  // أيام الإنبات
    
    private Integer minTemp;  // أدنى درجة حرارة مناسبة
    
    private Integer maxTemp;  // أعلى درجة حرارة مناسبة
    
    // ===== التصنيفات =====
    @Enumerated(EnumType.STRING)
    private DifficultyLevel difficultyLevel;  // مستوى الصعوبة
    
    @Enumerated(EnumType.STRING)
    private PlantCategory category;  // التصنيف (خضروات، فواكه، إلخ)
    
    // ===== العلاقات =====
    @OneToMany(mappedBy = "plant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlantImage> images = new ArrayList<>();  // صور متعددة
    
    @OneToMany(mappedBy = "plant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlantTask> tasks = new ArrayList<>();  // المهام الافتراضية
    
    @OneToMany(mappedBy = "plant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MonthPlant> monthPlants = new ArrayList<>();  // الأشهر المناسبة
    
    @OneToMany(mappedBy = "plant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlantSuitability> suitabilities = new ArrayList<>();  // ملاءمة الظروف
    
    // ===== التواريخ =====
    @Column(updatable = false)
    private Instant createdAt;
    
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
