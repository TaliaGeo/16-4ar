package group.g.graduation.backend.Security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @jakarta.validation.constraints.Size(min = 8, message = "كلمة السر يجب أن تكون 8 أحرف على الأقل")
    @jakarta.validation.constraints.Pattern(
        regexp = "^(?=.*[a-zA-Z])(?=.*\\d).+$",
        message = "كلمة السر يجب أن تحتوي على أحرف وأرقام"
    )
    private String password;

    private String profilePicture;
}