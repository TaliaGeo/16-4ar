package group.g.graduation.backend.user.service;

import group.g.graduation.backend.user.dto.home.WeatherResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * Weather Service - خدمة الطقس
 * تجلب بيانات الطقس من OpenWeatherMap API
 * مع fallback لبيانات افتراضية في حالة فشل API
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    @Value("${weather.api.key:}")
    private String apiKey;

    @Value("${weather.api.url:https://api.openweathermap.org/data/2.5/weather}")
    private String apiUrl;

    @Value("${weather.default.city:Amman}")
    private String defaultCity;

    @Value("${weather.default.lat:31.9454}")
    private String defaultLat;

    @Value("${weather.default.lon:35.9284}")
    private String defaultLon;

    /**
     * جلب حالة الطقس للموقع المحدد أو الموقع الافتراضي
     */
    public WeatherResponse getWeather(String city, Double latitude, Double longitude) {
        try {
            if (apiKey != null && !apiKey.isEmpty()) {
                return fetchFromApi(city, latitude, longitude);
            }
        } catch (Exception e) {
            log.warn("⚠️ Failed to fetch weather from API: {}. Using fallback.", e.getMessage());
        }

        // Fallback: بيانات افتراضية بناءً على الموسم
        return buildFallbackWeather(city);
    }

    /**
     * جلب الطقس من OpenWeatherMap API
     */
    @SuppressWarnings("unchecked")
    private WeatherResponse fetchFromApi(String city, Double latitude, Double longitude) {
        RestTemplate restTemplate = new RestTemplate();

        String url;
        if (latitude != null && longitude != null) {
            url = String.format("%s?lat=%s&lon=%s&appid=%s&units=metric&lang=ar",
                    apiUrl, latitude, longitude, apiKey);
        } else {
            String queryCity = (city != null && !city.isEmpty()) ? city : defaultCity;
            url = String.format("%s?q=%s&appid=%s&units=metric&lang=ar",
                    apiUrl, queryCity, apiKey);
        }

        log.debug("🌤️ Fetching weather from: {}", url.replaceAll("appid=[^&]+", "appid=***"));

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        if (response == null) {
            throw new RuntimeException("Empty response from weather API");
        }

        return parseApiResponse(response);
    }

    /**
     * تحليل استجابة API
     */
    @SuppressWarnings("unchecked")
    private WeatherResponse parseApiResponse(Map<String, Object> response) {
        // Extract main weather data
        Map<String, Object> main = (Map<String, Object>) response.get("main");
        Map<String, Object> wind = (Map<String, Object>) response.get("wind");
        java.util.List<Map<String, Object>> weatherList = (java.util.List<Map<String, Object>>) response.get("weather");
        Map<String, Object> weather = weatherList.get(0);

        String icon = (String) weather.get("icon");
        String cityName = (String) response.get("name");

        LocalDate today = LocalDate.now();

        return WeatherResponse.builder()
                .temperature(toDouble(main.get("temp")))
                .tempMax(toDouble(main.get("temp_max")))
                .tempMin(toDouble(main.get("temp_min")))
                .humidity(toInteger(main.get("humidity")))
                .descriptionAr((String) weather.get("description"))
                .descriptionEn(mapWeatherDescription((String) weather.get("main")))
                .weatherIcon(icon)
                .weatherImageUrl(String.format("https://openweathermap.org/img/wn/%s@4x.png", icon))
                .dayNameAr(getDayNameAr(today.getDayOfWeek()))
                .dayNameEn(today.getDayOfWeek().toString().charAt(0) +
                        today.getDayOfWeek().toString().substring(1).toLowerCase())
                .dateFormatted(formatDateAr(today))
                .locationAr(cityName != null ? cityName : defaultCity)
                .locationEn(cityName != null ? cityName : defaultCity)
                .windSpeed(wind != null ? toDouble(wind.get("speed")) : null)
                .seasonAr(getCurrentSeasonAr())
                .seasonEn(getCurrentSeasonEn())
                .build();
    }

    /**
     * بيانات طقس افتراضية بناءً على الشهر الحالي
     */
    private WeatherResponse buildFallbackWeather(String city) {
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();

        // بيانات تقريبية لمنطقة فلسطين/الأردن حسب الشهر
        double temp;
        double tempMax;
        double tempMin;
        int humidity;
        String descAr;
        String descEn;
        String icon;

        if (month >= 12 || month <= 2) {
            // شتاء
            temp = 10; tempMax = 14; tempMin = 5; humidity = 70;
            descAr = "غائم جزئياً"; descEn = "Partly Cloudy"; icon = "03d";
        } else if (month >= 3 && month <= 5) {
            // ربيع
            temp = 20; tempMax = 25; tempMin = 14; humidity = 50;
            descAr = "مشمس وربيعي"; descEn = "Sunny Spring"; icon = "02d";
        } else if (month >= 6 && month <= 8) {
            // صيف
            temp = 32; tempMax = 37; tempMin = 22; humidity = 30;
            descAr = "مشمس وحار"; descEn = "Sunny Hot"; icon = "01d";
        } else {
            // خريف
            temp = 22; tempMax = 27; tempMin = 16; humidity = 45;
            descAr = "معتدل"; descEn = "Moderate"; icon = "02d";
        }

        String locationName = (city != null && !city.isEmpty()) ? city : defaultCity;

        return WeatherResponse.builder()
                .temperature(temp)
                .tempMax(tempMax)
                .tempMin(tempMin)
                .humidity(humidity)
                .descriptionAr(descAr)
                .descriptionEn(descEn)
                .weatherIcon(icon)
                .weatherImageUrl(String.format("https://openweathermap.org/img/wn/%s@4x.png", icon))
                .dayNameAr(getDayNameAr(today.getDayOfWeek()))
                .dayNameEn(today.getDayOfWeek().toString().charAt(0) +
                        today.getDayOfWeek().toString().substring(1).toLowerCase())
                .dateFormatted(formatDateAr(today))
                .locationAr(locationName)
                .locationEn(locationName)
                .windSpeed(12.0)
                .seasonAr(getCurrentSeasonAr())
                .seasonEn(getCurrentSeasonEn())
                .build();
    }

    // ===== Helper Methods =====

    private String getDayNameAr(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "الاثنين";
            case TUESDAY -> "الثلاثاء";
            case WEDNESDAY -> "الأربعاء";
            case THURSDAY -> "الخميس";
            case FRIDAY -> "الجمعة";
            case SATURDAY -> "السبت";
            case SUNDAY -> "الأحد";
        };
    }

    private String formatDateAr(LocalDate date) {
        String[] arabicMonths = {
                "", "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
                "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
        };
        return date.getDayOfMonth() + " " + arabicMonths[date.getMonthValue()] + " " + date.getYear();
    }

    private String getCurrentSeasonAr() {
        int month = LocalDate.now().getMonthValue();
        if (month >= 12 || month <= 2) return "شتاء";
        if (month >= 3 && month <= 5) return "ربيع";
        if (month >= 6 && month <= 8) return "صيف";
        return "خريف";
    }

    private String getCurrentSeasonEn() {
        int month = LocalDate.now().getMonthValue();
        if (month >= 12 || month <= 2) return "Winter";
        if (month >= 3 && month <= 5) return "Spring";
        if (month >= 6 && month <= 8) return "Summer";
        return "Autumn";
    }

    private String mapWeatherDescription(String main) {
        if (main == null) return "Unknown";
        return switch (main.toLowerCase()) {
            case "clear" -> "Clear Sky";
            case "clouds" -> "Cloudy";
            case "rain" -> "Rainy";
            case "drizzle" -> "Drizzle";
            case "thunderstorm" -> "Thunderstorm";
            case "snow" -> "Snowy";
            case "mist", "fog", "haze" -> "Misty";
            default -> main;
        };
    }

    private Double toDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
