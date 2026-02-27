package group.g.graduation.backend.user.dto.preference;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * طلب تحديث إعدادات الإشعارات
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "طلب تحديث إعدادات الإشعارات")
public class NotificationSettingsRequest {

    @Schema(description = "تفعيل/تعطيل الإشعارات عموماً", example = "true")
    private Boolean notificationEnabled;

    @Schema(description = "تفعيل/تعطيل Push Notifications", example = "true")
    private Boolean pushEnabled;

    @Schema(description = "FCM Token من Firebase", example = "dGhpcyBpcyBhIHRlc3QgdG9rZW4...")
    private String fcmToken;

    @Schema(description = "وقت تذكير الصباح (HH:mm)", example = "07:00")
    private LocalTime morningReminderTime;

    @Schema(description = "وقت تذكير المساء (HH:mm)", example = "18:00")
    private LocalTime eveningReminderTime;

    @Schema(description = "اللغة المفضلة: ar أو en", example = "ar")
    private String language;
}
