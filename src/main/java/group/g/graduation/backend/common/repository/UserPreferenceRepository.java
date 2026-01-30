package group.g.graduation.backend.common.repository;

import group.g.graduation.backend.Security.model.User;
import group.g.graduation.backend.common.model.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {
    
    // جلب تفضيلات مستخدم
    Optional<UserPreference> findByUser(User user);
    
    Optional<UserPreference> findByUserId(Long userId);
    
    // التحقق من وجود تفضيلات
    boolean existsByUserId(Long userId);
    
    // جلب مستخدمين بلغة معينة
    java.util.List<UserPreference> findByLanguage(String language);
    
    // جلب مستخدمين مع إشعارات مفعلة
    java.util.List<UserPreference> findByPushEnabledTrueAndFcmTokenIsNotNull();
}
