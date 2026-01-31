package group.g.graduation.backend.common.repository;

import group.g.graduation.backend.common.model.PlantingQuestion;
import group.g.graduation.backend.common.model.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {
    
    // جلب خيارات سؤال
    List<QuestionOption> findByQuestionOrderByDisplayOrderAsc(PlantingQuestion question);
    
    List<QuestionOption> findByQuestionIdOrderByDisplayOrderAsc(Long questionId);
    
    // جلب الخيارات النشطة لسؤال
    List<QuestionOption> findByQuestionIdAndIsActiveTrueOrderByDisplayOrderAsc(Long questionId);
    
    // جلب خيار بالمفتاح
    Optional<QuestionOption> findByQuestionIdAndOptionKey(Long questionId, String optionKey);
    
    // التحقق من وجود خيار
    boolean existsByQuestionIdAndOptionKey(Long questionId, String optionKey);
    
    // جلب الخيار مع السؤال (لتجنب LazyInitializationException)
    @Query("SELECT o FROM QuestionOption o LEFT JOIN FETCH o.question WHERE o.id = :id")
    Optional<QuestionOption> findByIdWithQuestion(@Param("id") Long id);
    
    // جلب كل الخيارات مع الأسئلة
    @Query("SELECT o FROM QuestionOption o LEFT JOIN FETCH o.question")
    List<QuestionOption> findAllWithQuestion();
}
