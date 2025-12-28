package group.g.graduation.backend.Security.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import group.g.graduation.backend.Security.dto.PermissionDTO;
import group.g.graduation.backend.Security.service.PermissionService;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@Tag(name = "Permission Management", description = "API endpoints for managing permissions")
public class PermissionController {
    
    private final PermissionService permissionService;
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('INSTRUCTOR')")
    @Operation(summary = "Get all permissions", description = "Retrieve a list of all active permissions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Permissions retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PermissionDTO.class))),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<List<PermissionDTO>> getAllPermissions(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        List<PermissionDTO> permissions = includeInactive 
            ? permissionService.getAllPermissionsIncludingInactive()
            : permissionService.getAllPermissions();
        return ResponseEntity.ok(permissions);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INSTRUCTOR')")
    @Operation(summary = "Get permission by ID", description = "Retrieve a permission by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Permission found",
                    content = @Content(schema = @Schema(implementation = PermissionDTO.class))),
            @ApiResponse(responseCode = "404", description = "Permission not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<PermissionDTO> getPermissionById(@PathVariable Long id) {
        return ResponseEntity.ok(permissionService.getPermissionById(id));
    }
    
    @GetMapping("/name/{name}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INSTRUCTOR')")
    @Operation(summary = "Get permission by name", description = "Retrieve a permission by its name")
    public ResponseEntity<PermissionDTO> getPermissionByName(@PathVariable String name) {
        return ResponseEntity.ok(permissionService.getPermissionByName(name));
    }
    
    @GetMapping("/resource/{resource}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INSTRUCTOR')")
    @Operation(summary = "Get permissions by resource", description = "Get all permissions for a specific resource")
    public ResponseEntity<List<PermissionDTO>> getPermissionsByResource(@PathVariable String resource) {
        return ResponseEntity.ok(permissionService.getPermissionsByResource(resource));
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new permission", description = "Create a new permission with the specified details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Permission created successfully",
                    content = @Content(schema = @Schema(implementation = PermissionDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<PermissionDTO> createPermission(@Valid @RequestBody PermissionDTO permissionDTO) {
        return new ResponseEntity<>(permissionService.createPermission(permissionDTO), HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a permission", description = "Update an existing permission by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Permission updated successfully",
                    content = @Content(schema = @Schema(implementation = PermissionDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Permission not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<PermissionDTO> updatePermission(
            @PathVariable Long id, @Valid @RequestBody PermissionDTO permissionDTO) {
        return ResponseEntity.ok(permissionService.updatePermission(id, permissionDTO));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a permission", description = "Delete a permission by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Permission deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Permission not found"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "409", description = "Permission is in use and cannot be deleted")
    })
    public ResponseEntity<Void> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate a permission", description = "Activate a permission by its ID")
    public ResponseEntity<PermissionDTO> activatePermission(@PathVariable Long id) {
        return ResponseEntity.ok(permissionService.activatePermission(id));
    }
    
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate a permission", description = "Deactivate a permission by its ID")
    public ResponseEntity<PermissionDTO> deactivatePermission(@PathVariable Long id) {
        return ResponseEntity.ok(permissionService.deactivatePermission(id));
    }
    
    // Add these methods to your existing PermissionController

@GetMapping("/search")
@PreAuthorize("hasRole('ADMIN') or hasRole('INSTRUCTOR')")
@Operation(summary = "Search permissions", description = "Search permissions by name or description")
public ResponseEntity<List<PermissionDTO>> searchPermissions(@RequestParam String searchTerm) {
    return ResponseEntity.ok(permissionService.searchPermissions(searchTerm));
}

@GetMapping("/unassigned")
@PreAuthorize("hasRole('ADMIN')")
@Operation(summary = "Get unassigned permissions", description = "Get permissions not assigned to any role")
public ResponseEntity<List<PermissionDTO>> getUnassignedPermissions() {
    return ResponseEntity.ok(permissionService.getUnassignedPermissions());
}

@GetMapping("/{id}/usage")
@PreAuthorize("hasRole('ADMIN')")
@Operation(summary = "Get permission usage", description = "Get usage statistics for a permission")
public ResponseEntity<PermissionService.PermissionUsageDTO> getPermissionUsage(@PathVariable Long id) {
    return ResponseEntity.ok(permissionService.getPermissionUsage(id));
}
    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bulk create permissions", description = "Create multiple permissions at once")
    public ResponseEntity<List<PermissionDTO>> bulkCreatePermissions(
            @Valid @RequestBody List<PermissionDTO> permissions) {
        return new ResponseEntity<>(permissionService.bulkCreatePermissions(permissions), HttpStatus.CREATED);
    }
}