package group.g.graduation.backend.user.service;

import group.g.graduation.backend.Security.model.User;
import group.g.graduation.backend.Security.repository.UserRepository;
import group.g.graduation.backend.Security.util.SecurityUtils;
import group.g.graduation.backend.common.model.UserPreference;
import group.g.graduation.backend.common.repository.UserPreferenceRepository;
import group.g.graduation.backend.user.dto.preference.LocationResponse;
import group.g.graduation.backend.user.dto.preference.NotificationSettingsRequest;
import group.g.graduation.backend.user.dto.preference.NotificationSettingsResponse;
import group.g.graduation.backend.user.dto.preference.PalestineCityInfo;
import group.g.graduation.backend.user.dto.preference.SetLocationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * User Preference Service - خدمة تفضيلات المستخدم
 * إدارة موقع المستخدم وتفضيلاته الشخصية
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserPreferenceService {

    private final UserPreferenceRepository userPreferenceRepository;
    private final UserRepository userRepository;

    /**
     * قائمة المدن الفلسطينية مع الإحداثيات الجغرافية
     */
    private static final List<PalestineCityInfo> PALESTINE_CITIES = List.of(
            // الضفة الغربية
            PalestineCityInfo.builder().nameAr("القدس").nameEn("Jerusalem").latitude(31.7683).longitude(35.2137).build(),
            PalestineCityInfo.builder().nameAr("رام الله").nameEn("Ramallah").latitude(31.9038).longitude(35.2034).build(),
            PalestineCityInfo.builder().nameAr("نابلس").nameEn("Nablus").latitude(32.2211).longitude(35.2544).build(),
            PalestineCityInfo.builder().nameAr("الخليل").nameEn("Hebron").latitude(31.5326).longitude(35.0998).build(),
            PalestineCityInfo.builder().nameAr("بيت لحم").nameEn("Bethlehem").latitude(31.7054).longitude(35.2024).build(),
            PalestineCityInfo.builder().nameAr("جنين").nameEn("Jenin").latitude(32.4611).longitude(35.2998).build(),
            PalestineCityInfo.builder().nameAr("طولكرم").nameEn("Tulkarm").latitude(32.3104).longitude(35.0286).build(),
            PalestineCityInfo.builder().nameAr("قلقيلية").nameEn("Qalqilya").latitude(32.1894).longitude(34.9714).build(),
            PalestineCityInfo.builder().nameAr("سلفيت").nameEn("Salfit").latitude(32.0833).longitude(35.1833).build(),
            PalestineCityInfo.builder().nameAr("طوباس").nameEn("Tubas").latitude(32.3204).longitude(35.3706).build(),
            PalestineCityInfo.builder().nameAr("أريحا").nameEn("Jericho").latitude(31.8611).longitude(35.4611).build(),

            // قطاع غزة
            PalestineCityInfo.builder().nameAr("غزة").nameEn("Gaza").latitude(31.5017).longitude(34.4668).build(),
            PalestineCityInfo.builder().nameAr("خان يونس").nameEn("Khan Yunis").latitude(31.3462).longitude(34.3032).build(),
            PalestineCityInfo.builder().nameAr("رفح").nameEn("Rafah").latitude(31.2969).longitude(34.2471).build(),
            PalestineCityInfo.builder().nameAr("دير البلح").nameEn("Deir al-Balah").latitude(31.4167).longitude(34.35).build(),
            PalestineCityInfo.builder().nameAr("بيت حانون").nameEn("Beit Hanoun").latitude(31.5386).longitude(34.5361).build(),
            PalestineCityInfo.builder().nameAr("جباليا").nameEn("Jabalia").latitude(31.5281).longitude(34.4831).build(),

            // مدن 48
            PalestineCityInfo.builder().nameAr("حيفا").nameEn("Haifa").latitude(32.7940).longitude(34.9896).build(),
            PalestineCityInfo.builder().nameAr("يافا").nameEn("Jaffa").latitude(32.0503).longitude(34.7597).build(),
            PalestineCityInfo.builder().nameAr("عكا").nameEn("Acre").latitude(32.9333).longitude(35.0833).build(),
            PalestineCityInfo.builder().nameAr("الناصرة").nameEn("Nazareth").latitude(32.6990).longitude(35.3026).build(),
            PalestineCityInfo.builder().nameAr("اللد").nameEn("Lod").latitude(31.9514).longitude(34.8953).build(),
            PalestineCityInfo.builder().nameAr("الرملة").nameEn("Ramla").latitude(31.9287).longitude(34.8717).build(),
            PalestineCityInfo.builder().nameAr("بئر السبع").nameEn("Beersheba").latitude(31.2518).longitude(34.7913).build(),
            PalestineCityInfo.builder().nameAr("أم الفحم").nameEn("Umm al-Fahm").latitude(32.5167).longitude(35.1500).build(),
            PalestineCityInfo.builder().nameAr("صفد").nameEn("Safed").latitude(32.9646).longitude(35.4963).build(),
            PalestineCityInfo.builder().nameAr("طبريا").nameEn("Tiberias").latitude(32.7958).longitude(35.5311).build()
    );

    /**
     * جلب قائمة المدن الفلسطينية المتاحة
     */
    public List<PalestineCityInfo> getAvailableCities() {
        return PALESTINE_CITIES;
    }

    /**
     * تحديد موقع المستخدم
     * يدعم حالتين:
     *   1. إرسال اسم المدينة → البحث في القائمة
     *   2. إرسال إحداثيات GPS فقط → إيجاد أقرب مدينة (Reverse Geocoding)
     */
    @Transactional
    public LocationResponse setUserLocation(SetLocationRequest request) {
        User user = getCurrentUser();
        UserPreference preference = userPreferenceRepository.findByUserId(user.getId())
                .orElse(UserPreference.builder().user(user).build());

        boolean hasCity = request.getCity() != null && !request.getCity().isBlank();
        boolean hasCoordinates = request.getLatitude() != null && request.getLongitude() != null;

        // ── CASE 1: City name provided → look it up ──
        if (hasCity) {
            PalestineCityInfo cityInfo = findCity(request.getCity());

            if (cityInfo != null) {
                preference.setCity(cityInfo.getNameEn());
                preference.setLatitude(cityInfo.getLatitude());
                preference.setLongitude(cityInfo.getLongitude());

                log.info("📍 User {} set location by city name: {} ({}, {})",
                        user.getEmail(), cityInfo.getNameEn(), cityInfo.getLatitude(), cityInfo.getLongitude());

                userPreferenceRepository.save(preference);

                return LocationResponse.builder()
                        .cityAr(cityInfo.getNameAr())
                        .cityEn(cityInfo.getNameEn())
                        .latitude(cityInfo.getLatitude())
                        .longitude(cityInfo.getLongitude())
                        .region("فلسطين")
                        .build();
            }

            // City not in list but coordinates provided → use custom city + coordinates
            if (hasCoordinates) {
                preference.setCity(request.getCity());
                preference.setLatitude(request.getLatitude());
                preference.setLongitude(request.getLongitude());

                log.info("📍 User {} set custom location: {} ({}, {})",
                        user.getEmail(), request.getCity(), request.getLatitude(), request.getLongitude());

                userPreferenceRepository.save(preference);

                return LocationResponse.builder()
                        .cityAr(request.getCity())
                        .cityEn(request.getCity())
                        .latitude(request.getLatitude())
                        .longitude(request.getLongitude())
                        .region("فلسطين")
                        .build();
            }

            // City not in list, no coordinates → partial match or default
            PalestineCityInfo closest = findClosestCity(request.getCity());
            if (closest != null) {
                preference.setCity(closest.getNameEn());
                preference.setLatitude(closest.getLatitude());
                preference.setLongitude(closest.getLongitude());
                userPreferenceRepository.save(preference);

                return LocationResponse.builder()
                        .cityAr(closest.getNameAr())
                        .cityEn(closest.getNameEn())
                        .latitude(closest.getLatitude())
                        .longitude(closest.getLongitude())
                        .region("فلسطين")
                        .build();
            }
        }

        // ── CASE 2: GPS coordinates only (no city) → find nearest city ──
        if (hasCoordinates) {
            PalestineCityInfo nearest = findNearestCity(request.getLatitude(), request.getLongitude());

            preference.setCity(nearest.getNameEn());
            preference.setLatitude(request.getLatitude());
            preference.setLongitude(request.getLongitude());

            log.info("📍 User {} set location by GPS: ({}, {}) → nearest city: {}",
                    user.getEmail(), request.getLatitude(), request.getLongitude(), nearest.getNameEn());

            userPreferenceRepository.save(preference);

            return LocationResponse.builder()
                    .cityAr(nearest.getNameAr())
                    .cityEn(nearest.getNameEn())
                    .latitude(request.getLatitude())
                    .longitude(request.getLongitude())
                    .region("فلسطين")
                    .build();
        }

        // ── Fallback: Nablus as default ──
        PalestineCityInfo defaultCity = PALESTINE_CITIES.stream()
                .filter(c -> c.getNameEn().equals("Nablus"))
                .findFirst().orElse(PALESTINE_CITIES.get(0));

        preference.setCity(defaultCity.getNameEn());
        preference.setLatitude(defaultCity.getLatitude());
        preference.setLongitude(defaultCity.getLongitude());
        userPreferenceRepository.save(preference);

        return LocationResponse.builder()
                .cityAr(defaultCity.getNameAr())
                .cityEn(defaultCity.getNameEn())
                .latitude(defaultCity.getLatitude())
                .longitude(defaultCity.getLongitude())
                .region("فلسطين")
                .build();
    }

    /**
     * جلب موقع المستخدم الحالي
     */
    @Transactional(readOnly = true)
    public LocationResponse getUserLocation() {
        User user = getCurrentUser();
        Optional<UserPreference> prefOpt = userPreferenceRepository.findByUserId(user.getId());

        if (prefOpt.isPresent() && prefOpt.get().getCity() != null) {
            UserPreference pref = prefOpt.get();

            // البحث عن الاسم العربي
            String cityAr = pref.getCity();
            PalestineCityInfo cityInfo = PALESTINE_CITIES.stream()
                    .filter(c -> c.getNameEn().equalsIgnoreCase(pref.getCity())
                            || c.getNameAr().equals(pref.getCity()))
                    .findFirst().orElse(null);

            if (cityInfo != null) {
                cityAr = cityInfo.getNameAr();
            }

            return LocationResponse.builder()
                    .cityAr(cityAr)
                    .cityEn(pref.getCity())
                    .latitude(pref.getLatitude())
                    .longitude(pref.getLongitude())
                    .region("فلسطين")
                    .build();
        }

        return null; // لم يتم تحديد الموقع بعد
    }

    // ===== Helper Methods =====

    /**
     * البحث عن مدينة بالاسم (عربي أو إنجليزي)
     */
    private PalestineCityInfo findCity(String cityName) {
        if (cityName == null) return null;
        String name = cityName.trim();
        return PALESTINE_CITIES.stream()
                .filter(c -> c.getNameAr().equals(name)
                        || c.getNameEn().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * البحث عن أقرب مدينة بتطابق جزئي
     */
    private PalestineCityInfo findClosestCity(String cityName) {
        if (cityName == null) return null;
        String name = cityName.trim().toLowerCase();
        return PALESTINE_CITIES.stream()
                .filter(c -> c.getNameAr().contains(name)
                        || c.getNameEn().toLowerCase().contains(name)
                        || name.contains(c.getNameAr())
                        || name.contains(c.getNameEn().toLowerCase()))
                .findFirst()
                .orElse(null);
    }

    /**
     * إيجاد أقرب مدينة من الإحداثيات باستخدام Haversine distance
     * (Reverse Geocoding محلي بدون API خارجي)
     */
    private PalestineCityInfo findNearestCity(double lat, double lng) {
        PalestineCityInfo nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (PalestineCityInfo city : PALESTINE_CITIES) {
            double distance = haversineDistance(lat, lng, city.getLatitude(), city.getLongitude());
            if (distance < minDistance) {
                minDistance = distance;
                nearest = city;
            }
        }

        log.debug("📍 Nearest city to ({}, {}): {} (distance: {} km)",
                lat, lng, nearest != null ? nearest.getNameEn() : "N/A", String.format("%.2f", minDistance));

        // If no city found (shouldn't happen), fallback to Nablus
        if (nearest == null) {
            nearest = PALESTINE_CITIES.stream()
                    .filter(c -> c.getNameEn().equals("Nablus"))
                    .findFirst().orElse(PALESTINE_CITIES.get(0));
        }

        return nearest;
    }

    /**
     * حساب المسافة بين نقطتين جغرافيتين بالكيلومتر (Haversine formula)
     */
    private double haversineDistance(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371.0; // نصف قطر الأرض بالكيلومتر

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    // ═══════════════════════════════════════════════
    // إعدادات الإشعارات
    // ═══════════════════════════════════════════════

    /**
     * تحديث إعدادات الإشعارات للمستخدم
     */
    @Transactional
    public NotificationSettingsResponse updateNotificationSettings(NotificationSettingsRequest request) {
        User user = getCurrentUser();
        UserPreference pref = userPreferenceRepository.findByUserId(user.getId())
                .orElse(UserPreference.builder().user(user).build());

        if (request.getNotificationEnabled() != null) {
            pref.setNotificationEnabled(request.getNotificationEnabled());
        }
        if (request.getPushEnabled() != null) {
            pref.setPushEnabled(request.getPushEnabled());
        }
        if (request.getFcmToken() != null) {
            pref.setFcmToken(request.getFcmToken());
        }
        if (request.getMorningReminderTime() != null) {
            pref.setMorningReminderTime(request.getMorningReminderTime());
        }
        if (request.getEveningReminderTime() != null) {
            pref.setEveningReminderTime(request.getEveningReminderTime());
        }
        if (request.getLanguage() != null) {
            pref.setLanguage(request.getLanguage());
        }

        userPreferenceRepository.save(pref);
        log.info("⚙️ User {} updated notification settings", user.getEmail());

        return mapToSettingsResponse(pref);
    }

    /**
     * جلب إعدادات الإشعارات الحالية
     */
    @Transactional(readOnly = true)
    public NotificationSettingsResponse getNotificationSettings() {
        User user = getCurrentUser();
        UserPreference pref = userPreferenceRepository.findByUserId(user.getId())
                .orElse(UserPreference.builder()
                        .user(user)
                        .notificationEnabled(true)
                        .pushEnabled(true)
                        .language("ar")
                        .build());

        return mapToSettingsResponse(pref);
    }

    /**
     * تحديث FCM Token فقط (يُستدعى عند كل فتح للتطبيق)
     */
    @Transactional
    public void updateFcmToken(String fcmToken) {
        User user = getCurrentUser();
        UserPreference pref = userPreferenceRepository.findByUserId(user.getId())
                .orElse(UserPreference.builder().user(user).build());

        pref.setFcmToken(fcmToken);
        userPreferenceRepository.save(pref);
        log.debug("📱 FCM token updated for user {}", user.getEmail());
    }

    private NotificationSettingsResponse mapToSettingsResponse(UserPreference pref) {
        return NotificationSettingsResponse.builder()
                .notificationEnabled(pref.getNotificationEnabled() != null ? pref.getNotificationEnabled() : true)
                .pushEnabled(pref.getPushEnabled() != null ? pref.getPushEnabled() : true)
                .hasFcmToken(pref.getFcmToken() != null && !pref.getFcmToken().isEmpty())
                .morningReminderTime(pref.getMorningReminderTime())
                .eveningReminderTime(pref.getEveningReminderTime())
                .language(pref.getLanguage() != null ? pref.getLanguage() : "ar")
                .build();
    }

    // ===== Helper Methods =====

    /**
     * جلب المستخدم الحالي من الـ Security Context
     */
    private User getCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }
}
