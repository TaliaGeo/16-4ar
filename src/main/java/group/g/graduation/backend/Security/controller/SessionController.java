package group.g.graduation.backend.Security.controller;

import group.g.graduation.backend.Security.dto.MessageResponse;
import group.g.graduation.backend.Security.dto.UserDTO;
import group.g.graduation.backend.Security.dto.UserSessionDTO;
import group.g.graduation.backend.Security.service.UserService;
import group.g.graduation.backend.Security.service.UserSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for managing user sessions and devices
 */
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Session Management", description = "APIs for managing user sessions and devices")
@SecurityRequirement(name = "bearerAuth")
public class SessionController {

    private final UserSessionService sessionService;
    private final UserService userService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get active sessions", description = "Get all active sessions/devices for the current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sessions retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Map<String, Object>> getActiveSessions(
            @RequestAttribute(value = "tokenId", required = false) String tokenId) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        UserDTO user = userService.getUserByEmail(email);
        
        List<UserSessionDTO> sessions = sessionService.getActiveSessions(user.getId(), tokenId);
        long sessionCount = sessionService.getActiveSessionCount(user.getId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalActiveSessions", sessionCount);
        response.put("sessions", sessions);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get session count", description = "Get the number of active sessions for the current user")
    public ResponseEntity<Map<String, Long>> getSessionCount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        UserDTO user = userService.getUserByEmail(email);
        
        long count = sessionService.getActiveSessionCount(user.getId());
        
        Map<String, Long> response = new HashMap<>();
        response.put("activeSessionCount", count);
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Logout from specific session", description = "Logout from a specific device/session by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Session terminated successfully"),
            @ApiResponse(responseCode = "404", description = "Session not found"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<MessageResponse> logoutSession(@PathVariable Long sessionId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        UserDTO user = userService.getUserByEmail(email);
        
        boolean success = sessionService.logoutSession(user.getId(), sessionId);
        
        if (success) {
            return ResponseEntity.ok(new MessageResponse("Session terminated successfully"));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/others")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Logout from other sessions", description = "Logout from all devices except the current one")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Other sessions terminated successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<MessageResponse> logoutOtherSessions(
            @RequestAttribute(value = "tokenId", required = false) String tokenId) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        UserDTO user = userService.getUserByEmail(email);
        
        int count = sessionService.logoutOtherSessions(user.getId(), tokenId);
        
        return ResponseEntity.ok(new MessageResponse("Logged out from " + count + " other session(s)"));
    }

    @DeleteMapping("/all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Logout from all sessions", description = "Logout from all devices (including current)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All sessions terminated successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<MessageResponse> logoutAllSessions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        UserDTO user = userService.getUserByEmail(email);
        
        int count = sessionService.logoutAllSessions(user.getId());
        
        return ResponseEntity.ok(new MessageResponse("Logged out from all " + count + " session(s)"));
    }
}
