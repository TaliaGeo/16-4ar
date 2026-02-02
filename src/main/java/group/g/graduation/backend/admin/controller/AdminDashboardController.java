package group.g.graduation.backend.admin.controller;

import group.g.graduation.backend.admin.dto.DashboardOverviewResponse;
import group.g.graduation.backend.admin.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin Dashboard Controller
 * تحكم لوحة التحكم للأدمن
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Dashboard", description = "لوحة التحكم والإحصائيات - APIs للأدمن")
@SecurityRequirement(name = "bearerAuth")
public class AdminDashboardController {
    
    private final AdminDashboardService dashboardService;
    
    // ===================== Overview =====================
    
    @GetMapping("/overview")
    @Operation(summary = "نظرة عامة", description = "جلب نظرة عامة على لوحة التحكم مع كل الإحصائيات")
    public ResponseEntity<DashboardOverviewResponse> getDashboardOverview() {
        return ResponseEntity.ok(dashboardService.getDashboardOverview());
    }
    
    @GetMapping("/all")
    @Operation(summary = "كل الإحصائيات", description = "جلب كل الإحصائيات في طلب واحد")
    public ResponseEntity<Map<String, Object>> getAllStats() {
        return ResponseEntity.ok(dashboardService.getAllStats());
    }
    
    @GetMapping("/summary")
    @Operation(summary = "ملخص النظام", description = "جلب ملخص سريع عن حالة النظام")
    public ResponseEntity<Map<String, Object>> getSystemSummary() {
        return ResponseEntity.ok(dashboardService.getSystemSummary());
    }
    
    // ===================== User Statistics =====================
    
    @GetMapping("/users/stats")
    @Operation(summary = "إحصائيات المستخدمين", description = "جلب إحصائيات تفصيلية عن المستخدمين")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        return ResponseEntity.ok(dashboardService.getUserStats());
    }
    
    @GetMapping("/users/growth")
    @Operation(summary = "نمو المستخدمين", description = "جلب بيانات نمو المستخدمين خلال آخر 30 يوم")
    public ResponseEntity<List<Map<String, Object>>> getUserGrowth() {
        return ResponseEntity.ok(dashboardService.getUserGrowth());
    }
    
    // ===================== Plant Statistics =====================
    
    @GetMapping("/plants/stats")
    @Operation(summary = "إحصائيات النباتات", description = "جلب إحصائيات تفصيلية عن النباتات")
    public ResponseEntity<Map<String, Object>> getPlantStats() {
        return ResponseEntity.ok(dashboardService.getPlantStats());
    }
    
    // ===================== Content Statistics =====================
    
    @GetMapping("/content/stats")
    @Operation(summary = "إحصائيات المحتوى", description = "جلب إحصائيات عن المحتوى (اقتباسات، أسئلة، مهام)")
    public ResponseEntity<Map<String, Object>> getContentStats() {
        return ResponseEntity.ok(dashboardService.getContentStats());
    }
    
    // ===================== Notification Statistics =====================
    
    @GetMapping("/notifications/stats")
    @Operation(summary = "إحصائيات الإشعارات", description = "جلب إحصائيات عن الإشعارات")
    public ResponseEntity<Map<String, Object>> getNotificationStats() {
        return ResponseEntity.ok(dashboardService.getNotificationStats());
    }
}
