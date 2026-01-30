package group.g.graduation.backend.common.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * TaskType entity - أنواع المهام
 * ري، تسميد، حصاد، تقليم، رش
 */
@Entity
@Table(name = "task_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskType {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String nameAr;  // ري، تسميد، حصاد...
    
    private String nameEn;  // Watering, Fertilizing, Harvesting...
    
    private String icon;  // أيقونة المهمة 💧🧪🌾✂️
    
    @Column(columnDefinition = "TEXT")
    private String descriptionAr;  // وصف عام عربي
    
    @Column(columnDefinition = "TEXT")
    private String descriptionEn;  // وصف عام إنجليزي
    
    // ===== العلاقات =====
    @OneToMany(mappedBy = "taskType", cascade = CascadeType.ALL)
    private List<PlantTask> plantTasks = new ArrayList<>();
}
