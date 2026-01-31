package group.g.graduation.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for paginated quote list response - استجابة قائمة الاقتباسات مع الترقيم
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "استجابة قائمة الاقتباسات مع الترقيم")
public class QuoteListResponse {
    
    @Schema(description = "قائمة الاقتباسات")
    private List<QuoteResponse> quotes;
    
    @Schema(description = "الصفحة الحالية", example = "0")
    private int currentPage;
    
    @Schema(description = "إجمالي الصفحات", example = "5")
    private int totalPages;
    
    @Schema(description = "إجمالي العناصر", example = "50")
    private long totalElements;
    
    @Schema(description = "حجم الصفحة", example = "10")
    private int pageSize;
    
    @Schema(description = "هل هي الصفحة الأولى", example = "true")
    private boolean first;
    
    @Schema(description = "هل هي الصفحة الأخيرة", example = "false")
    private boolean last;
}
