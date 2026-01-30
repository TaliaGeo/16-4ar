package group.g.graduation.backend.common.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Plant Image entity - صور النباتات
 * كل نبتة ممكن يكون لها أكثر من صورة
 */
@Entity
@Table(name = "plant_images")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlantImage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;
    
    @Column(nullable = false)
    private String imageUrl;  // رابط الصورة
    
    private String altTextAr;  // نص بديل عربي
    
    private String altTextEn;  // نص بديل إنجليزي
    
    private Boolean isPrimary = false;  // هل هي الصورة الرئيسية
    
    private Integer displayOrder;  // ترتيب العرض
}
