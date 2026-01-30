package group.g.graduation.backend.common.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * PlantingQuestion entity - أسئلة إضافة المحصول
 * الأسئلة اللي بتطلع لليوزر عشان نحدد شو بناسبو يزرع
 */
@Entity
@Table(name = "planting_questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlantingQuestion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionTextAr;  // نص السؤال بالعربي
    
    @Column(columnDefinition = "TEXT")
    private String questionTextEn;  // نص السؤال بالإنجليزي
    
    @Column(nullable = false, unique = true)
    private String questionKey;  // مفتاح فريد: location, sunlight, container, etc.
    
    private Boolean isRequired = true;  // إجباري أم اختياري
    
    private Boolean allowMultiple = false;  // هل يسمح باختيار أكثر من خيار
    
    private Integer displayOrder;  // ترتيب العرض
    
    private Boolean isActive = true;  // هل السؤال مفعّل
    
    // ===== العلاقات =====
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<QuestionOption> options = new ArrayList<>();  // خيارات السؤال
}
