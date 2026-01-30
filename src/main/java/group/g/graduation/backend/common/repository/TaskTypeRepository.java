package group.g.graduation.backend.common.repository;

import group.g.graduation.backend.common.model.TaskType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaskTypeRepository extends JpaRepository<TaskType, Long> {
    
    // البحث بالاسم
    Optional<TaskType> findByNameAr(String nameAr);
    
    Optional<TaskType> findByNameEn(String nameEn);
}
