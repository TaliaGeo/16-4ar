package group.g.graduation.backend.user.service;

import group.g.graduation.backend.Security.model.User;
import group.g.graduation.backend.Security.repository.UserRepository;
import group.g.graduation.backend.Security.util.SecurityUtils;
import group.g.graduation.backend.admin.dto.NotificationResponse;
import group.g.graduation.backend.admin.mapper.NotificationMapper;
import group.g.graduation.backend.common.enums.NotificationType;
import group.g.graduation.backend.common.model.Notification;
import group.g.graduation.backend.common.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserNotificationService - إشعارات المستخدم
 */
@ExtendWith(MockitoExtension.class)
class UserNotificationServiceTest {

    @InjectMocks
    private UserNotificationService service;

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationMapper notificationMapper;
    @Mock private UserRepository userRepository;

    private User testUser;
    private Notification testNotification;
    private NotificationResponse testResponse;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@gharsih.ps");
        testUser.setFullName("Test User");

        testNotification = new Notification();
        testNotification.setId(10L);
        testNotification.setUser(testUser);
        testNotification.setTitleAr("تذكير: ري 🌱");
        testNotification.setTitleEn("Reminder: Watering 🌱");
        testNotification.setMessageAr("حان وقت ري نبتتك");
        testNotification.setMessageEn("Time to water your plant");
        testNotification.setType(NotificationType.WATERING_REMINDER);
        testNotification.setIsRead(false);
        testNotification.setIsPushed(false);
        testNotification.setCreatedAt(Instant.now());

