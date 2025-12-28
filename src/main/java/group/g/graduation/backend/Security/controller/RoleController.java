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

import group.g.graduation.backend.Security.dto.RoleDTO;
import group.g.graduation.backend.Security.service.RoleService;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Tag(name = "Role Management", description = "API endpoints for managing roles")
public class RoleController {
    
    private final RoleService roleService;
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('INSTRUCTOR')")
    @Operation(summary = "Get all roles", description = "Retrieve a list of all active roles")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Roles retrieved successfully",
                    content = @Content(schema = @Schema(implementation = RoleDTO.class))),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<List<RoleDTO>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INSTRUCTOR')")
    @Operation(summary = "Get role by ID", description = "Retrieve a role by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role found",
                    content = @Content(schema = @Schema(implementation = RoleDTO.class))),
            @ApiResponse(responseCode = "404", description = "Role not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<RoleDTO> getRoleById(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.getRoleById(id));
    }
    
    @GetMapping("/name/{name}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INSTRUCTOR')")
    @Operation(summary = "Get role by name", description = "Retrieve a role by its name")
    public ResponseEntity<RoleDTO> getRoleByName(@PathVariable String name) {
        return ResponseEntity.ok(roleService.getRoleByName(name));
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new role", description = "Create a new role with the specified details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Role created successfully",
                    content = @Content(schema = @Schema(implementation = RoleDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<RoleDTO> createRole(@Valid @RequestBody RoleDTO roleDTO) {
        return new ResponseEntity<>(roleService.createRole(roleDTO), HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing role", description = "Update the details of an existing role")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role updated successfully",
                    content = @Content(schema = @Schema(implementation = RoleDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Role not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<RoleDTO> updateRole(@PathVariable Long id, @Valid @RequestBody RoleDTO roleDTO) {
        return ResponseEntity.ok(roleService.updateRole(id, roleDTO));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a role", description = "Delete a role by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Role deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Role not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{roleId}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign permissions to a role", description = "Assign a list of permissions to a role")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Permissions assigned successfully",
                    content = @Content(schema = @Schema(implementation = RoleDTO.class))),
            @ApiResponse(responseCode = "404", description = "Role or permissions not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<RoleDTO> assignPermissionsToRole(
            @PathVariable Long roleId, @RequestBody List<Long> permissionIds) {
        return ResponseEntity.ok(roleService.assignPermissionsToRole(roleId, permissionIds));
    }

    @PostMapping("/{roleId}/permissions/{permissionId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add single permission to a role", description = "Add a single permission to a role")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Permission added successfully",
                    content = @Content(schema = @Schema(implementation = RoleDTO.class))),
            @ApiResponse(responseCode = "404", description = "Role or permission not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<RoleDTO> addPermissionToRole(
            @PathVariable Long roleId, @PathVariable Long permissionId) {
        return ResponseEntity.ok(roleService.addPermissionToRole(roleId, permissionId));
    }
    
    @DeleteMapping("/{roleId}/permissions/{permissionId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove permission from a role", description = "Remove a specific permission from a role")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Permission removed successfully",
                    content = @Content(schema = @Schema(implementation = RoleDTO.class))),
            @ApiResponse(responseCode = "404", description = "Role or permission not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<RoleDTO> removePermissionFromRole(
            @PathVariable Long roleId, @PathVariable Long permissionId) {
        return ResponseEntity.ok(roleService.removePermissionFromRole(roleId, permissionId));
    }
    
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate a role", description = "Activate a role by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role activated successfully",
                    content = @Content(schema = @Schema(implementation = RoleDTO.class))),
            @ApiResponse(responseCode = "404", description = "Role not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<RoleDTO> activateRole(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.activateRole(id));
    }
    
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate a role", description = "Deactivate a role by its ID")
    public ResponseEntity<RoleDTO> deactivateRole(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.deactivateRole(id));
    }
    
    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INSTRUCTOR')")
    @Operation(summary = "Get role permissions", description = "Get all permissions assigned to a role")
    public ResponseEntity<List<String>> getRolePermissions(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.getRolePermissions(id));
    }
}