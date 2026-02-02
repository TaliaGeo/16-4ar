package group.g.graduation.backend.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Unified Page Response DTO - استجابة الترقيم الموحدة
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paginated response structure - بنية استجابة الترقيم")
public class PageResponse<T> {
    
    @Schema(description = "Page content items - محتويات الصفحة")
    private List<T> content;
    
    @Schema(description = "Current page number (0-based)", example = "0")
    private int pageNumber;
    
    @Schema(description = "Page size", example = "20")
    private int pageSize;
    
    @Schema(description = "Total number of elements", example = "100")
    private long totalElements;
    
    @Schema(description = "Total number of pages", example = "5")
    private int totalPages;
    
    @Schema(description = "Is this the first page?", example = "true")
    private boolean first;
    
    @Schema(description = "Is this the last page?", example = "false")
    private boolean last;
    
    @Schema(description = "Number of elements in current page", example = "20")
    private int numberOfElements;
    
    @Schema(description = "Is the page empty?", example = "false")
    private boolean empty;
    
    /**
     * Create PageResponse from Spring Data Page
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .numberOfElements(page.getNumberOfElements())
                .empty(page.isEmpty())
                .build();
    }
    
    /**
     * Create PageResponse from Spring Data Page with mapping function
     */
    public static <T, R> PageResponse<R> from(Page<T> page, Function<T, R> mapper) {
        List<R> mappedContent = page.getContent().stream()
                .map(mapper)
                .collect(Collectors.toList());
        
        return PageResponse.<R>builder()
                .content(mappedContent)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .numberOfElements(page.getNumberOfElements())
                .empty(page.isEmpty())
                .build();
    }
}
