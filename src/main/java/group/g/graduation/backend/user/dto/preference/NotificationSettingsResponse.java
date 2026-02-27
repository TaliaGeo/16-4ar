package group.g.graduation.backend.user.dto.preference;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * استجابة إعدادات الإشعارات
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "إعدادات الإشعارات الحالية")
public class NotificationSettingsResponse {

    @Schema(description = "هل الإشعارات مفعلة؟", example = "true")
    private boolean notificationEnabled;

    @Schema(description = "هل Push Notifications مفعلة؟", example = "true")
    private boolean pushEnabled;

    @Schema(description = "هل يوجد FCM Token مسجل؟", example = "true")
    private boolean hasFcmToken;

    @Schema(description = "وقت تذكير الصباح", example = "07:00")
    private LocalTime morningReminderTime;

    @Schema(description = "وقت تذكير المساء", example = "18:00")
    private LocalTime eveningReminderTime;

    @Schema(description = "اللغة المفضلة", example = "ar")
    private String language;
}
