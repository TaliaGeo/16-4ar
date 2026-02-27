package group.g.graduation.backend.common.service;

import group.g.graduation.backend.Security.model.User;
import group.g.graduation.backend.common.enums.NotificationType;
import group.g.graduation.backend.common.model.Notification;
import group.g.graduation.backend.common.model.UserPlant;
import group.g.graduation.backend.common.model.UserPlantTask;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FcmService - خدمة Firebase Push
 * 
 * ملاحظة: هذه الاختبارات تغطي السلوك عندما Firebase غير مفعّل
 * (الحالة الطبيعية في بيئة التطوير والاختبار)
 */
@ExtendWith(MockitoExtension.class)
class FcmServiceTest {

    private FcmService fcmService;

    @BeforeEach
    void setUp() {
        fcmService = new FcmService();
    }

    // =====================================================
    // ===== isFirebaseInitialized =====
    // =====================================================

    @Test
    @DisplayName("Firebase غير مفعّل في بيئة الاختبار")
    void isFirebaseInitialized_inTestEnvironment_returnsFalse() {
        // في بيئة الاختبار، Firebase لا يتم تهيئته
        // لأنه لا يوجد ملف service account
        assertFalse(fcmService.isFirebaseInitialized());
    }

    // =====================================================
    // ===== sendPushNotification - graceful degradation =====
    // =====================================================

    @Test
    @DisplayName("إرسال push بدون Firebase - يرجع false بدون خطأ")
    void sendPushNotification_firebaseNotInitialized_returnsFalse() {
        Notification notification = createTestNotification();

        boolean result = fcmService.sendPushNotification("test-token-123", notification);

        assertFalse(result);
    }

    @Test
    @DisplayName("إرسال push بتوكن null - يرجع false")
    void sendPushNotification_nullToken_returnsFalse() {
        Notification notification = createTestNotification();

        boolean result = fcmService.sendPushNotification(null, notification);

        assertFalse(result);
    }

    @Test
    @DisplayName("إرسال push بتوكن فارغ - يرجع false")
    void sendPushNotification_emptyToken_returnsFalse() {
        Notification notification = createTestNotification();

        boolean result = fcmService.sendPushNotification("", notification);

        assertFalse(result);
    }

    @Test
    @DisplayName("إرسال push بتوكن مسافات - يرجع false")
    void sendPushNotification_blankToken_returnsFalse() {
        Notification notification = createTestNotification();

        boolean result = fcmService.sendPushNotification("   ", notification);

        assertFalse(result);
    }

    // =====================================================
    // ===== sendSimplePush - graceful degradation =====
    // =====================================================

    @Test
    @DisplayName("إرسال push بسيط بدون Firebase - يرجع false")
    void sendSimplePush_firebaseNotInitialized_returnsFalse() {
        boolean result = fcmService.sendSimplePush("token", "Title", "Body", null);

        assertFalse(result);
    }

    @Test
    @DisplayName("إرسال push بسيط بتوكن null - يرجع false")
    void sendSimplePush_nullToken_returnsFalse() {
        boolean result = fcmService.sendSimplePush(null, "Title", "Body", null);

        assertFalse(result);
    }

    // =====================================================
    // ===== Helper =====
    // =====================================================

    private Notification createTestNotification() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        Notification notification = new Notification();
        notification.setId(10L);
        notification.setUser(user);
        notification.setTitleAr("تذكير ري");
        notification.setTitleEn("Watering Reminder");
        notification.setMessageAr("حان وقت الري");
        notification.setMessageEn("Time to water");
        notification.setType(NotificationType.WATERING_REMINDER);

        return notification;
    }
}
