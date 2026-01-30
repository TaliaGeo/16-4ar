package group.g.graduation.backend.common.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PlantSuitability entity - ملاءمة النبات للظروف
 * نقاط التوافق بين كل نبات وكل خيار من خيارات الأسئلة
 */
@Entity
@Table(name = "plant_suitability", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"plant_id", "option_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlantSuitability {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    private QuestionOption option;
    
    @Column(nullable = false)
    private Integer score;  // نقاط (0-100)
    
    @Column(columnDefinition = "TEXT")
    private String adjustmentTipAr;  // نصيحة تعديل - عربي
    
    @Column(columnDefinition = "TEXT")
    private String adjustmentTipEn;  // نصيحة تعديل - إنجليزي
}
