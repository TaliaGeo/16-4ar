package group.g.graduation.backend.Security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String email;
    private String fullName;
    private String password; // Only used for authentication
    private String phoneNumber;
    private Set<Long> roleIds = new HashSet<>();
    private Set<String> roleNames = new HashSet<>();
    private String profilePicture;
    private Boolean active; // Changed to Boolean to allow null in partial updates
    private Instant createdAt;
    private Instant updatedAt;
}