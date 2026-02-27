package group.g.graduation.backend.user.controller;

import group.g.graduation.backend.user.dto.preference.LocationResponse;
import group.g.graduation.backend.user.dto.preference.NotificationSettingsRequest;
import group.g.graduation.backend.user.dto.preference.NotificationSettingsResponse;
import group.g.graduation.backend.user.dto.preference.PalestineCityInfo;
import group.g.graduation.backend.user.dto.preference.SetLocationRequest;
import group.g.graduation.backend.user.service.UserPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * User Preference Controller - تفضيلات المستخدم 📍
 * 
 * إدارة موقع المستخدم وتفضيلاته:
 * - تحديد الموقع (المدينة في فلسطين)
 * - جلب قائمة المدن المتاحة
 * - جلب الموقع الحالي للمستخدم
 */
@RestController
@RequestMapping("/api/user/preferences")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User - Preferences", description = "تفضيلات المستخدم: الموقع، اللغة، الإشعارات")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    // ===== 1. قائمة المدن الفلسطينية =====

    @GetMapping("/cities")
    @Operation(
            summary = "جلب قائمة المدن الفلسطينية المتاحة",
            description = "يعرض كل المدن المتاحة في فلسطين (الضفة الغربية + قطاع غزة + مدن 48) مع إحداثياتها"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم جلب القائمة بنجاح"),
            @ApiResponse(responseCode = "401", description = "غير مصرح")
    })
    public ResponseEntity<List<PalestineCityInfo>> getAvailableCities() {
        log.info("🏙️ GET /api/user/preferences/cities - Fetching available cities");
        return ResponseEntity.ok(userPreferenceService.getAvailableCities());
    }

    // ===== 2. تحديد الموقع =====

    @PostMapping("/location")
    @Operation(
            summary = "تحديد موقع المستخدم",
            description = "المستخدم يختار مدينته في فلسطين. يمكن إرسال اسم المدينة بالعربي أو الإنجليزي، " +
                    "وسيتم تعبئة الإحداثيات تلقائياً. البيانات تُستخدم لعرض الطقس الحقيقي."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم تحديد الموقع بنجاح"),
            @ApiResponse(responseCode = "400", description = "بيانات غير صحيحة"),
            @ApiResponse(responseCode = "401", description = "غير مصرح")
    })
    public ResponseEntity<LocationResponse> setLocation(@Valid @RequestBody SetLocationRequest request) {
        log.info("📍 POST /api/user/preferences/location - Setting location: {}", request.getCity());
        LocationResponse response = userPreferenceService.setUserLocation(request);
        return ResponseEntity.ok(response);
    }

    // ===== 3. جلب الموقع الحالي =====

    @GetMapping("/location")
    @Operation(
            summary = "جلب موقع المستخدم الحالي",
            description = "يعرض المدينة والإحداثيات المحفوظة للمستخدم"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم جلب الموقع بنجاح"),
            @ApiResponse(responseCode = "204", description = "لم يتم تحديد الموقع بعد"),
            @ApiResponse(responseCode = "401", description = "غير مصرح")
    })
    public ResponseEntity<?> getLocation() {
        log.info("📍 GET /api/user/preferences/location - Fetching user location");
        LocationResponse location = userPreferenceService.getUserLocation();

        if (location == null) {
            return ResponseEntity.status(204).body(Map.of(
                    "message", "لم يتم تحديد الموقع بعد. يرجى اختيار مدينتك من قائمة المدن الفلسطينية.",
                    "messageEn", "Location not set yet. Please choose your city from the available Palestinian cities."
            ));
        }

        return ResponseEntity.ok(location);
    }

    // ===== 4. إعدادات الإشعارات =====

    @PutMapping("/notifications")
    @Operation(
            summary = "تحديث إعدادات الإشعارات",
            description = "تعديل إعدادات الإشعارات: تفعيل/تعطيل، Push، أوقات التذكير، اللغة"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم تحديث الإعدادات"),
            @ApiResponse(responseCode = "401", description = "غير مصرح")
    })
    public ResponseEntity<NotificationSettingsResponse> updateNotificationSettings(
            @Valid @RequestBody NotificationSettingsRequest request) {
        log.info("⚙️ PUT /api/user/preferences/notifications");
        return ResponseEntity.ok(userPreferenceService.updateNotificationSettings(request));
    }

    @GetMapping("/notifications")
    @Operation(
            summary = "جلب إعدادات الإشعارات الحالية",
            description = "يعرض حالة الإشعارات وأوقات التذكير واللغة المفضلة"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم جلب الإعدادات"),
            @ApiResponse(responseCode = "401", description = "غير مصرح")
    })
    public ResponseEntity<NotificationSettingsResponse> getNotificationSettings() {
        log.info("⚙️ GET /api/user/preferences/notifications");
        return ResponseEntity.ok(userPreferenceService.getNotificationSettings());
    }

    // ===== 5. تحديث FCM Token =====

    @PostMapping("/fcm-token")
    @Operation(
            summary = "تحديث FCM Token",
            description = "التطبيق يرسل الـ Firebase token عند كل فتح. يُستخدم لإرسال Push Notifications."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم تحديث التوكن"),
            @ApiResponse(responseCode = "401", description = "غير مصرح")
    })
    public ResponseEntity<Map<String, String>> updateFcmToken(@RequestBody Map<String, String> body) {
        String token = body.get("fcmToken");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "fcmToken is required",
                    "errorAr", "الـ FCM Token مطلوب"
            ));
        }
        log.info("📱 POST /api/user/preferences/fcm-token");
        userPreferenceService.updateFcmToken(token);
        return ResponseEntity.ok(Map.of(
                "message", "FCM token updated successfully",
                "messageAr", "تم تحديث التوكن بنجاح"
        ));
    }
}
