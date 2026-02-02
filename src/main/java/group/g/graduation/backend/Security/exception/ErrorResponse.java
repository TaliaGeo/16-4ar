package group.g.graduation.backend.Security.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private int status;
    private String message;
    private LocalDateTime timestamp;
    private String errorCode;  // كود الخطأ للتعرف عليه
    private String details;    // تفاصيل إضافية (dev profile only)
    private String path;       // الـ endpoint الذي حدث فيه الخطأ
    
    // Constructor for backward compatibility
    public ErrorResponse(int status, String message, LocalDateTime timestamp) {
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
    }
    
    // Constructor with error code
    public ErrorResponse(int status, String message, LocalDateTime timestamp, String errorCode) {
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
        this.errorCode = errorCode;
    }
}