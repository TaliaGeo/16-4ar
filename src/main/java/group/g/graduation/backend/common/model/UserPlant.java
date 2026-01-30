package group.g.graduation.backend.common.model;

import group.g.graduation.backend.Security.model.User;
import group.g.graduation.backend.common.enums.PlantStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * UserPlant entity - محاصيل المستخدم
 * كل نبتة يزرعها المستخدم (مخطط، مزروع، تم حصاده)
 */
@Entity
@Table(name = "user_plants")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPlant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlantStatus status = PlantStatus.PLANNED;  // حالة النبتة
    
    private String nickname;  // اسم مخصص للنبتة (اختياري)
    
    // ===== التواريخ =====
    private LocalDate plannedDate;  // تاريخ التخطيط
    
    private LocalDate plantedDate;  // تاريخ الزراعة الفعلي
    
    private LocalDate harvestedDate;  // تاريخ الحصاد
    
    @Column(columnDefinition = "TEXT")
    private String notes;  // ملاحظات المستخدم
    
    // ===== العلاقات =====
    @OneToMany(mappedBy = "userPlant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WateringHistory> wateringHistory = new ArrayList<>();  // سجل الري
    
    @OneToMany(mappedBy = "userPlant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserPlantTask> tasks = new ArrayList<>();  // مهام هذه النبتة
    
    // ===== التواريخ =====
    @Column(updatable = false)
    private Instant createdAt;
    
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (plannedDate == null) {
            plannedDate = LocalDate.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
