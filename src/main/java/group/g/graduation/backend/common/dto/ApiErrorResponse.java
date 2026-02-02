package group.g.graduation.backend.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Standard API Error Response DTO - استجابة الأخطاء الموحدة
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard error response structure - بنية استجابة الأخطاء الموحدة")
public class ApiErrorResponse {
    
    @Schema(description = "Timestamp of the error", example = "2024-01-15T10:30:00Z")
    private Instant timestamp;
    
    @Schema(description = "HTTP status code", example = "400")
    private int status;
    
    @Schema(description = "Error type/category", example = "Bad Request")
    private String error;
    
    @Schema(description = "Detailed error message", example = "Validation failed for the request")
    private String message;
    
    @Schema(description = "API endpoint path", example = "/api/admin/plants")
    private String path;
    
    @Schema(description = "Field-specific validation errors")
    private Map<String, String> fieldErrors;
    
    @Schema(description = "List of error details")
    private List<String> details;
    
    // Convenience factory methods
    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return ApiErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .build();
    }
    
    public static ApiErrorResponse badRequest(String message, String path) {
        return of(400, "Bad Request", message, path);
    }
    
    public static ApiErrorResponse unauthorized(String message, String path) {
        return of(401, "Unauthorized", message, path);
    }
    
    public static ApiErrorResponse forbidden(String message, String path) {
        return of(403, "Forbidden", message, path);
    }
    
    public static ApiErrorResponse notFound(String message, String path) {
        return of(404, "Not Found", message, path);
    }
    
    public static ApiErrorResponse serverError(String message, String path) {
        return of(500, "Internal Server Error", message, path);
    }
}
