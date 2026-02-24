package group.g.graduation.backend.user.controller;

import group.g.graduation.backend.common.enums.PlantStatus;
import group.g.graduation.backend.user.dto.crop.*;
import group.g.graduation.backend.user.service.UserMyCropsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

/**
 * User My Crops Controller - صفحة محاصيلي 🌾
 *
 * الأقسام الثلاثة:
 * 1. مخطط لزراعتها (PLANNED) - النباتات المختارة من الاقتراحات
 * 2. المزروعة الآن (PLANTED) - النباتات التي زرعها اليوزر
 * 3. تم الحصاد (HARVESTED) - النباتات المحصودة
 *
 * المسار:
 * - GET  /overview          → نظرة عامة (الأقسام الثلاثة)
 * - GET  /planned           → كل المخططة
 * - GET  /planted           → كل المزروعة
 * - GET  /harvested         → كل المحصودة
 * - GET  /planned/{id}      → تفاصيل نبتة مخططة
 * - GET  /{id}/planting-steps → خطوات الزراعة (صفحة منفصلة)
 * - POST /mark-planted      → نقل مخطط → مزروع
 * - POST /{id}/harvest      → نقل مزروع → محصود
 * - GET  /{id}/overview     → نظرة عامة على مزروعة (تبويب 1)
 * - GET  /{id}/tasks        → مهام مزروعة (تبويب 2)
 * - POST /tasks/action      → تمت / ذكّرني غداً
 * - GET  /{id}/info         → معلومات مزروعة (تبويب 3)
 */
