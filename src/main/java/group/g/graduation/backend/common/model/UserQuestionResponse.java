package group.g.graduation.backend.common.model;

import group.g.graduation.backend.Security.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * UserQuestionResponse entity - إجابات المستخدم على الأسئلة
 * حفظ إجابات المستخدم المؤقتة قبل عرض الاقتراحات
 */
@Entity
@Table(name = "user_question_responses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserQuestionResponse {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String sessionId;  // معرف الجلسة (لربط الإجابات معاً)
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private PlantingQuestion question;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    private QuestionOption option;  // كل صف = خيار واحد (للأسئلة المتعددة نحفظ عدة صفوف)
    
    @Column(updatable = false)
    private Instant createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
