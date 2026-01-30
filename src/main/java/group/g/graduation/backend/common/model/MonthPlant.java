package group.g.graduation.backend.common.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MonthPlant entity - علاقة الأشهر بالنباتات
 * أي نباتات تنزرع في أي شهر
 */
@Entity
@Table(name = "month_plants", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"month_id", "plant_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthPlant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "month_id", nullable = false)
    private Month month;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;
    
    @Column(columnDefinition = "TEXT")
    private String plantingNoteAr;  // ملاحظة خاصة للزراعة بهذا الشهر - عربي
    
    @Column(columnDefinition = "TEXT")
    private String plantingNoteEn;  // ملاحظة خاصة للزراعة بهذا الشهر - إنجليزي
}
