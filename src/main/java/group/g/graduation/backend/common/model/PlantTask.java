package group.g.graduation.backend.common.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PlantTask entity - مهام النباتات الافتراضية
 * كل نبتة لها مهام معينة بفترات محددة
 */
@Entity
@Table(name = "plant_tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlantTask {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_type_id", nullable = false)
    private TaskType taskType;
    
    private Integer intervalDays;  // كل كم يوم تتكرر المهمة
    
    @Column(columnDefinition = "TEXT")
    private String descriptionAr;  // وصف المهمة - عربي (مثل: اسق التربة حتى تصبح رطبة)
    
    @Column(columnDefinition = "TEXT")
    private String descriptionEn;  // وصف المهمة - إنجليزي
    
    private Integer startDayAfterPlanting;  // تبدأ بعد كم يوم من الزراعة
    
    private Boolean isRecurring = true;  // هل تتكرر أم مرة واحدة
}
