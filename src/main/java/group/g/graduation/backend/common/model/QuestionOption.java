package group.g.graduation.backend.common.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * QuestionOption entity - خيارات الأسئلة
 * الخيارات المتاحة لكل سؤال
 */
@Entity
@Table(name = "question_options")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionOption {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private PlantingQuestion question;
    
    @Column(nullable = false)
    private String optionTextAr;  // نص الخيار بالعربي
    
    private String optionTextEn;  // نص الخيار بالإنجليزي
    
    @Column(nullable = false)
    private String optionKey;  // مفتاح: indoor, balcony, full_sun, etc.
    
    private Integer displayOrder;  // ترتيب العرض
    
    private Boolean isActive = true;  // هل الخيار مفعّل
    
    // ===== العلاقات =====
    @OneToMany(mappedBy = "option", cascade = CascadeType.ALL)
    private List<PlantSuitability> suitabilities = new ArrayList<>();  // ملاءمة النباتات لهذا الخيار
}
