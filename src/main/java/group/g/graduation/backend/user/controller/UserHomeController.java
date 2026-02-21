package group.g.graduation.backend.user.controller;

import group.g.graduation.backend.user.dto.home.*;
import group.g.graduation.backend.user.service.UserHomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * User Home Controller - الصفحة الرئيسية للمستخدم 🏠
 * 
 * تعرض:
 * 1. حالة الطقس اليوم مع صورة تعبيرية واليوم والموقع
 * 2. مثل شعبي أو تحفيز يومي يتغير كل يوم
 * 3. كالندر زراعية شهرية مع نباتات كل شهر
 */
@RestController
@RequestMapping("/api/user/home")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User - Home Page", description = "الصفحة الرئيسية: طقس، اقتباس يومي، تقويم زراعي")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class UserHomeController {

    private final UserHomeService homeService;

    // ===== 1. الصفحة الرئيسية الكاملة =====

    @GetMapping
    @Operation(
            summary = "جلب الصفحة الرئيسية كاملة",
            description = "يجلب كل بيانات الهوم بيج دفعة واحدة: الطقس + المثل اليومي + الكالندر الزراعية"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم جلب بيانات الصفحة الرئيسية بنجاح"),
            @ApiResponse(responseCode = "401", description = "غير مصرح - يجب تسجيل الدخول")
    })
    public ResponseEntity<HomePageResponse> getHomePage() {
        log.info("🏠 GET /api/user/home - Fetching home page");
        HomePageResponse response = homeService.getHomePage();
        return ResponseEntity.ok(response);
    }

    // ===== 2. حالة الطقس =====

    @GetMapping("/weather")
    @Operation(
            summary = "جلب حالة الطقس اليوم",
            description = "يعرض حالة الطقس: درجة الحرارة، اليوم، الموقع، وصورة تعبيرية عن الطقس"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم جلب الطقس بنجاح"),
            @ApiResponse(responseCode = "401", description = "غير مصرح")
    })
    public ResponseEntity<WeatherResponse> getWeather() {
        log.info("🌤️ GET /api/user/home/weather - Fetching weather");
        WeatherResponse weather = homeService.getWeather();
        return ResponseEntity.ok(weather);
    }

    // ===== 3. المثل/التحفيز اليومي =====

    @GetMapping("/daily-quote")
    @Operation(
            summary = "جلب المثل أو التحفيز اليومي",
            description = "مثل شعبي أو تحفيز يومي يتغير كل يوم تلقائياً"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم جلب الاقتباس بنجاح"),
            @ApiResponse(responseCode = "401", description = "غير مصرح")
    })
    public ResponseEntity<DailyQuoteResponse> getDailyQuote() {
        log.info("📜 GET /api/user/home/daily-quote - Fetching daily quote");
        DailyQuoteResponse quote = homeService.getDailyQuote();
        return ResponseEntity.ok(quote);
    }

    // ===== 4. الكالندر الزراعية =====

    @GetMapping("/calendar")
    @Operation(
            summary = "جلب الكالندر الزراعية الشهرية",
            description = "يعرض 12 شهر مع وصف حالة الطقس (كلمة أو كلمتين) وعدد النباتات لكل شهر"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم جلب الكالندر بنجاح"),
            @ApiResponse(responseCode = "401", description = "غير مصرح")
    })
    public ResponseEntity<MonthlyCalendarResponse> getCalendar() {
        log.info("📅 GET /api/user/home/calendar - Fetching monthly calendar");
        MonthlyCalendarResponse calendar = homeService.getCalendar();
        return ResponseEntity.ok(calendar);
    }

    // ===== 5. تفاصيل شهر محدد (عند الضغط على الشهر) =====

    @GetMapping("/calendar/{monthNumber}")
    @Operation(
            summary = "جلب تفاصيل شهر محدد مع النباتات المقترحة",
            description = "عند الضغط على شهر في الكالندر، يظهر تحته: اسم الشهر، المنطقة، " +
                    "والنباتات المقترحة مع اسم كل نبتة ووصف مختصر وملخص عنها"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم جلب تفاصيل الشهر بنجاح"),
            @ApiResponse(responseCode = "404", description = "الشهر غير موجود"),
            @ApiResponse(responseCode = "401", description = "غير مصرح")
    })
    public ResponseEntity<MonthDetailResponse> getMonthDetail(
            @Parameter(description = "رقم الشهر (1-12)", example = "3")
            @PathVariable Integer monthNumber) {
        log.info("📅 GET /api/user/home/calendar/{} - Fetching month detail", monthNumber);

        if (monthNumber < 1 || monthNumber > 12) {
            return ResponseEntity.badRequest().build();
        }

        MonthDetailResponse detail = homeService.getMonthDetail(monthNumber);
        return ResponseEntity.ok(detail);
    }
}
