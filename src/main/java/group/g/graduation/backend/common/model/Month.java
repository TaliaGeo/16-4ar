package group.g.graduation.backend.common.model;

import group.g.graduation.backend.common.enums.Season;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Month entity - الأشهر الـ 12
 * يحتوي على معلومات كل شهر والفصل والطقس في فلسطين
 */
@Entity
@Table(name = "months")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Month {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private Integer monthNumber;  // رقم الشهر (1-12)
    
    @Column(nullable = false)
    private String nameAr;  // يناير، فبراير، مارس...
    
    private String nameEn;  // January, February, March...
    
    @Enumerated(EnumType.STRING)
    private Season season;  // الفصل
    
    @Column(columnDefinition = "TEXT")
    private String weatherDescriptionAr;  // وصف حالة الطقس بفلسطين - عربي
    
    @Column(columnDefinition = "TEXT")
    private String weatherDescriptionEn;  // وصف حالة الطقس بفلسطين - إنجليزي
    
    private String imageUrl;  // صورة تعبر عن الشهر
    
    // ===== العلاقات =====
    @OneToMany(mappedBy = "month", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MonthPlant> monthPlants = new ArrayList<>();  // النباتات المناسبة لهذا الشهر
}
