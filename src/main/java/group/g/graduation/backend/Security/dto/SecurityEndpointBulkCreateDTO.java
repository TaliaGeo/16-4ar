package group.g.graduation.backend.Security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create multiple security endpoints")
public class SecurityEndpointBulkCreateDTO {
    
    @NotBlank(message = "Base path is required")
    @Schema(description = "Base path for endpoints", example = "/api/courses", required = true)
    private String basePath;
    
    @Schema(description = "List of endpoint configurations")
    private List<EndpointConfig> endpoints;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EndpointConfig {
        @Schema(description = "HTTP method", example = "GET")
        private String method;
        
        @Schema(description = "Sub path", example = "/**")
        private String subPath;
        
        @Schema(description = "Permission name", example = "course:read")
        private String permissionName;
        
        @Schema(description = "Description", example = "Get course information")
        private String description;
    }
}