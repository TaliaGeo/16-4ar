package group.g.graduation.backend.common.email;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO لإرسال البريد الإلكتروني
 * Email Data Transfer Object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailDTO {

    @NotBlank(message = "البريد الإلكتروني للمستلم مطلوب")
    @Email(message = "صيغة البريد الإلكتروني غير صحيحة")
    private String to;

    private List<String> cc;

    private List<String> bcc;

    @NotBlank(message = "عنوان الرسالة مطلوب")
    private String subject;

    @NotBlank(message = "محتوى الرسالة مطلوب")
    private String body;

    private boolean isHtml;

    private String templateName;
}