        testResponse = NotificationResponse.builder()
                .id(10L)
                .userId(1L)
                .titleAr("تذكير: ري 🌱")
                .titleEn("Reminder: Watering 🌱")
                .type(NotificationType.WATERING_REMINDER)
                .isRead(false)
                .build();
    }

    // =====================================================
    // ===== getMyNotifications =====
    // =====================================================

    @Test
    @DisplayName("جلب كل الإشعارات - يرجع قائمة")
    void getMyNotifications_returnsList() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@gharsih.ps"));
            when(userRepository.findByEmail("test@gharsih.ps")).thenReturn(Optional.of(testUser));
            when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(testNotification));
            when(notificationMapper.toResponse(testNotification)).thenReturn(testResponse);

            List<NotificationResponse> result = service.getMyNotifications();

            assertEquals(1, result.size());
            assertEquals("تذكير: ري 🌱", result.get(0).getTitleAr());
        }
    }

    @Test
    @DisplayName("إشعارات مستخدم جديد - قائمة فارغة")
    void getMyNotifications_noNotifications_returnsEmpty() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@gharsih.ps"));
            when(userRepository.findByEmail("test@gharsih.ps")).thenReturn(Optional.of(testUser));
            when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L))
                    .thenReturn(Collections.emptyList());

            List<NotificationResponse> result = service.getMyNotifications();

            assertTrue(result.isEmpty());
        }
    }

    // =====================================================
    // ===== getMyUnreadNotifications =====
    // =====================================================

    @Test
    @DisplayName("جلب غير المقروءة فقط")
    void getMyUnreadNotifications_returnsUnreadOnly() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@gharsih.ps"));
            when(userRepository.findByEmail("test@gharsih.ps")).thenReturn(Optional.of(testUser));
            when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(testNotification));
            when(notificationMapper.toResponse(testNotification)).thenReturn(testResponse);

            List<NotificationResponse> result = service.getMyUnreadNotifications();

            assertEquals(1, result.size());
            assertFalse(result.get(0).getIsRead());
        }
    }

    // =====================================================
    // ===== getMyNotificationsByType =====
    // =====================================================

    @Test
    @DisplayName("جلب إشعارات حسب النوع")
    void getMyNotificationsByType_filtersCorrectly() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@gharsih.ps"));
            when(userRepository.findByEmail("test@gharsih.ps")).thenReturn(Optional.of(testUser));
            when(notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(1L, NotificationType.WATERING_REMINDER))
                    .thenReturn(List.of(testNotification));
            when(notificationMapper.toResponse(testNotification)).thenReturn(testResponse);

            List<NotificationResponse> result = service.getMyNotificationsByType(NotificationType.WATERING_REMINDER);

            assertEquals(1, result.size());
            assertEquals(NotificationType.WATERING_REMINDER, result.get(0).getType());
        }
    }

    // =====================================================
    // ===== getUnreadCount =====
    // =====================================================

    @Test
    @DisplayName("عدد غير المقروءة - يرجع العدد الصحيح")
    void getUnreadCount_returnsCorrectCount() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@gharsih.ps"));
            when(userRepository.findByEmail("test@gharsih.ps")).thenReturn(Optional.of(testUser));
            when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(5L);

            Map<String, Object> result = service.getUnreadCount();

            assertEquals(5L, result.get("unreadCount"));
            assertEquals(1L, result.get("userId"));
        }
    }

    @Test
    @DisplayName("عدد غير المقروءة - صفر لمستخدم جديد")
    void getUnreadCount_noUnread_returnsZero() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@gharsih.ps"));
            when(userRepository.findByEmail("test@gharsih.ps")).thenReturn(Optional.of(testUser));
            when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(0L);

            Map<String, Object> result = service.getUnreadCount();

            assertEquals(0L, result.get("unreadCount"));
        }
    }

    // =====================================================
    // ===== markAsRead =====
    // =====================================================

    @Test
    @DisplayName("تحديد كمقروء - ينجح لإشعار المستخدم")
    void markAsRead_ownNotification_marksSuccessfully() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@gharsih.ps"));
            when(userRepository.findByEmail("test@gharsih.ps")).thenReturn(Optional.of(testUser));
            when(notificationRepository.findById(10L)).thenReturn(Optional.of(testNotification));

            NotificationResponse readResponse = NotificationResponse.builder()
                    .id(10L).isRead(true).build();
            when(notificationMapper.toResponse(any())).thenReturn(readResponse);

            NotificationResponse result = service.markAsRead(10L);

            assertTrue(result.getIsRead());
            verify(notificationRepository).save(testNotification);
        }
    }

    @Test
    @DisplayName("تحديد كمقروء - يرفض إشعار مستخدم آخر")
    void markAsRead_otherUserNotification_throwsException() {
        User otherUser = new User();
        otherUser.setId(999L);

        Notification otherNotification = new Notification();
        otherNotification.setId(20L);
        otherNotification.setUser(otherUser);

        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@gharsih.ps"));
            when(userRepository.findByEmail("test@gharsih.ps")).thenReturn(Optional.of(testUser));
            when(notificationRepository.findById(20L)).thenReturn(Optional.of(otherNotification));

            assertThrows(SecurityException.class, () -> service.markAsRead(20L));
        }
    }

    @Test
    @DisplayName("تحديد كمقروء - إشعار غير موجود")
    void markAsRead_notFound_throwsException() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@gharsih.ps"));
            when(userRepository.findByEmail("test@gharsih.ps")).thenReturn(Optional.of(testUser));
            when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> service.markAsRead(999L));
        }
    }

    // =====================================================
    // ===== markAllAsRead =====
    // =====================================================

    @Test
    @DisplayName("تحديد الكل كمقروء - يرجع عدد المحدثة")
    void markAllAsRead_returnsMarkedCount() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@gharsih.ps"));
            when(userRepository.findByEmail("test@gharsih.ps")).thenReturn(Optional.of(testUser));
            when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(3L);

            Map<String, Object> result = service.markAllAsRead();

            assertEquals(3L, result.get("markedCount"));
            verify(notificationRepository).markAllAsReadForUser(1L);
        }
    }

    // =====================================================
    // ===== deleteNotification =====
    // =====================================================

    @Test
    @DisplayName("حذف إشعار - ينجح للمالك")
    void deleteNotification_ownNotification_deletesSuccessfully() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@gharsih.ps"));
            when(userRepository.findByEmail("test@gharsih.ps")).thenReturn(Optional.of(testUser));
            when(notificationRepository.findById(10L)).thenReturn(Optional.of(testNotification));

            service.deleteNotification(10L);

            verify(notificationRepository).delete(testNotification);
        }
    }

    @Test
    @DisplayName("حذف إشعار - يرفض حذف إشعار مستخدم آخر")
    void deleteNotification_otherUserNotification_throwsException() {
        User otherUser = new User();
        otherUser.setId(999L);

        Notification otherNotification = new Notification();
        otherNotification.setId(20L);
        otherNotification.setUser(otherUser);

        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@gharsih.ps"));
            when(userRepository.findByEmail("test@gharsih.ps")).thenReturn(Optional.of(testUser));
            when(notificationRepository.findById(20L)).thenReturn(Optional.of(otherNotification));

            assertThrows(SecurityException.class, () -> service.deleteNotification(20L));
        }
    }

    // =====================================================
    // ===== Auth Edge Cases =====
    // =====================================================

    @Test
    @DisplayName("مستخدم غير مسجل دخول - يرمي خطأ")
    void getMyNotifications_notAuthenticated_throwsException() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> service.getMyNotifications());
        }
    }
}