@RestController
@RequestMapping("/api/user/my-crops")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User - My Crops", description = "صفحة محاصيلي: مخططة، مزروعة، محصودة")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class UserMyCropsController {

    private final UserMyCropsService myCropsService;

    // =====================================================
    // ===== القوائم والنظرة العامة =====
    // =====================================================

    @GetMapping("/overview")
    @Operation(summary = "نظرة عامة على محاصيلي",
            description = "يجلب الأقسام الثلاثة: مخططة، مزروعة، محصودة مع عددهم")
    @ApiResponse(responseCode = "200", description = "تم جلب النظرة العامة")
    public ResponseEntity<MyCropsOverviewResponse> getOverview() {
        log.info("REST: جلب نظرة عامة على محاصيلي");
        return ResponseEntity.ok(myCropsService.getMyCropsOverview());
    }

    @PostMapping("/add/{plantId}")
    @Operation(summary = "إضافة نبتة للمخططة",
            description = "إضافة نبتة مباشرة لقسم المخططة بدون المرور بنظام التوصية")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم إضافة النبتة"),
            @ApiResponse(responseCode = "404", description = "النبتة غير موجودة")
    })
    public ResponseEntity<UserPlantCardResponse> addPlantToCrops(
            @Parameter(description = "معرّف النبتة") @PathVariable Long plantId,
            @RequestParam(required = false) String nickname) {
        log.info("REST: إضافة نبتة {} للمخططة", plantId);
        return ResponseEntity.ok(myCropsService.addPlantToCrops(plantId, nickname));
    }

    @GetMapping("/planned")
    @Operation(summary = "النباتات المخطط لزراعتها", description = "كل النباتات التي اختارها المستخدم من الاقتراحات")
    public ResponseEntity<List<UserPlantCardResponse>> getPlanned() {
        return ResponseEntity.ok(myCropsService.getPlantsByStatus(PlantStatus.PLANNED));
    }

    @GetMapping("/planted")
    @Operation(summary = "النباتات المزروعة حالياً", description = "كل النباتات التي زرعها المستخدم")
    public ResponseEntity<List<UserPlantCardResponse>> getPlanted() {
        return ResponseEntity.ok(myCropsService.getPlantsByStatus(PlantStatus.PLANTED));
    }

    @GetMapping("/harvested")
    @Operation(summary = "النباتات المحصودة", description = "كل النباتات التي تم حصادها")
    public ResponseEntity<List<UserPlantCardResponse>> getHarvested() {
        return ResponseEntity.ok(myCropsService.getPlantsByStatus(PlantStatus.HARVESTED));
    }

    // =====================================================
    // ===== تفاصيل النبتة المخططة =====
    // =====================================================

    @GetMapping("/planned/{userPlantId}")
    @Operation(summary = "تفاصيل نبتة مخططة",
            description = "معلومات كاملة عن نبتة مخططة: ضوء، تربة، ري، عناية (بدون خطوات الزراعة)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم جلب التفاصيل"),
            @ApiResponse(responseCode = "404", description = "النبتة غير موجودة")
    })
    public ResponseEntity<PlannedPlantDetailResponse> getPlannedDetail(
            @Parameter(description = "معرّف نبتة المستخدم") @PathVariable Long userPlantId) {
        log.info("REST: جلب تفاصيل نبتة مخططة - ID: {}", userPlantId);
        return ResponseEntity.ok(myCropsService.getPlannedPlantDetail(userPlantId));
    }

    @GetMapping("/{userPlantId}/planting-steps")
    @Operation(summary = "خطوات الزراعة",
            description = "صفحة منفصلة: خطوات زراعة النبتة، فيديو، معلومات تقنية (حرارة، مسافات، إنبات)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم جلب خطوات الزراعة"),
            @ApiResponse(responseCode = "404", description = "النبتة غير موجودة")
    })
    public ResponseEntity<PlantingStepsResponse> getPlantingSteps(
            @Parameter(description = "معرّف نبتة المستخدم") @PathVariable Long userPlantId) {
        log.info("REST: جلب خطوات الزراعة - ID: {}", userPlantId);
        return ResponseEntity.ok(myCropsService.getPlantingSteps(userPlantId));
    }

    // =====================================================
    // ===== تغيير الحالة =====
    // =====================================================

    @PostMapping("/mark-planted")
    @Operation(summary = "قمت بزراعتها",
            description = "نقل نبتة من المخططة إلى المزروعة + إنشاء مهام الرعاية تلقائياً")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم نقل النبتة للمزروعة"),
            @ApiResponse(responseCode = "400", description = "النبتة ليست في قسم المخططة"),
            @ApiResponse(responseCode = "404", description = "النبتة غير موجودة")
    })
    public ResponseEntity<UserPlantCardResponse> markAsPlanted(@Valid @RequestBody MarkPlantedRequest request) {
        log.info("REST: نقل نبتة للمزروعة - UserPlant ID: {}", request.getUserPlantId());
        return ResponseEntity.ok(myCropsService.markAsPlanted(request));
    }

    @PostMapping("/{userPlantId}/harvest")
    @Operation(summary = "حصاد النبتة", description = "نقل نبتة من المزروعة إلى المحصودة")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم حصاد النبتة"),
            @ApiResponse(responseCode = "400", description = "النبتة ليست في قسم المزروعة"),
            @ApiResponse(responseCode = "404", description = "النبتة غير موجودة")
    })
    public ResponseEntity<UserPlantCardResponse> markAsHarvested(
            @Parameter(description = "معرّف نبتة المستخدم") @PathVariable Long userPlantId) {
        log.info("REST: حصاد النبتة - UserPlant ID: {}", userPlantId);
        return ResponseEntity.ok(myCropsService.markAsHarvested(userPlantId));
    }

    // =====================================================
    // ===== تبويبات النبتة المزروعة =====
    // =====================================================

    @GetMapping("/{userPlantId}/overview")
    @Operation(summary = "نظرة عامة على نبتة مزروعة",
            description = "التبويب الأول: اسم، صورة، تاريخ زراعة، آخر ري، الري القادم")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم جلب النظرة العامة"),
            @ApiResponse(responseCode = "404", description = "النبتة غير موجودة")
    })
    public ResponseEntity<PlantedOverviewResponse> getPlantedOverview(
            @Parameter(description = "معرّف نبتة المستخدم") @PathVariable Long userPlantId) {
        log.info("REST: نظرة عامة على مزروعة - ID: {}", userPlantId);
        return ResponseEntity.ok(myCropsService.getPlantedOverview(userPlantId));
    }

    @GetMapping("/{userPlantId}/tasks")
    @Operation(summary = "مهام النبتة المزروعة",
            description = "التبويب الثاني: إشعارات الري والتسميد والحصاد مع حالة كل مهمة")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم جلب المهام"),
            @ApiResponse(responseCode = "404", description = "النبتة غير موجودة")
    })
    public ResponseEntity<PlantedTasksResponse> getPlantedTasks(
            @Parameter(description = "معرّف نبتة المستخدم") @PathVariable Long userPlantId) {
        log.info("REST: مهام النبتة - ID: {}", userPlantId);
        return ResponseEntity.ok(myCropsService.getPlantedTasks(userPlantId));
    }

    @PostMapping("/tasks/action")
    @Operation(summary = "إجراء على مهمة",
            description = "تمت (COMPLETE) أو ذكّرني غداً (SNOOZE)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم تنفيذ الإجراء"),
            @ApiResponse(responseCode = "404", description = "المهمة غير موجودة")
    })
    public ResponseEntity<UserPlantTaskResponse> taskAction(@Valid @RequestBody TaskActionRequest request) {
        log.info("REST: إجراء على مهمة - ID: {}, Action: {}", request.getTaskId(), request.getAction());
        return ResponseEntity.ok(myCropsService.performTaskAction(request));
    }

    @GetMapping("/{userPlantId}/info")
    @Operation(summary = "معلومات النبتة المزروعة",
            description = "التبويب الثالث: معلومات الرعاية والحصاد والاستخدامات وأشهر الزراعة")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم جلب المعلومات"),
            @ApiResponse(responseCode = "404", description = "النبتة غير موجودة")
    })
    public ResponseEntity<PlantedInfoResponse> getPlantedInfo(
            @Parameter(description = "معرّف نبتة المستخدم") @PathVariable Long userPlantId) {
        log.info("REST: معلومات النبتة - ID: {}", userPlantId);
        return ResponseEntity.ok(myCropsService.getPlantedInfo(userPlantId));
    }
}
