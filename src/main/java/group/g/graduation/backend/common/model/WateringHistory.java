package group.g.graduation.backend.common.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * WateringHistory entity - سجل الري
 * تتبع كل مرة يروي فيها المستخدم النبتة
 */
@Entity
@Table(name = "watering_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WateringHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_plant_id", nullable = false)
    private UserPlant userPlant;
    
    @Column(nullable = false)
    private LocalDateTime wateredAt;  // وقت الري
    
    @Column(columnDefinition = "TEXT")
    private String notes;  // ملاحظات (اختياري)
    
    @Column(updatable = false)
    private Instant createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (wateredAt == null) {
            wateredAt = LocalDateTime.now();
        }
    }
}
