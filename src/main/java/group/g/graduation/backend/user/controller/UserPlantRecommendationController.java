package group.g.graduation.backend.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import group.g.graduation.backend.user.dto.plant.PlantingQuestionsResponse;
import group.g.graduation.backend.user.dto.plant.RecommendationDetailResponse;
import group.g.graduation.backend.user.dto.plant.RecommendationListResponse;
import group.g.graduation.backend.user.dto.plant.SelectPlantRequest;
import group.g.graduation.backend.user.dto.plant.SelectPlantResponse;
import group.g.graduation.backend.user.dto.plant.SubmitAnswersRequest;
import group.g.graduation.backend.user.service.UserPlantRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * User Plant Recommendation Controller - إضافة محصول 🌱
 * 
 * المسار الكامل:
 * 1. GET /questions → جلب الأسئلة الإجبارية والاختيارية
 * 2. POST /submit-answers → تقديم الإجابات والحصول على اقتراحات
 * 3. GET /recommendations/{sessionId} → جلب اقتراحات جلسة سابقة
 * 4. GET /recommendations/{sessionId}/plant/{plantId} → تفاصيل اقتراح معين
 * 5. POST /select → اختيار نبتة من الاقتراحات → تنضاف لمحاصيلي
 */
@RestController
@RequestMapping("/api/user/plant-recommendation")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User - Plant Recommendation", description = "إضافة محصول: أسئلة → اقتراحات → اختيار")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class UserPlantRecommendationController {

    private final UserPlantRecommendationService recommendationService;

    // ===== 1. جلب الأسئلة =====

    @GetMapping("/questions")
    @Operation(
            summary = "جلب أسئلة إضافة المحصول",
            description = """
                    يجلب كل الأسئلة النشطة مقسمة لإجبارية واختيارية.
                    كل سؤال فيه خيارات، واليوزر لازم يجاوب على كل الأسئلة الإجبارية.
                    بعض الأسئلة بتسمح باختيار أكثر من خيار (allowMultiple = true).
                    في كل سؤال خيار "مش متأكد" عشان اليوزر ما ينحجز.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم جلب الأسئلة بنجاح"),
            @ApiResponse(responseCode = "401", description = "غير مصرح - يجب تسجيل الدخول")
    })
    public ResponseEntity<PlantingQuestionsResponse> getPlantingQuestions() {
        log.info("📋 GET /api/user/plant-recommendation/questions");
        PlantingQuestionsResponse response = recommendationService.getPlantingQuestions();
        return ResponseEntity.ok(response);
    }

    // ===== 2. تقديم الإجابات والحصول على اقتراحات =====

    @PostMapping("/submit-answers")
    @Operation(
            summary = "تقديم الإجابات والحصول على اقتراحات",
            description = """
                    اليوزر يبعث إجاباته على الأسئلة.
                    النظام يتحقق من إجابة كل الأسئلة الإجبارية.
                    بعدين يحسب نسبة التطابق لكل نبتة ويرجع اقتراحات مرتبة.
                    كل اقتراح فيه: اسم النبتة، نسبة التطابق، مستوى التطابق، ملخص الظروف.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم حساب الاقتراحات بنجاح"),
            @ApiResponse(responseCode = "400", description = "الأسئلة الإجبارية ناقصة أو خيارات غير صحيحة"),
            @ApiResponse(responseCode = "401", description = "غير مصرح")
    })
    public ResponseEntity<RecommendationListResponse> submitAnswers(
            @Valid @RequestBody SubmitAnswersRequest request) {
        log.info("📝 POST /api/user/plant-recommendation/submit-answers");
        RecommendationListResponse response = recommendationService.submitAnswersAndGetRecommendations(request);
        return ResponseEntity.ok(response);
    }

    // ===== 3. جلب اقتراحات جلسة سابقة =====

    @GetMapping("/recommendations/{sessionId}")
    @Operation(
            summary = "جلب اقتراحات جلسة سابقة",
            description = "يجلب الاقتراحات المحفوظة لجلسة معينة (مثلاً لو رجع للصفحة)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم جلب الاقتراحات بنجاح"),
            @ApiResponse(responseCode = "404", description = "الجلسة غير موجودة"),
            @ApiResponse(responseCode = "401", description = "غير مصرح")
    })
    public ResponseEntity<RecommendationListResponse> getSessionRecommendations(
            @Parameter(description = "معرّف الجلسة") @PathVariable String sessionId) {
        log.info("📋 GET /api/user/plant-recommendation/recommendations/{}", sessionId);
        RecommendationListResponse response = recommendationService.getSessionRecommendations(sessionId);
        return ResponseEntity.ok(response);
    }

    // ===== 4. تفاصيل اقتراح معين =====

    @GetMapping("/recommendations/{sessionId}/plant/{plantId}")
    @Operation(
            summary = "تفاصيل اقتراح معين",
            description = """
                    لما اليوزر يضغط على اقتراح معين بيشوف:
                    - ليش اقترحناه (أسباب الاقتراح)
                    - تفاصيل كل سؤال ونقاطه
                    - تعديلات بسيطة لنجاح أفضل (إن وجدت)
                    - معلومات العناية (ضوء، تربة، ري)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم جلب التفاصيل بنجاح"),
            @ApiResponse(responseCode = "404", description = "الاقتراح غير موجود"),
            @ApiResponse(responseCode = "401", description = "غير مصرح")
    })
    public ResponseEntity<RecommendationDetailResponse> getRecommendationDetail(
            @Parameter(description = "معرّف الجلسة") @PathVariable String sessionId,
            @Parameter(description = "معرّف النبتة") @PathVariable Long plantId) {
        log.info("🔍 GET /api/user/plant-recommendation/recommendations/{}/plant/{}", sessionId, plantId);
        RecommendationDetailResponse response = recommendationService.getRecommendationDetail(sessionId, plantId);
        return ResponseEntity.ok(response);
    }

    // ===== 5. اختيار نبتة =====

    @PostMapping("/select")
    @Operation(
            summary = "اختيار نبتة من الاقتراحات",
            description = """
                    اليوزر يختار نبتة من الاقتراحات.
                    النبتة تنضاف لمحاصيله بحالة PLANNED (مخطط).
                    بعدين بيروح على صفحة "محاصيلي".
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم إضافة النبتة لمحاصيلك بنجاح"),
            @ApiResponse(responseCode = "404", description = "الاقتراح أو النبتة غير موجودة"),
            @ApiResponse(responseCode = "401", description = "غير مصرح")
    })
    public ResponseEntity<SelectPlantResponse> selectPlant(
            @Valid @RequestBody SelectPlantRequest request) {
        log.info("🌱 POST /api/user/plant-recommendation/select");
        SelectPlantResponse response = recommendationService.selectPlant(request);
        return ResponseEntity.ok(response);
    }
}
