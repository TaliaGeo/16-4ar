package group.g.graduation.backend.common.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import group.g.graduation.backend.common.model.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Firebase Cloud Messaging Service - خدمة الإشعارات الفورية 📲
 * 
 * ترسل push notifications للموبايل عبر Firebase.
 * إذا Firebase مش مفعّل، ما بتعمل إشي (graceful degradation).
 */
@Service
@Slf4j
public class FcmService {

    /**
     * إرسال push notification لمستخدم واحد
     * 
     * @param fcmToken توكن FCM الخاص بالجهاز
     * @param notification الإشعار المراد إرساله
     * @return true إذا نجح الإرسال
     */
    @Async
    public boolean sendPushNotification(String fcmToken, Notification notification) {
        if (!isFirebaseInitialized()) {
            log.debug("🔕 Firebase not initialized — skipping push for notification #{}", notification.getId());
            return false;
        }

        if (fcmToken == null || fcmToken.isBlank()) {
            log.debug("🔕 No FCM token — skipping push for user #{}", notification.getUser().getId());
            return false;
        }

        try {
            // بناء data payload (بيانات إضافية للتطبيق)
            Map<String, String> data = buildDataPayload(notification);

            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(notification.getTitleEn() != null ? notification.getTitleEn() : notification.getTitleAr())
                            .setBody(notification.getMessageEn() != null ? notification.getMessageEn() : notification.getMessageAr())
                            .build())
                    // Android specific
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setTitle(notification.getTitleAr())
                                    .setBody(notification.getMessageAr())
                                    .setClickAction("OPEN_NOTIFICATION")
                                    .setSound("default")
                                    .build())
                            .build())
                    // iOS specific (APNs)
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder()
                                    .setAlert(ApsAlert.builder()
                                            .setTitle(notification.getTitleAr())
                                            .setBody(notification.getMessageAr())
                                            .build())
                                    .setSound("default")
                                    .setBadge(1)
                                    .build())
                            .build())
                    .putAllData(data)
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("✅ FCM sent to user #{} — response: {}", notification.getUser().getId(), response);
            return true;

        } catch (FirebaseMessagingException e) {
            handleFcmError(e, fcmToken, notification);
            return false;
        } catch (Exception e) {
            log.error("❌ Unexpected FCM error for notification #{}: {}", notification.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * إرسال push notification برسالة مخصصة
     */
    @Async
    public boolean sendSimplePush(String fcmToken, String title, String body, Map<String, String> data) {
        if (!isFirebaseInitialized() || fcmToken == null || fcmToken.isBlank()) {
            return false;
        }

        try {
            Message.Builder builder = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());

            if (data != null && !data.isEmpty()) {
                builder.putAllData(data);
            }

            String response = FirebaseMessaging.getInstance().send(builder.build());
            log.debug("✅ Simple FCM sent — response: {}", response);
            return true;

        } catch (Exception e) {
            log.warn("⚠️ Simple FCM failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * التحقق إذا Firebase مفعّل
     */
    public boolean isFirebaseInitialized() {
        return !FirebaseApp.getApps().isEmpty();
    }

    // ═══════════════════════════════════════════════════════
    // Private Helpers
    // ═══════════════════════════════════════════════════════

    private Map<String, String> buildDataPayload(Notification notification) {
        Map<String, String> data = new HashMap<>();
        data.put("notificationId", String.valueOf(notification.getId()));
        data.put("type", notification.getType() != null ? notification.getType().name() : "GENERAL");

        if (notification.getUserPlant() != null) {
            data.put("userPlantId", String.valueOf(notification.getUserPlant().getId()));
        }
        if (notification.getTask() != null) {
            data.put("taskId", String.valueOf(notification.getTask().getId()));
        }

        return data;
    }

    private void handleFcmError(FirebaseMessagingException e, String fcmToken, Notification notification) {
        MessagingErrorCode errorCode = e.getMessagingErrorCode();

        if (errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
            // التوكن منتهي أو غير صالح — لازم يتحدث من التطبيق
            log.warn("🔑 Invalid/expired FCM token for user #{} — token should be refreshed",
                    notification.getUser().getId());
        } else if (errorCode == MessagingErrorCode.QUOTA_EXCEEDED) {
            log.warn("📊 FCM quota exceeded — retrying later");
        } else {
            log.error("❌ FCM error [{}] for notification #{}: {}",
                    errorCode, notification.getId(), e.getMessage());
        }
    }
}
