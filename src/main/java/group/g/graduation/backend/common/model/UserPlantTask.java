package group.g.graduation.backend.common.model;

import group.g.graduation.backend.common.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * UserPlantTask entity - مهام المستخدم الفعلية
 * المهام المحددة لكل نبتة يزرعها المستخدم
 */
@Entity
@Table(name = "user_plant_tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPlantTask {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_plant_id", nullable = false)
    private UserPlant userPlant;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_type_id", nullable = false)
    private TaskType taskType;
    
    @Column(columnDefinition = "TEXT")
    private String descriptionAr;  // وصف المهمة - عربي
    
    @Column(columnDefinition = "TEXT")
    private String descriptionEn;  // وصف المهمة - إنجليزي
    
    @Column(nullable = false)
    private LocalDate dueDate;  // تاريخ الاستحقاق
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.PENDING;  // حالة المهمة
    
    private LocalDateTime completedAt;  // وقت إتمام المهمة
    
    private LocalDateTime snoozedUntil;  // تأجيل التذكير حتى
    
    @Column(updatable = false)
    private Instant createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
