package group.g.graduation.backend.user.service;

import group.g.graduation.backend.Security.model.User;
import group.g.graduation.backend.Security.repository.UserRepository;
import group.g.graduation.backend.Security.util.SecurityUtils;
import group.g.graduation.backend.common.model.UserPreference;
import group.g.graduation.backend.common.repository.UserPreferenceRepository;
import group.g.graduation.backend.user.dto.home.DailyQuoteResponse;
import group.g.graduation.backend.user.dto.home.HomePageResponse;
import group.g.graduation.backend.user.dto.home.MonthDetailResponse;
import group.g.graduation.backend.user.dto.home.MonthlyCalendarResponse;
import group.g.graduation.backend.user.dto.home.WeatherResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * User Home Service - خدمة الصفحة الرئيسية للمستخدم
 * تجمع كل بيانات الهوم بيج: طقس + اقتباس + كالندر
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserHomeService {

    private final WeatherService weatherService;
    private final UserDailyQuoteService dailyQuoteService;
    private final MonthlyCalendarService monthlyCalendarService;
    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;

    /**
     * جلب بيانات الصفحة الرئيسية كاملة
     * API واحد للـ Frontend يجلب كل شيء دفعة واحدة
     */
    public HomePageResponse getHomePage() {
        log.debug("🏠 Building home page for current user");

        // جلب بيانات المستخدم والموقع
        String city = null;
        Double latitude = null;
        Double longitude = null;

        try {
            Optional<String> email = SecurityUtils.getCurrentUserEmail();
            if (email.isPresent()) {
                Optional<User> user = userRepository.findByEmail(email.get());
                if (user.isPresent()) {
                    Optional<UserPreference> prefs = userPreferenceRepository.findByUserId(user.get().getId());
                    if (prefs.isPresent()) {
                        city = prefs.get().getCity();
                        latitude = prefs.get().getLatitude();
                        longitude = prefs.get().getLongitude();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not fetch user preferences: {}", e.getMessage());
        }

        // 1. حالة الطقس
        WeatherResponse weather = weatherService.getWeather(city, latitude, longitude);

        // 2. المثل اليومي
        DailyQuoteResponse dailyQuote = dailyQuoteService.getTodayQuote();

        // 3. الكالندر الزراعية
        MonthlyCalendarResponse calendar = monthlyCalendarService.getCalendar();

        return HomePageResponse.builder()
                .weather(weather)
                .dailyQuote(dailyQuote)
                .calendar(calendar)
                .build();
    }

    /**
     * جلب حالة الطقس فقط
     */
    public WeatherResponse getWeather() {
        String city = null;
        Double lat = null;
        Double lon = null;

        try {
            Optional<String> email = SecurityUtils.getCurrentUserEmail();
            if (email.isPresent()) {
                Optional<User> user = userRepository.findByEmail(email.get());
                if (user.isPresent()) {
                    Optional<UserPreference> prefs = userPreferenceRepository.findByUserId(user.get().getId());
                    if (prefs.isPresent()) {
                        city = prefs.get().getCity();
                        lat = prefs.get().getLatitude();
                        lon = prefs.get().getLongitude();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not fetch user location: {}", e.getMessage());
        }

        return weatherService.getWeather(city, lat, lon);
    }

    /**
     * جلب المثل اليومي فقط
     */
    public DailyQuoteResponse getDailyQuote() {
        return dailyQuoteService.getTodayQuote();
    }

    /**
     * جلب الكالندر الزراعية
     */
    public MonthlyCalendarResponse getCalendar() {
        return monthlyCalendarService.getCalendar();
    }

    /**
     * جلب تفاصيل شهر محدد (عند الضغط على الشهر)
     */
    public MonthDetailResponse getMonthDetail(Integer monthNumber) {
        return monthlyCalendarService.getMonthDetail(monthNumber);
    }
}
