package group.g.graduation.backend.common.model;

import group.g.graduation.backend.Security.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * PlantRecommendation entity - توصيات النباتات
 * الاقتراحات اللي بتطلع لليوزر بعد ما يجاوب على الأسئلة
 */
@Entity
@Table(name = "plant_recommendations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlantRecommendation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String sessionId;  // معرف جلسة الأسئلة
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;
    
    @Column(nullable = false)
    private Double matchPercentage;  // نسبة التطابق (0-100%)
    
    @Column(columnDefinition = "TEXT")
    private String recommendationReasonAr;  // سبب الاقتراح - عربي
    
    @Column(columnDefinition = "TEXT")
    private String recommendationReasonEn;  // سبب الاقتراح - إنجليزي
    
    @Column(columnDefinition = "TEXT")
    private String adjustmentTipsAr;  // نصائح للتحسين - عربي
    
    @Column(columnDefinition = "TEXT")
    private String adjustmentTipsEn;  // نصائح للتحسين - إنجليزي
    
    private Boolean isSelected = false;  // هل اختار المستخدم هذا النبات
    
    @Column(updatable = false)
    private Instant createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
