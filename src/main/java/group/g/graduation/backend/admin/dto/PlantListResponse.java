package group.g.graduation.backend.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for paginated plant list response - رد قائمة النباتات مع الـ pagination
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantListResponse {
    
    private List<PlantSummaryResponse> plants;
    
    // ===== معلومات الـ Pagination =====
    private int currentPage;      // الصفحة الحالية (0-based)
    private int pageSize;         // حجم الصفحة
    private long totalElements;   // إجمالي العناصر
    private int totalPages;       // إجمالي الصفحات
    private boolean hasNext;      // هل يوجد صفحة تالية
    private boolean hasPrevious;  // هل يوجد صفحة سابقة
    private boolean isFirst;      // هل هذه الصفحة الأولى
    private boolean isLast;       // هل هذه الصفحة الأخيرة
}
